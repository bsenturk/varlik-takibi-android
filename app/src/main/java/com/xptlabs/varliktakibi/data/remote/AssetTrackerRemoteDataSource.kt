package com.xptlabs.varliktakibi.data.remote

import android.util.Log
import com.xptlabs.varliktakibi.data.local.entities.RateEntity
import com.xptlabs.varliktakibi.data.remote.parser.AssetTrackerHtmlParser
import com.xptlabs.varliktakibi.data.remote.scraper.AssetTrackerWebService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetTrackerRemoteDataSource @Inject constructor(
    @GoldWebService private val goldWebService: AssetTrackerWebService,
    @CurrencyWebService private val currencyWebService: AssetTrackerWebService,
    private val htmlParser: AssetTrackerHtmlParser
) {

    companion object {
        private const val TAG = "RemoteDataSource"
    }

    suspend fun getGoldRates(): Result<List<RateEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching gold rates from https://altin.doviz.com")
            val response = goldWebService.getGoldRates()
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Successfully fetched gold rates HTML, parsing...")
                val rates = htmlParser.parseGoldRates(response.body()!!)
                Log.d(TAG, "Parsed ${rates.size} gold rates")
                Result.success(rates)
            } else {
                val error = "Failed to fetch gold rates: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching gold rates", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrencyRates(): Result<List<RateEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching currency rates from https://kur.doviz.com")
            val response = currencyWebService.getCurrencyRates()
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Successfully fetched currency rates HTML, parsing...")
                val rates = htmlParser.parseCurrencyRates(response.body()!!)
                Log.d(TAG, "Parsed ${rates.size} currency rates")
                Result.success(rates)
            } else {
                val error = "Failed to fetch currency rates: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching currency rates", e)
            Result.failure(e)
        }
    }
}

// Qualifiers for different web services
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoldWebService

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrencyWebService