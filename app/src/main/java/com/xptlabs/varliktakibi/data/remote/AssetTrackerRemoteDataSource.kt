package com.xptlabs.varliktakibi.data.remote

import com.xptlabs.varliktakibi.data.remote.dto.RateDto
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

    suspend fun getGoldRates(): Result<List<RateDto>> = withContext(Dispatchers.IO) {
        try {
            val response = goldWebService.getGoldRates()
            if (response.isSuccessful && response.body() != null) {
                val rates = htmlParser.parseGoldRates(response.body()!!)
                Result.success(rates)
            } else {
                Result.failure(Exception("Failed to fetch gold rates: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrencyRates(): Result<List<RateDto>> = withContext(Dispatchers.IO) {
        try {
            val response = currencyWebService.getCurrencyRates()
            if (response.isSuccessful && response.body() != null) {
                val rates = htmlParser.parseCurrencyRates(response.body()!!)
                Result.success(rates)
            } else {
                Result.failure(Exception("Failed to fetch currency rates: ${response.code()}"))
            }
        } catch (e: Exception) {
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