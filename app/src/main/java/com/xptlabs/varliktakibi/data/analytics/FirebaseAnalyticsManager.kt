// Firebase Analytics Manager - Android
package com.xptlabs.varliktakibi.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsManager @Inject constructor() {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    // Asset Events
    fun logAssetAdded(assetType: String, amount: Double) {
        firebaseAnalytics.logEvent("asset_added") {
            param("asset_type", assetType)
            param("amount", amount)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logAssetUpdated(assetType: String, oldAmount: Double, newAmount: Double) {
        firebaseAnalytics.logEvent("asset_updated") {
            param("asset_type", assetType)
            param("old_amount", oldAmount)
            param("new_amount", newAmount)
            param("amount_change", newAmount - oldAmount)
        }
    }

    fun logAssetDeleted(assetType: String, amount: Double) {
        firebaseAnalytics.logEvent("asset_deleted") {
            param("asset_type", assetType)
            param("amount", amount)
        }
    }

    // Portfolio Events
    fun logPortfolioViewed(totalValue: Double, assetCount: Int) {
        firebaseAnalytics.logEvent("portfolio_viewed") {
            param("total_value", totalValue)
            param("asset_count", assetCount.toLong())
        }
    }

    fun logPortfolioProfitLoss(profitLoss: Double, profitLossPercentage: Double) {
        firebaseAnalytics.logEvent("portfolio_profit_loss") {
            param("profit_loss", profitLoss)
            param("profit_loss_percentage", profitLossPercentage)
            param("is_profit", if (profitLoss >= 0) 1L else 0L)
        }
    }

    // Rate Events
    fun logRatesViewed(goldRateCount: Int, currencyRateCount: Int) {
        firebaseAnalytics.logEvent("rates_viewed") {
            param("gold_rate_count", goldRateCount.toLong())
            param("currency_rate_count", currencyRateCount.toLong())
        }
    }

    fun logRatesRefreshed(success: Boolean, errorMessage: String? = null) {
        firebaseAnalytics.logEvent("rates_refreshed") {
            param("success", if (success) 1L else 0L)
            errorMessage?.let { param("error_message", it) }
        }
    }

    // App Events
    fun logAppOpened(source: String = "launcher") {
        firebaseAnalytics.logEvent("app_opened") {
            param("source", source)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logScreenView(screenName: String, screenClass: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    // Onboarding Events
    fun logOnboardingStarted() {
        firebaseAnalytics.logEvent("onboarding_started") {
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logOnboardingCompleted(timeSpent: Long) {
        firebaseAnalytics.logEvent("onboarding_completed") {
            param("time_spent_seconds", timeSpent)
        }
    }

    fun logOnboardingSkipped(step: Int) {
        firebaseAnalytics.logEvent("onboarding_skipped") {
            param("step", step.toLong())
        }
    }

    // Permission Events
    fun logNotificationPermissionRequested() {
        firebaseAnalytics.logEvent("notification_permission_requested") {
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logNotificationPermissionGranted(granted: Boolean) {
        firebaseAnalytics.logEvent("notification_permission_result") {
            param("granted", if (granted) 1L else 0L)
        }
    }

    // Error Events
    fun logError(errorType: String, errorMessage: String, screenName: String? = null) {
        firebaseAnalytics.logEvent("app_error") {
            param("error_type", errorType)
            param("error_message", errorMessage)
            screenName?.let { param("screen_name", it) }
        }
    }

    // Background Sync Events
    fun logBackgroundSyncStarted() {
        firebaseAnalytics.logEvent("background_sync_started") {
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logBackgroundSyncCompleted(success: Boolean, syncedCount: Int) {
        firebaseAnalytics.logEvent("background_sync_completed") {
            param("success", if (success) 1L else 0L)
            param("synced_count", syncedCount.toLong())
        }
    }

    // User Properties
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    fun setUserPortfolioValue(totalValue: Double) {
        val valueRange = when {
            totalValue < 1000 -> "0-1K"
            totalValue < 5000 -> "1K-5K"
            totalValue < 10000 -> "5K-10K"
            totalValue < 50000 -> "10K-50K"
            totalValue < 100000 -> "50K-100K"
            else -> "100K+"
        }
        setUserProperty("portfolio_value_range", valueRange)
    }

    fun setUserAssetCount(assetCount: Int) {
        val countRange = when {
            assetCount == 0 -> "0"
            assetCount <= 2 -> "1-2"
            assetCount <= 5 -> "3-5"
            assetCount <= 10 -> "6-10"
            else -> "10+"
        }
        setUserProperty("asset_count_range", countRange)
    }

    // Custom Events
    fun logCustomEvent(eventName: String, parameters: Map<String, Any>) {
        firebaseAnalytics.logEvent(eventName) {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                }
            }
        }
    }

    // FirebaseAnalyticsManager.kt içerisine AdMob analytics için aşağıdaki methodları ekleyin:

    // AdMob Events
    fun logAdLoaded(adType: String, adUnitId: String) {
        firebaseAnalytics.logEvent("ad_loaded") {
            param("ad_type", adType)
            param("ad_unit_id", adUnitId)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logAdFailedToLoad(adType: String, adUnitId: String, errorCode: Int, errorMessage: String) {
        firebaseAnalytics.logEvent("ad_failed_to_load") {
            param("ad_type", adType)
            param("ad_unit_id", adUnitId)
            param("error_code", errorCode.toLong())
            param("error_message", errorMessage)
        }
    }

    fun logAdShown(adType: String, adUnitId: String) {
        firebaseAnalytics.logEvent("ad_shown") {
            param("ad_type", adType)
            param("ad_unit_id", adUnitId)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logAdClicked(adType: String, adUnitId: String) {
        firebaseAnalytics.logEvent("ad_clicked") {
            param("ad_type", adType)
            param("ad_unit_id", adUnitId)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logAdClosed(adType: String, adUnitId: String) {
        firebaseAnalytics.logEvent("ad_closed") {
            param("ad_type", adType)
            param("ad_unit_id", adUnitId)
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logAppOpenAdShown() {
        firebaseAnalytics.logEvent("app_open_ad_shown") {
            param("timestamp", System.currentTimeMillis())
        }
    }

    fun logBannerAdShown(screenName: String) {
        firebaseAnalytics.logEvent("banner_ad_shown") {
            param("screen_name", screenName)
            param("timestamp", System.currentTimeMillis())
        }
    }
}