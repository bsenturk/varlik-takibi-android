package com.xptlabs.varliktakibi.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
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
     * Reklamın gösterilmeyi beklediğini işaretler. İlk açılışta **false**:
     * app-open reklamını soğuk açılışta göstermek kullanıcının ilk deneyimini
     * (onboarding dahil) reklamla karşılıyor. Yalnızca gerçek bir arka plandan
     * dönüşte açılıyor — Google'ın da önerdiği davranış.
     */
    private var appOpenPending = false
    private var backgroundedAt: Long? = null

    // Interstitial
    private var interstitialAd: InterstitialAd? = null
    private var loadingInterstitial = false

    init {
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

    fun onBannerEvent(event: String) = analytics.logAdEvent(event, "banner")

    // ── App-open ─────────────────────────────────────────────────────────────

    fun preloadAppOpenAd() { if (adsEnabled) loadAppOpenAd() }

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
                    appOpenAd = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    loadingAppOpen = false
                    analytics.logAdEvent("ad_loaded", "app_open")
                    if (appOpenPending) showAppOpenAdIfAvailable()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                    Log.w(TAG, "App-open yüklenemedi: ${error.message}")
                    analytics.logAdEvent("ad_load_failed", "app_open", error.message)
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
        appOpenPending = false
        hideBanner()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() =
                analytics.logAdEvent("ad_shown", "app_open")

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingFullScreenAd = false
                analytics.logAdEvent("ad_closed", "app_open")
                showBanner()
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingFullScreenAd = false
                Log.w(TAG, "App-open gösterilemedi: ${error.message}")
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
                    interstitialAd = ad
                    loadingInterstitial = false
                    analytics.logAdEvent("ad_loaded", "interstitial")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    loadingInterstitial = false
                    Log.w(TAG, "Interstitial yüklenemedi: ${error.message}")
                    analytics.logAdEvent("ad_load_failed", "interstitial", error.message)
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
                analytics.logAdEvent("ad_shown", "interstitial")

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingFullScreenAd = false
                analytics.logAdEvent("ad_closed", "interstitial")
                showBanner()
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isShowingFullScreenAd = false
                Log.w(TAG, "Interstitial gösterilemedi: ${error.message}")
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
        if (away != null && away >= MIN_BACKGROUND_MS) {
            appOpenPending = true
            showAppOpenAdIfAvailable()
            if (appOpenAd == null) loadAppOpenAd()
        }
        backgroundedAt = null
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingFullScreenAd) currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        backgroundedAt = System.currentTimeMillis()
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
    }
}
