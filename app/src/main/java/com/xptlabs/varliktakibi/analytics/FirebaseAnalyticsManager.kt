package com.xptlabs.varliktakibi.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS `FirebaseAnalyticsHelper` ile aynı olay adları ve parametreler — iki
 * platformun rakamları tek panoda karşılaştırılabilsin diye.
 *
 * Gizlilik: hiçbir olay kullanıcının **tutarını, portföy değerini veya girdiği
 * alış fiyatını** taşımaz. Yalnızca enstrüman sınıfı/sembolü ve davranış
 * bayrakları gönderilir. Eski Android sürümü portföy değerini ve kâr/zararı
 * olay parametresi olarak yolluyordu; kaldırıldı.
 */
@Singleton
class FirebaseAnalyticsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val analytics = FirebaseAnalytics.getInstance(context)

    fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    fun setUserProperty(name: String, value: String) = analytics.setUserProperty(name, value)

    /**
     * Her raporun ücretsiz/Pro kırılımı bu özelliğe dayanıyor; abonelik durumu
     * her değiştiğinde yazılır.
     */
    fun setProUser(isPro: Boolean) = setUserProperty("is_pro", isPro.toString())

    // ── Ekran / genel ────────────────────────────────────────────────────────

    /** Hangi ekranın kullanıldığı — Firebase'in etkileşim raporlarının tabanı. */
    fun logScreenView(screenName: String) = log(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
    }

    /**
     * Görüntüleme para birimi gerçekten değiştiğinde. `from_currency` olmadan
     * tekrar eden değişimlerin bir gidiş-geliş mi yoksa tek yönlü bir tercih mi
     * olduğu ayırt edilemiyor.
     */
    fun logCurrencyChanged(from: String, to: String) = log("currency_changed") {
        putString("from_currency", from)
        putString("currency", to)
    }

    fun logOnboardingCompleted(reachedPage: Int, skipped: Boolean) =
        log("onboarding_completed") {
            putLong("reached_page", reachedPage.toLong())
            putBoolean("skipped", skipped)
        }

    // ── Varlık ekleme hunisi ─────────────────────────────────────────────────
    //
    // `asset_added` tek başına yalnızca BAŞARIYI logluyordu: varlık eklemeyen
    // kullanıcının akışı hiç açmadığı mı, kategori ızgarasında mı bıraktığı,
    // yoksa tutar ekranında mı vazgeçtiği ölçülemiyordu. Aşağıdaki dört olay
    // adımları ayırıyor. `source` her adımda taşınıyor ki onboarding'den gelen
    // kullanıcı ile sonradan + butonuna basan ayrılabilsin.

    /** Varlık ekleme akışı açıldı. [source]: "onboarding" | "manual". */
    fun logAddAssetOpened(source: String) = log("add_asset_opened") {
        putString("source", source)
    }

    /** Kategori ızgarasından bir kategori seçildi (1. adım geçildi). */
    fun logAddAssetCategorySelected(category: String, source: String) =
        log("add_asset_category_selected") {
            putString("asset_category", category)
            putString("source", source)
        }

    /** Listeden somut bir enstrüman seçildi (2. adım geçildi, tutar ekranı açıldı). */
    fun logAddAssetInstrumentSelected(category: String, symbol: String, source: String) =
        log("add_asset_instrument_selected") {
            putString("asset_category", category)
            putString("asset_symbol", symbol)
            putString("source", source)
        }

    /**
     * Akış varlık eklenmeden kapatıldı. [step] ulaşılan **en derin** adım
     * ("category" | "type_list" | "amount") — geri dönülse bile. Huninin nerede
     * koptuğunu gösteren asıl olay bu.
     */
    fun logAddAssetAbandoned(step: String, category: String?, source: String) =
        log("add_asset_abandoned") {
            putString("step", step)
            category?.let { putString("asset_category", it) }
            putString("source", source)
        }

    fun logAssetAdded(
        category: String,
        symbol: String,
        isMerge: Boolean,
        hasPurchasePrice: Boolean,
        source: String
    ) = log("asset_added") {
        putString("asset_category", category)
        putString("asset_symbol", symbol)
        putBoolean("is_merge", isMerge)
        putBoolean("has_purchase_price", hasPurchasePrice)
        putString("source", source)
    }

    fun logAssetDeleted(category: String, symbol: String) = log("asset_deleted") {
        putString("asset_category", category)
        putString("asset_symbol", symbol)
    }

    fun logPremiumCategoryLocked(category: String) = log("premium_category_locked") {
        putString("asset_category", category)
    }

    // ── Portföy ──────────────────────────────────────────────────────────────
    // "Sınırsız portföy" Pro'nun ana vaadi; gerçekten talep var mı bu üç olayla
    // görülüyor. Portföy adı gönderilmiyor, kullanıcının kendi yazdığı metin.

    fun logPortfolioCreated(totalCount: Int) = log("portfolio_created") {
        putLong("total_count", totalCount.toLong())
    }

    fun logPortfolioDeleted() = log("portfolio_deleted") {}

    /** Ücretsiz kullanıcı portföy limitine çarptı. */
    fun logPortfolioLimitReached() = log("portfolio_limit_reached") {}

    // ── Piyasalar / analiz ───────────────────────────────────────────────────

    /** Piyasalar ekranında hangi varlık sınıfına bakılıyor. */
    fun logRatesTabSelected(tab: String) = log("rates_tab_selected") { putString("tab", tab) }

    /** Analiz grafiğinde seçilen zaman aralığı — uzun geçmiş gerçekten gerekli mi. */
    fun logAnalysisRangeSelected(range: String) = log("analysis_range_selected") {
        putString("range", range)
    }

    // ── Paywall / abonelik ───────────────────────────────────────────────────

    fun logPaywallShown(context: String) = log("paywall_shown") { putString("context", context) }

    fun logPaywallPlanSelected(plan: String) = log("paywall_plan_selected") {
        putString("plan", plan)
    }

    fun logPaywallCtaTapped(plan: String, hasTrial: Boolean) = log("paywall_cta_tapped") {
        putString("plan", plan)
        putBoolean("has_trial", hasTrial)
    }

    /**
     * Paywall satın alma yapılmadan kapatıldı. [secondsShown] refleks kapatma
     * ile "okudu ama ikna olmadı"yı ayırır.
     */
    fun logPaywallDismissed(context: String, secondsShown: Int) = log("paywall_dismissed") {
        putString("context", context)
        putLong("seconds_shown", secondsShown.toLong())
    }

    fun logSubscriptionPurchased(plan: String, hadTrial: Boolean) = log("subscription_purchased") {
        putString("plan", plan)
        putBoolean("had_trial", hadTrial)
    }

    /**
     * Satın alma tamamlanmadı. [reason] olmadan kullanıcı iptali ile gerçek
     * store hatası aynı sayıya düşüyor ve huninin son adımı okunamıyor.
     *
     * @param reason "cancelled" (kullanıcı Play sayfasını kapattı) ya da "error"
     * @param errorCode RevenueCat/Play Billing hata kodu; iptalde null
     */
    fun logSubscriptionPurchaseFailed(plan: String, reason: String, errorCode: Int?) =
        log("subscription_purchase_failed") {
            putString("plan", plan)
            putString("reason", reason)
            errorCode?.let { putLong("error_code", it.toLong()) }
        }

    fun logSubscriptionRestored() = log("subscription_restored") {}

    // ── Puan istemi ──────────────────────────────────────────────────────────

    /**
     * Play puan istemini açmayı denedik. Play kendi kotasına göre pencereyi hiç
     * göstermeyebilir ve bunu bize bildirmiyor — yani bu olay "istem gösterildi"
     * değil, "isteyebildiğimiz an geldi" demek.
     */
    fun logReviewPromptRequested(assetCount: Int) = log("review_prompt_requested") {
        putLong("asset_count", assetCount.toLong())
    }

    // ── İzinler ──────────────────────────────────────────────────────────────

    /**
     * Bildirim izni diyalogunun sonucu — iOS'taki `att_result`'ın Android
     * karşılığı. Yalnızca reddi loglamak izin oranını hesaplanamaz bırakırdı;
     * her sonuç tek olaya yazılıyor.
     */
    fun logNotificationPermissionResult(granted: Boolean) =
        log("notification_permission_result") {
            putString("status", if (granted) "granted" else "denied")
        }

    // ── Reklam ───────────────────────────────────────────────────────────────

    /**
     * Olay adı iOS ile birebir aynı olsun diye `<adType>_ad_<action>` biçiminde
     * kuruluyor: `app_open_ad_loaded`, `interstitial_ad_dismissed`,
     * `banner_ad_impression`…
     *
     * @param action "loaded" | "load_failed" | "will_present" | "did_present" |
     *   "dismissed" | "present_failed" | "impression" | "clicked"
     */
    fun logAdEvent(action: String, adType: String, errorMessage: String? = null) =
        log("${adType}_ad_$action") {
            putString("ad_type", adType)
            errorMessage?.let { putString("error_message", it.take(100)) }
        }

    /**
     * AdMob'un impression bazlı gelir bildirimi (`OnPaidEventListener`).
     * Firebase'in standart `ad_impression` olayı olarak yazılıyor; GA4 bunu
     * gelir sayıyor, yani AdMob↔GA4 hesap bağlantısı olmadan da reklam geliri
     * raporlarda görünüyor.
     */
    fun logAdRevenue(
        valueMicros: Long,
        currencyCode: String,
        precisionType: Int,
        format: String,
        adUnitId: String,
        source: String?
    ) = log(FirebaseAnalytics.Event.AD_IMPRESSION) {
        putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob")
        putString(FirebaseAnalytics.Param.AD_FORMAT, format)
        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
        putString(FirebaseAnalytics.Param.AD_SOURCE, source ?: "unknown")
        putLong("precision", precisionType.toLong())
        // GA4, currency geçersizse `value`'yu sessizce yok sayıyor — gelir
        // kaybolur ve nedeni raporda görünmez. Geçerli ISO-4217 yoksa olay yine
        // gönderiliyor (impression sayımı için) ama tutar yazılmıyor.
        if (currencyCode.length == 3) {
            putString(FirebaseAnalytics.Param.CURRENCY, currencyCode)
            // valueMicros mikro cinsinden: 1.000.000 mikro = 1 birim.
            putDouble(FirebaseAnalytics.Param.VALUE, valueMicros / 1_000_000.0)
        } else {
            putString("invalid_currency", currencyCode.ifEmpty { "empty" })
        }
    }

    private inline fun log(name: String, block: Bundle.() -> Unit) {
        analytics.logEvent(name, Bundle().apply(block))
    }
}
