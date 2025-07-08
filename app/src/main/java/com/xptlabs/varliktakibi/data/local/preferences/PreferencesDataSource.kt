package com.xptlabs.varliktakibi.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val NOTIFICATION_PERMISSION_REQUESTED_KEY = booleanPreferencesKey("notification_permission_requested")
        private val DARK_MODE_PREFERENCE_KEY = stringPreferencesKey("dark_mode_preference")
        private val ANALYTICS_ENABLED_KEY = booleanPreferencesKey("analytics_enabled")
        private val LAST_APP_VERSION_KEY = stringPreferencesKey("last_app_version")
    }

    // Existing methods
    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    fun isOnboardingCompleted(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setNotificationPermissionRequested() {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_PERMISSION_REQUESTED_KEY] = true
        }
    }

    fun isNotificationPermissionRequested(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATION_PERMISSION_REQUESTED_KEY] ?: false
    }

    // New settings methods
    suspend fun setDarkModePreference(preference: String) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_PREFERENCE_KEY] = preference
        }
    }

    fun getDarkModePreference(): Flow<String> = dataStore.data.map { preferences ->
        preferences[DARK_MODE_PREFERENCE_KEY] ?: "SYSTEM"
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED_KEY] = enabled
        }
    }

    fun isAnalyticsEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ANALYTICS_ENABLED_KEY] ?: true // Default to enabled
    }

    suspend fun setLastAppVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[LAST_APP_VERSION_KEY] = version
        }
    }

    fun getLastAppVersion(): Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_APP_VERSION_KEY] ?: ""
    }

    // Clear all preferences (for debug)
    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}