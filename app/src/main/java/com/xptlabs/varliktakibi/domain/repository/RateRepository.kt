package com.xptlabs.varliktakibi.domain.repository

import com.xptlabs.varliktakibi.domain.models.Rate
import kotlinx.coroutines.flow.Flow

interface RateRepository {
    suspend fun refreshGoldRates(): Result<List<Rate>>
    suspend fun refreshCurrencyRates(): Result<List<Rate>>
    suspend fun refreshAllRates(): Result<Pair<List<Rate>, List<Rate>>>
    fun getLastGoldRates(): Flow<List<Rate>>
    fun getLastCurrencyRates(): Flow<List<Rate>>
}