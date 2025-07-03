package com.xptlabs.varliktakibi.data.repository

import com.xptlabs.varliktakibi.data.local.preferences.PreferencesDataSource
import com.xptlabs.varliktakibi.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : OnboardingRepository {

    override suspend fun setOnboardingCompleted() {
        preferencesDataSource.setOnboardingCompleted()
    }

    override suspend fun isOnboardingCompleted(): Flow<Boolean> {
        return preferencesDataSource.isOnboardingCompleted()
    }

    override suspend fun setNotificationPermissionRequested() {
        preferencesDataSource.setNotificationPermissionRequested()
    }

    override suspend fun isNotificationPermissionRequested(): Flow<Boolean> {
        return preferencesDataSource.isNotificationPermissionRequested()
    }
}