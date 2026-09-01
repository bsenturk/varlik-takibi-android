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

    fun logCurrencyChanged(currency: String) = log("currency_changed") {
        putString("currency", currency)
    }

    fun logOnboardingCompleted(reachedPage: Int, skipped: Boolean) =
        log("onboarding_completed") {
            putLong("reached_page", reachedPage.toLong())
            putBoolean("skipped", skipped)
        }

    // ── Varlık ───────────────────────────────────────────────────────────────

    fun logAssetAdded(category: String, symbol: String, isMerge: Boolean, hasPurchasePrice: Boolean) =
        log("asset_added") {
            putString("category", category)
            putString("symbol", symbol)
            putBoolean("is_merge", isMerge)
            putBoolean("has_purchase_price", hasPurchasePrice)
        }

    fun logAssetDeleted(category: String, symbol: String) = log("asset_deleted") {
        putString("category", category)
        putString("symbol", symbol)
    }

    fun logPremiumCategoryLocked(category: String) = log("premium_category_locked") {
        putString("category", category)
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

    fun logPaywallDismissed(context: String) = log("paywall_dismissed") {
        putString("context", context)
    }

    fun logSubscriptionPurchased(plan: String, hadTrial: Boolean) = log("subscription_purchased") {
        putString("plan", plan)
        putBoolean("had_trial", hadTrial)
    }

    fun logSubscriptionPurchaseFailed(plan: String) = log("subscription_purchase_failed") {
        putString("plan", plan)
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

    // ── Reklam ───────────────────────────────────────────────────────────────

    fun logAdEvent(event: String, adType: String, detail: String? = null) = log(event) {
        putString("ad_type", adType)
        detail?.let { putString("detail", it) }
    }

    // ── Hata ─────────────────────────────────────────────────────────────────

    fun logError(errorType: String, message: String) = log("app_error") {
        putString("error_type", errorType)
        putString("message", message.take(100))
    }

    private inline fun log(name: String, block: Bundle.() -> Unit) {
        analytics.logEvent(name, Bundle().apply(block))
    }
}
