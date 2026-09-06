package com.xptlabs.varliktakibi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.core.model.Currency
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.prefs.DarkModePreference
import com.xptlabs.varliktakibi.push.PushRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isPro: Boolean = false,
    val currency: Currency = Currency.TRY,
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val versionLabel: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val purchaseManager: PurchaseManager,
    private val pushRegistrar: PushRegistrar,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    private val notificationsEnabled = MutableStateFlow(pushRegistrar.notificationsEnabled())

    val uiState: StateFlow<SettingsUiState> = combine(
        purchaseManager.isPro,
        prefs.selectedCurrency,
        prefs.darkMode,
        notificationsEnabled
    ) { isPro, currency, darkMode, notifications ->
        SettingsUiState(
            isPro = isPro,
            currency = currency,
            darkMode = darkMode,
            notificationsEnabled = notifications,
            versionLabel = "Sürüm ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Ekran her öne geldiğinde: izin sistem ayarlarından değişmiş olabilir. */
    fun refreshNotificationStatus() {
        val enabled = pushRegistrar.notificationsEnabled()
        notificationsEnabled.value = enabled
        pushRegistrar.setEnabled(enabled)
    }

    fun setCurrency(currency: Currency) = viewModelScope.launch {
        prefs.setSelectedCurrency(currency)
        analytics.logCurrencyChanged(currency.code)
    }

    fun setDarkMode(preference: DarkModePreference) = viewModelScope.launch {
        prefs.setDarkMode(preference)
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val restored = purchaseManager.restore()
        if (restored) analytics.logSubscriptionRestored()
        onResult(restored)
    }
}
