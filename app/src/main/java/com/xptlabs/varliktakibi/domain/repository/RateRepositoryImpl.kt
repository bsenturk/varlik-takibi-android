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
        return try {
            Log.d(TAG, "Refreshing gold rates")
            val result = remoteDataSource.getGoldRates()
            result.onSuccess { rates ->
                Log.d(TAG, "Successfully fetched ${rates.size} gold rates")
                rateDao.insertRates(rates)
                Log.d(TAG, "Gold rates saved to database")
            }.onFailure { error ->
                Log.e(TAG, "Failed to refresh gold rates: ${error.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while refreshing gold rates", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshCurrencyRates(): Result<List<RateEntity>> {
        return try {
            Log.d(TAG, "Refreshing currency rates")
            val result = remoteDataSource.getCurrencyRates()
            result.onSuccess { rates ->
                Log.d(TAG, "Successfully fetched ${rates.size} currency rates")
                rateDao.insertRates(rates)
                Log.d(TAG, "Currency rates saved to database")
            }.onFailure { error ->
                Log.e(TAG, "Failed to refresh currency rates: ${error.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while refreshing currency rates", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshAllRates(): Result<Pair<List<RateEntity>, List<RateEntity>>> {
        return try {
            Log.d(TAG, "Refreshing all rates")
            val goldResult = refreshGoldRates()
            val currencyResult = refreshCurrencyRates()

            if (goldResult.isSuccess && currencyResult.isSuccess) {
                val goldRates = goldResult.getOrDefault(emptyList())
                val currencyRates = currencyResult.getOrDefault(emptyList())
                Log.d(TAG, "Successfully refreshed all rates - Gold: ${goldRates.size}, Currency: ${currencyRates.size}")
                Result.success(Pair(goldRates, currencyRates))
            } else {
                val goldError = goldResult.exceptionOrNull()
                val currencyError = currencyResult.exceptionOrNull()
                val combinedError = goldError ?: currencyError ?: Exception("Unknown error")
                Log.e(TAG, "Failed to refresh all rates: ${combinedError.message}")
                Result.failure(combinedError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while refreshing all rates", e)
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