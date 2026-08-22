package com.xptlabs.varliktakibi.data.repository

import android.util.Log
import com.xptlabs.varliktakibi.data.local.dao.RateDao
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.data.remote.AssetTrackerRemoteDataSource
import com.xptlabs.varliktakibi.domain.repository.RateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val remoteDataSource: AssetTrackerRemoteDataSource,
    private val rateDao: RateDao
) : RateRepository {

    companion object {
        private const val TAG = "RateRepositoryImpl"
    }

    override suspend fun refreshGoldRates(): Result<List<RateEntity>> {
        // Now uses the unified Finance API
        return refreshAllRatesFromApi()
    }

    override suspend fun refreshCurrencyRates(): Result<List<RateEntity>> {
        // Now uses the unified Finance API
        return refreshAllRatesFromApi()
    }

    override suspend fun refreshAllRates(): Result<Pair<List<RateEntity>, List<RateEntity>>> {
        return try {
            Log.d(TAG, "Refreshing all rates from unified Finance API")
            val result = refreshAllRatesFromApi()

            return if (result.isSuccess) {
                val allRates = result.getOrNull() ?: emptyList()
                val goldRates = allRates.filter { it.type == "GOLD" || it.type == "SILVER" }
                val currencyRates = allRates.filter { it.type == "CURRENCY" }
                Log.d(TAG, "Successfully split rates - Gold: ${goldRates.size}, Currency: ${currencyRates.size}")
                Result.success(Pair(goldRates, currencyRates))
            } else {
                val error = result.exceptionOrNull() ?: Exception("Unknown error")
                Log.e(TAG, "Failed to refresh all rates: ${error.message}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while refreshing all rates", e)
            Result.failure(e)
        }
    }

    /**
     * Internal method to fetch all rates from the new Finance API
     */
    private suspend fun refreshAllRatesFromApi(): Result<List<RateEntity>> {
        return try {
            Log.d(TAG, "Fetching rates from Finance API")
            val result = remoteDataSource.getAllRates()
            result.onSuccess { rates ->
                Log.d(TAG, "Successfully fetched ${rates.size} rates from API")
                rateDao.insertRates(rates)
                Log.d(TAG, "All rates saved to database")
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch rates from API: ${error.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching rates from API", e)
            Result.failure(e)
        }
    }

    override fun getGoldRates(): Flow<List<RateEntity>> {
        return rateDao.getGoldRates()
    }

    override fun getCurrencyRates(): Flow<List<RateEntity>> {
        return rateDao.getCurrencyRates()
    }

    override fun getAllRates(): Flow<List<RateEntity>> {
        return rateDao.getAllRates()
    }

    override suspend fun getRateById(id: String): RateEntity? {
        return rateDao.getRateById(id)
    }

    override suspend fun getLastUpdateTime(): java.util.Date? {
        return rateDao.getLastUpdateTime()
    }
}