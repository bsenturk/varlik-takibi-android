package com.xptlabs.varliktakibi.domain.repository

import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface RateRepository {
    suspend fun refreshGoldRates(): Result<List<RateEntity>>
    suspend fun refreshCurrencyRates(): Result<List<RateEntity>>
    suspend fun refreshAllRates(): Result<Pair<List<RateEntity>, List<RateEntity>>>
    fun getGoldRates(): Flow<List<RateEntity>>
    fun getCurrencyRates(): Flow<List<RateEntity>>
    fun getAllRates(): Flow<List<RateEntity>>
    suspend fun getRateById(id: String): RateEntity?
    suspend fun getLastUpdateTime(): Date?
}