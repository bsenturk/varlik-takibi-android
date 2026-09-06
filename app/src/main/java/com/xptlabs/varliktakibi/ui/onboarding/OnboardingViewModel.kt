package com.xptlabs.varliktakibi.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.push.PushRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val pushRegistrar: PushRegistrar,
    private val analytics: FirebaseAnalyticsManager
) : ViewModel() {

    fun onNotificationPermissionResult(granted: Boolean) {
        analytics.logNotificationPermissionResult(granted)
        // İzin verildiyse token'ı hemen kaydet; verilmediyse backend'deki bayrağı
        // indir ki bu cihaza boşuna push denenmesin.
        if (granted) pushRegistrar.sync() else pushRegistrar.setEnabled(false)
    }

    /**
     * [onDone] bilerek yazmalardan **sonra** çağrılıyor: ana ekran bayrakları
     * okuyor, geçişi önce yaparsak henüz yazılmamış değeri görüp ilk varlık
     * yönlendirmesini atlıyor ve bayrak bir sonraki açılışa sarkıyordu.
     */
    fun complete(reachedPage: Int, skipped: Boolean, onDone: () -> Unit) {
        analytics.logOnboardingCompleted(reachedPage, skipped)
        viewModelScope.launch {
            prefs.setOnboardingCompleted()
            // iOS'taki devir: tanıtımdan sonra varlık ekleme akışı kendiliğinden
            // açılsın (ilk gerçek varlık = "aha" anı), ekleme kapanınca paywall
            // bir kez görünsün.
            prefs.setPendingFirstAssetAdd(true)
            prefs.setPendingOnboardingPaywall(true)
            onDone()
        }
    }
}
