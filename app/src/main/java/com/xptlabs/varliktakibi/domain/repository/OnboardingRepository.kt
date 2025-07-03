package com.xptlabs.varliktakibi.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    suspend fun setOnboardingCompleted()
    suspend fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setNotificationPermissionRequested()
    suspend fun isNotificationPermissionRequested(): Flow<Boolean>
}