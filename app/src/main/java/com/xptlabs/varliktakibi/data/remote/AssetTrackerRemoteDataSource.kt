package com.xptlabs.varliktakibi.data.remote

import android.util.Log
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.data.remote.api.FinanceApiService
import com.xptlabs.varliktakibi.data.remote.mapper.FinanceApiMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetTrackerRemoteDataSource @Inject constructor(
    private val financeApiService: FinanceApiService
) {

    companion object {
        private const val TAG = "RemoteDataSource"
    }

    /**
     * Fetch all rates from the new Finance API
     * Replaces both getGoldRates() and getCurrencyRates()
     */
    suspend fun getAllRates(): Result<List<RateEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching rates from https://finance.truncgil.com/api/today.json")
            val response = financeApiService.getTodayRates()

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Successfully fetched rates from API")
                val rates = FinanceApiMapper.mapToRateEntities(response.body()!!)
                Log.d(TAG, "Mapped ${rates.size} rates from API")
                Result.success(rates)
            } else {
                val error = "Failed to fetch rates: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching rates from API", e)
            Result.failure(e)
        }
    }

    /**
     * Legacy method - delegates to getAllRates()
     * @deprecated Use getAllRates() instead
     */
    @Deprecated("Use getAllRates() instead", ReplaceWith("getAllRates()"))
    suspend fun getGoldRates(): Result<List<RateEntity>> = getAllRates()

    /**
     * Legacy method - delegates to getAllRates()
     * @deprecated Use getAllRates() instead
     */
    @Deprecated("Use getAllRates() instead", ReplaceWith("getAllRates()"))
    suspend fun getCurrencyRates(): Result<List<RateEntity>> = getAllRates()
}

// Qualifiers for different web services
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoldWebService

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrencyWebService