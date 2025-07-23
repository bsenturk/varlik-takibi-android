package com.xptlabs.varliktakibi.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.local.preferences.PreferencesDataSource
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.notifications.AppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val notificationsEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val assetRepository: AssetRepository,
    private val analyticsManager: FirebaseAnalyticsManager,
    val notificationManager: AppNotificationManager
) : ViewModel() {

    companion object {
        private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.xptlabs.varliktakibi"
        private const val DEVELOPER_EMAIL = "buraksenturktr@icloud.com"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesDataSource.getDarkModePreference().collect { preferenceString ->
                val preference = try {
                    DarkModePreference.valueOf(preferenceString)
                } catch (e: IllegalArgumentException) {
                    DarkModePreference.SYSTEM
                }

                _uiState.value = _uiState.value.copy(
                    darkModePreference = preference,
                    notificationsEnabled = notificationManager.areNotificationsEnabled()
                )
            }
        }
    }

    fun setDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            preferencesDataSource.setDarkModePreference(preference.name)
            _uiState.value = _uiState.value.copy(
                darkModePreference = preference
            )

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "theme_changed",
                parameters = mapOf("theme" to preference.name.lowercase())
            )
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                when {
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                        // Android 8+ için notification settings
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        // Eski versiyonlar için app details
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "notification_settings_opened",
                parameters = mapOf(
                    "current_status" to notificationManager.areNotificationsEnabled(),
                    "method" to "settings_screen"
                )
            )
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Log error
                analyticsManager.logError(
                    errorType = "settings_open_failed",
                    errorMessage = ex.message ?: "Failed to open settings"
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendTestNotification() {
        try {
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.sendTestNotification()

                // Analytics
                analyticsManager.logCustomEvent(
                    eventName = "test_notification_sent",
                    parameters = mapOf(
                        "source" to "settings_screen",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                // Notification izni yok, capability'yi kaydet
                notificationManager.forceRegisterNotificationCapability()

                analyticsManager.logCustomEvent(
                    eventName = "test_notification_failed",
                    parameters = mapOf(
                        "reason" to "permission_denied",
                        "source" to "settings_screen"
                    )
                )
            }
        } catch (e: Exception) {
            analyticsManager.logError(
                errorType = "test_notification_failed",
                errorMessage = e.message ?: "Failed to send test notification"
            )
        }
    }

    fun openAppStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
            context.startActivity(intent)

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "app_store_opened",
                parameters = mapOf("source" to "settings_screen")
            )
        } catch (e: Exception) {
            analyticsManager.logError(
                errorType = "app_store_open_failed",
                errorMessage = e.message ?: "Failed to open Play Store"
            )
        }
    }

    fun sendFeedback(context: Context, category: String, message: String) {
        try {
            val subject = "Varlık Takibi - $category"
            val deviceInfo = buildString {
                appendLine("--- Cihaz Bilgileri ---")
                appendLine("Uygulama Versiyonu: ${com.xptlabs.varliktakibi.BuildConfig.VERSION_NAME}")
                appendLine("Android Versiyonu: ${android.os.Build.VERSION.RELEASE}")
                appendLine("Cihaz Modeli: ${android.os.Build.MODEL}")
                appendLine("Bildirimler: ${if (notificationManager.areNotificationsEnabled()) "Açık" else "Kapalı"}")
                appendLine("--- Mesaj ---")
                appendLine(message)
            }

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, deviceInfo)
            }

            context.startActivity(Intent.createChooser(emailIntent, "E-posta Gönder"))

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "feedback_sent",
                parameters = mapOf(
                    "category" to category.lowercase(),
                    "has_device_info" to true
                )
            )
        } catch (e: Exception) {
            analyticsManager.logError(
                errorType = "feedback_send_failed",
                errorMessage = e.message ?: "Failed to send feedback"
            )
        }
    }

    fun shareApp(context: Context) {
        try {
            val shareText = buildString {
                appendLine("🏦 Varlık Takibi uygulamasını keşfedin!")
                appendLine()
                appendLine("✨ Özellikler:")
                appendLine("📊 Altın ve döviz takibi")
                appendLine("📈 Güncel kurlar")
                appendLine("💰 Kar/zarar hesaplama")
                appendLine("📱 Kolay kullanım")
                appendLine()
                appendLine("İndir: $PLAY_STORE_URL")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Varlık Takibi Uygulaması")
            }

            context.startActivity(Intent.createChooser(shareIntent, "Uygulamayı Paylaş"))

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "app_shared",
                parameters = mapOf("source" to "settings_screen")
            )
        } catch (e: Exception) {
            analyticsManager.logError(
                errorType = "app_share_failed",
                errorMessage = e.message ?: "Failed to share app"
            )
        }
    }

    // Debug functions
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Clear preferences
                preferencesDataSource.clearAllPreferences()

                // Clear all assets
                assetRepository.deleteAllAssets()

                // Cancel all notifications
                notificationManager.cancelAllScheduledNotifications()

                analyticsManager.logCustomEvent(
                    eventName = "debug_data_cleared",
                    parameters = mapOf("source" to "settings_debug")
                )
            } catch (e: Exception) {
                analyticsManager.logError(
                    errorType = "debug_clear_failed",
                    errorMessage = e.message ?: "Failed to clear data"
                )
            }
        }
    }

    fun testAnalytics() {
        analyticsManager.logCustomEvent(
            eventName = "debug_analytics_test",
            parameters = mapOf(
                "test_time" to System.currentTimeMillis(),
                "test_source" to "settings_debug",
                "notifications_enabled" to notificationManager.areNotificationsEnabled(),
                "app_version" to com.xptlabs.varliktakibi.BuildConfig.VERSION_NAME
            )
        )
    }

    // Notification status güncelleme
    fun refreshNotificationStatus() {
        _uiState.value = _uiState.value.copy(
            notificationsEnabled = notificationManager.areNotificationsEnabled()
        )
    }

    // Notification capability'sini force register et
    fun forceRegisterNotificationCapability() {
        try {
            notificationManager.forceRegisterNotificationCapability()

            analyticsManager.logCustomEvent(
                eventName = "notification_capability_registered",
                parameters = mapOf(
                    "source" to "settings_debug",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            analyticsManager.logError(
                errorType = "notification_capability_register_failed",
                errorMessage = e.message ?: "Failed to register notification capability"
            )
        }
    }
}