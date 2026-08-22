package com.xptlabs.varliktakibi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Reklam fırsatında ne yapılacağı. */
enum class AdOpportunity { INTERSTITIAL, PAYWALL, NOTHING }

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    private val _onboardingCompleted = MutableStateFlow<Boolean?>(null)
    /** null = henüz okunmadı; splash bu sürede gösteriliyor. */
    val onboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingCompleted.value = prefs.onboardingCompleted.first()
        }
    }

    fun onOnboardingComplete() { _onboardingCompleted.value = true }

    /**
     * Onboarding devri: tanıtımdan sonra varlık ekleme akışı bir kez otomatik
     * açılır. Bayrak tüketilir ki sonraki açılışlarda tekrarlanmasın.
     */
    suspend fun consumePendingFirstAssetAdd(): Boolean {
        val pending = prefs.pendingFirstAssetAdd.first()
        if (pending) prefs.setPendingFirstAssetAdd(false)
        return pending
    }

    /**
     * Varlık ekleme ekranı kapandığında ne gösterilecek?
     *
     * - Onboarding devrinde: paywall (varlık eklenmese bile, iOS ile aynı).
     * - Normalde yalnızca gerçekten bir varlık eklendiyse: her 3. fırsatta
     *   interstitial yerine paywall, diğerlerinde interstitial.
     * - Pro kullanıcıda hiçbir şey.
     */
    suspend fun onAddAssetClosed(didAddAsset: Boolean, isPro: Boolean): AdOpportunity {
        if (prefs.pendingOnboardingPaywall.first()) {
            prefs.setPendingOnboardingPaywall(false)
            return if (isPro) AdOpportunity.NOTHING else AdOpportunity.PAYWALL
        }
        if (!didAddAsset || isPro) return AdOpportunity.NOTHING

        return if (prefs.nextAdOpportunity() % PAYWALL_EVERY == 0) AdOpportunity.PAYWALL
        else AdOpportunity.INTERSTITIAL
    }

    private companion object {
        /** Her 3. reklam fırsatında reklam yerine paywall (iOS AdPaywallGate). */
        const val PAYWALL_EVERY = 3
    }
}
