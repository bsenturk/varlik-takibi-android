package com.xptlabs.varliktakibi.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            // Splash screen'i minimum 2 saniye göster
            delay(2000)

            val isOnboardingCompleted = onboardingRepository.isOnboardingCompleted().first()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                shouldNavigateToMain = isOnboardingCompleted,
                shouldNavigateToOnboarding = !isOnboardingCompleted
            )
        }
    }
}

data class SplashUiState(
    val isLoading: Boolean = true,
    val shouldNavigateToMain: Boolean = false,
    val shouldNavigateToOnboarding: Boolean = false
)