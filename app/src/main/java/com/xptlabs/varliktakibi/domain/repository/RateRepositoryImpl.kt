package com.xptlabs.varliktakibi.data.repository

import com.xptlabs.varliktakibi.data.remote.AssetTrackerRemoteDataSource
import com.xptlabs.varliktakibi.domain.models.Rate
import com.xptlabs.varliktakibi.domain.repository.RateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val remoteDataSource: AssetTrackerRemoteDataSource
) : RateRepository {

    // In-memory cache for rates
    private val _lastGoldRates = MutableStateFlow<List<Rate>>(emptyList())
    private val _lastCurrencyRates = MutableStateFlow<List<Rate>>(emptyList())

    override suspend fun refreshGoldRates(): Result<List<Rate>> {
        return try {
            val result = remoteDataSource.getGoldRates()
            result.onSuccess { rates ->
                val domainRates = rates.map { it.toDomain() }
                _lastGoldRates.value = domainRates
            }
            result.map { dtoList -> dtoList.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshCurrencyRates(): Result<List<Rate>> {
        return try {
            val result = remoteDataSource.getCurrencyRates()
            result.onSuccess { rates ->
                val domainRates = rates.map { it.toDomain() }
                _lastCurrencyRates.value = domainRates
            }
            result.map { dtoList -> dtoList.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshAllRates(): Result<Pair<List<Rate>, List<Rate>>> {
        return try {
            val goldResult = refreshGoldRates()
            val currencyResult = refreshCurrencyRates()

            if (goldResult.isSuccess && currencyResult.isSuccess) {
                Result.success(
                    Pair(
                        goldResult.getOrDefault(emptyList()),
                        currencyResult.getOrDefault(emptyList())
                    )
                )
            } else {
                val goldError = goldResult.exceptionOrNull()
                val currencyError = currencyResult.exceptionOrNull()
                val combinedError = goldError ?: currencyError ?: Exception("Unknown error")
                Result.failure(combinedError)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLastGoldRates(): Flow<List<Rate>> = _lastGoldRates.asStateFlow()

    override fun getLastCurrencyRates(): Flow<List<Rate>> = _lastCurrencyRates.asStateFlow()
}