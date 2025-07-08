package com.xptlabs.varliktakibi.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.local.preferences.PreferencesDataSource
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val assetRepository: AssetRepository,
    private val analyticsManager: FirebaseAnalyticsManager
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
                    darkModePreference = preference
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
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "notification_settings_opened",
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
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

    fun openAppStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
            context.startActivity(intent)

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "app_store_opened",
                parameters = emptyMap()
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
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
            }

            context.startActivity(Intent.createChooser(emailIntent, "E-posta Gönder"))

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "feedback_sent",
                parameters = mapOf("category" to category.lowercase())
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
            val shareText = "Varlık Takibi uygulamasını keşfedin! Altın ve döviz varlıklarınızı kolayca takip edin.\n\n$PLAY_STORE_URL"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Varlık Takibi Uygulaması")
            }

            context.startActivity(Intent.createChooser(shareIntent, "Uygulamayı Paylaş"))

            // Analytics
            analyticsManager.logCustomEvent(
                eventName = "app_shared",
                parameters = emptyMap()
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

                analyticsManager.logCustomEvent(
                    eventName = "debug_data_cleared",
                    parameters = emptyMap()
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
                "test_source" to "settings_debug"
            )
        )
    }
}