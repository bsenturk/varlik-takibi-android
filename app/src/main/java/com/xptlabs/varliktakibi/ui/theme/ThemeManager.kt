package com.xptlabs.varliktakibi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xptlabs.varliktakibi.data.local.preferences.PreferencesDataSource
import com.xptlabs.varliktakibi.presentation.settings.DarkModePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : ViewModel() {

    private val _darkModePreference = MutableStateFlow(DarkModePreference.SYSTEM)
    val darkModePreference: StateFlow<DarkModePreference> = _darkModePreference.asStateFlow()

    init {
        loadThemePreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            preferencesDataSource.getDarkModePreference().collect { preferenceString ->
                val preference = try {
                    DarkModePreference.valueOf(preferenceString)
                } catch (e: IllegalArgumentException) {
                    DarkModePreference.SYSTEM
                }
                _darkModePreference.value = preference
            }
        }
    }

    fun setDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            preferencesDataSource.setDarkModePreference(preference.name)
            _darkModePreference.value = preference
        }
    }
}

@Composable
fun shouldUseDarkTheme(
    darkModePreference: DarkModePreference
): Boolean {
    return when (darkModePreference) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
}