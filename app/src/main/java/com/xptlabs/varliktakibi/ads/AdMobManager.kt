package com.xptlabs.varliktakibi.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulamanın tüm reklam yüzeyleri: banner, interstitial, app-open.
 *
 * Pro abonelikte hiçbir reklam yüklenmez ve gösterilmez — kontrol tek noktada
 * ([adsEnabled]) ki bir yüzey unutulup Pro kullanıcıya reklam çıkmasın.
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: FirebaseAnalyticsManager,
    private val purchaseManager: PurchaseManager
) : Application.ActivityLifecycleCallbacks {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Banner görünür mü — tam ekran reklam sırasında gizlenir. */
    private val _bannerVisible = MutableStateFlow(true)
    val bannerVisible: StateFlow<Boolean> = _bannerVisible.asStateFlow()

    private val adsEnabled: Boolean get() = !purchaseManager.isPro.value

    private var currentActivity: Activity? = null
    private var isShowingFullScreenAd = false

    // App-open
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingAppOpen = false
    /**
     * Reklamın gösterilmeyi beklediği pencerenin bitiş anı. Yükleme bittiğinde
     * pencere kapanmışsa gösterilmez: kullanıcı çoktan uygulamayı kullanmaya
     * başlamışken ekrana reklam düşmesin.
     */
    private var appOpenPendingUntil = 0L
    private var coldStartHandled = false

    /**
     * Soğuk açılış kapısı: açılış ekranı, app-open reklamı gösterilene KADAR
     * içeriğin üstünde durur.
     *
     * Eskiden reklam yüklenene kadar içerik görünüyordu; kullanıcı portföyünü
     * görüp reklam hiç çıkmadan çıkabiliyordu. Daha kötüsü, yükleme geç biterse
     * reklam kullanıcı içeriği kullanırken patlıyordu — AdMob'un app-open
     * politikası bunu yasaklıyor (app-open yalnızca uygulama yüklenirken).
     *
     * Kapı [COLD_START_TIMEOUT_MS] dolduğunda koşulsuz açılır: süresiz
     * bekletmek, doluluk düşükken kullanıcıyı açılış ekranında kilitler.
     */
    private val _coldStartGateClosed = MutableStateFlow(false)
    val coldStartGateClosed: StateFlow<Boolean> = _coldStartGateClosed.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var backgroundedAt: Long? = null

    // Interstitial
    private var interstitialAd: InterstitialAd? = null
    private var loadingInterstitial = false

    init {
        // Kapı ilk kareden itibaren kapalı. Yalnızca içerik çizildikten sonra
        // kapatılsaydı (ve öyleydi) ana ekran bir an görünüp üstüne açılış
        // ekranı biniyordu. Reklam gösterilmeyecek her yol kapıyı açıyor;
        // hiçbiri çalışmazsa zaman aşımı açıyor.
        beginColdStartGate()

        MobileAds.initialize(context) {
            _isInitialized.value = true
            Log.d(TAG, "Mobile Ads SDK hazır")
            if (adsEnabled) {
                loadAppOpenAd()
                loadInterstitialAd()
            }
        }
    }

    fun registerActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    fun showBanner() { if (adsEnabled) _bannerVisible.value = true }
    fun hideBanner() { _bannerVisible.value = false }

    fun bannerAdUnitId(): String = BuildConfig.ADMOB_BANNER_ID
    fun newAdRequest(): AdRequest = AdRequest.Builder().build()

    fun onBannerEvent(action: String, errorMessage: String? = null) =
        analytics.logAdEvent(action, "banner", errorMessage)

    /**
     * AdMob'un impression bazlı gelir bildirimi. Üç yüzey de buraya bağlı —
     * gelir raporu tek olaydan (`ad_impression`) besleniyor.
     */
    fun onAdRevenue(value: AdValue, format: String, adUnitId: String, responseInfo: ResponseInfo?) =
        analytics.logAdRevenue(
            valueMicros = value.valueMicros,
            currencyCode = value.currencyCode,
            precisionType = value.precisionType,
            format = format,
            adUnitId = adUnitId,
            source = responseInfo?.loadedAdapterResponseInfo?.adSourceName
        )

    // ── App-open ─────────────────────────────────────────────────────────────

    fun preloadAppOpenAd() { if (adsEnabled) loadAppOpenAd() }

    /**
     * Ana içerik ilk kez ekrana geldiğinde soğuk açılış reklamını ister.
     * Onboarding'i ve hemen ardındaki ilk varlık yönlendirmesini reklamla
     * karşılamamak için gösterimi Application.onCreate'e değil bu ana bağlıyoruz;
     * [showAd] false ise pencere yalnızca tüketilir, reklam açılmaz.
     */
    fun onMainContentReady(showAd: Boolean = true) {
        if (coldStartHandled) return
        coldStartHandled = true
        // Reklam gösterilmeyecek durumlarda (Pro, onboarding devri) kapı hemen
        // açılıyor: o kullanıcılar açılış ekranında bekletilmemeli.
        if (!showAd || !adsEnabled) {
            openColdStartGate()
            return
        }
        requestAppOpenAd()
    }

    private fun beginColdStartGate() {
        _coldStartGateClosed.value = true
        mainHandler.postDelayed(coldStartTimeout, COLD_START_TIMEOUT_MS)
    }

    /** Reklam isteği kapıdan bağımsız açıldı; pencereyi baştan başlat. */
    private fun restartColdStartWindow() {
        mainHandler.removeCallbacks(coldStartTimeout)
        mainHandler.postDelayed(coldStartTimeout, COLD_START_TIMEOUT_MS)
    }

    /**
     * Kapının koşulsuz çıkışı. Reklam ekrandaysa kapı açılmaz (içerik reklamın
     * arkasında açığa çıkardı) ama kontrol yeniden kuyruğa alınır: gösterim
     * bayrağı bir şekilde asılı kalırsa kullanıcı açılış ekranında sonsuza
     * kadar kilitli kalmasın.
     */
    private val coldStartTimeout = object : Runnable {
        override fun run() {
            if (!_coldStartGateClosed.value) return
            if (isShowingFullScreenAd) {
                mainHandler.postDelayed(this, COLD_START_TIMEOUT_MS)
                return
            }
            // Bekleyen gösterim de düşürülüyor: aksi hâlde reklam kullanıcı
            // içeriği kullanırken açılır — politika ihlali olan tam bu.
            appOpenPendingUntil = 0L
            openColdStartGate()
        }
    }

    /** İçeriği serbest bırakır. Birden çok kez çağrılması güvenli. */
    private fun openColdStartGate() {
        _coldStartGateClosed.value = false
    }

    private fun isAppOpenPending(): Boolean = System.currentTimeMillis() < appOpenPendingUntil

    private fun requestAppOpenAd() {
        restartColdStartWindow()
        appOpenPendingUntil = System.currentTimeMillis() + COLD_START_TIMEOUT_MS
        showAppOpenAdIfAvailable()
        if (appOpenAd == null) loadAppOpenAd()
    }

    private fun loadAppOpenAd() {
        if (!adsEnabled || loadingAppOpen || isAppOpenAdFresh()) return
        if (!_isInitialized.value) return

        loadingAppOpen = true
        AppOpenAd.load(
            context,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.onPaidEventListener = OnPaidEventListener { value ->
                        onAdRevenue(value, "app_open", ad.adUnitId, ad.responseInfo)
                    }
                    appOpenAd = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    loadingAppOpen = false
                    analytics.logAdEvent("loaded", "app_open")
                    if (isAppOpenPending()) showAppOpenAdIfAvailable()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                    Log.w(TAG, "App-open yüklenemedi: ${error.message}")
                    analytics.logAdEvent("load_failed", "app_open", error.message)
                    // Gösterilecek reklam yok; kullanıcıyı zaman aşımı boyunca
                    // açılış ekranında bekletmenin anlamı kalmadı.
                    openColdStartGate()
                }
            }
        )
    }

    /** Reklam 4 saatten eskiyse AdMob politikası gereği kullanılmaz. */
    private fun isAppOpenAdFresh(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - appOpenLoadedAt < APP_OPEN_TTL_MS

    private fun showAppOpenAdIfAvailable() {
        if (!adsEnabled || isShowingFullScreenAd || !isAppOpenAdFresh()) return
        val activity = currentActivity ?: return
        val ad = appOpenAd ?: return

        isShowingFullScreenAd = true
        appOpenPendingUntil = 0L
        hideBanner()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() =
                analytics.logAdEvent("did_present", "app_open")

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingFullScreenAd = false
                analytics.logAdEvent("dismissed", "app_open")
                openColdStartGate()
                showBanner()
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingFullScreenAd = false
                Log.w(TAG, "App-open gösterilemedi: ${error.message}")
                analytics.logAdEvent("present_failed", "app_open", error.message)
                openColdStartGate()
                showBanner()
                loadAppOpenAd()
            }
        }
        ad.show(activity)
    }

    // ── Interstitial ─────────────────────────────────────────────────────────

    fun loadInterstitialAd() {
        if (!adsEnabled || loadingInterstitial || interstitialAd != null) return
        if (!_isInitialized.value) return

        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.onPaidEventListener = OnPaidEventListener { value ->
                        onAdRevenue(value, "interstitial", ad.adUnitId, ad.responseInfo)
                    }
                    interstitialAd = ad
                    loadingInterstitial = false
                    analytics.logAdEvent("loaded", "interstitial")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    loadingInterstitial = false
                    Log.w(TAG, "Interstitial yüklenemedi: ${error.message}")
                    analytics.logAdEvent("load_failed", "interstitial", error.message)
                }
            }
        )
    }

    val isInterstitialReady: Boolean get() = adsEnabled && interstitialAd != null

    /**
     * Interstitial'ı gösterir. Reklam hazır değilse ya da kullanıcı Pro ise
     * hiçbir şey yapmadan [onDismissed] çağrılır — akış hiçbir durumda takılmaz.
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (!adsEnabled || ad == null) {
            onDismissed()
            return
        }

        isShowingFullScreenAd = true
        hideBanner()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() =
                analytics.logAdEvent("will_present", "interstitial")

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingFullScreenAd = false
                analytics.logAdEvent("dismissed", "interstitial")
                showBanner()
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isShowingFullScreenAd = false
                Log.w(TAG, "Interstitial gösterilemedi: ${error.message}")
                analytics.logAdEvent("present_failed", "interstitial", error.message)
                showBanner()
                loadInterstitialAd()
                onDismissed()
            }
        }
        ad.show(activity)
    }

    /** Pro'ya geçildiğinde elde tutulan reklamları at, banner'ı gizle. */
    fun onProStatusChanged(isPro: Boolean) {
        if (isPro) {
            appOpenAd = null
            interstitialAd = null
            // Pro kullanıcı reklam beklemez.
            openColdStartGate()
            hideBanner()
        } else {
            showBanner()
            loadAppOpenAd()
            loadInterstitialAd()
        }
    }

    // ── Activity yaşam döngüsü ───────────────────────────────────────────────

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingFullScreenAd) currentActivity = activity

        // Kısa bir uygulama değişiminden (bildirim çekmecesi, hızlı kopyala)
        // dönüşte reklam göstermiyoruz — Google'ın önerdiği davranış.
        val away = backgroundedAt?.let { System.currentTimeMillis() - it }
        if (away != null && away >= MIN_BACKGROUND_MS) requestAppOpenAd()
        backgroundedAt = null
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingFullScreenAd) currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        // Tam ekran reklam kendi Activity'sini açarken de burası tetikleniyor;
        // uzun izlenen bir reklamı "arka plandan dönüş" sayıp üstüne ikinci bir
        // reklam açmayalım.
        if (!isShowingFullScreenAd) backgroundedAt = System.currentTimeMillis()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private companion object {
        const val TAG = "AdMobManager"
        const val APP_OPEN_TTL_MS = 4 * 60 * 60 * 1000L
        const val MIN_BACKGROUND_MS = 30_000L
        /**
         * ponytail: sabit; Remote Config'e taşınabilir ama önce ölçülmeli.
         * Hem kapının hem bekleyen gösterimin ömrü — reklam bu süre içinde
         * gelmezse kullanıcı serbest bırakılıyor ve gösterim düşürülüyor.
         */
        const val COLD_START_TIMEOUT_MS = 4_000L
    }
}
