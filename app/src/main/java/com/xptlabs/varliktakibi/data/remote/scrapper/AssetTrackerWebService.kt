package com.xptlabs.varliktakibi.data.remote.scraper

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface AssetTrackerWebService {

    @Headers(
        "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: tr-TR,tr;q=0.9,en;q=0.8",
        "Accept-Encoding: gzip, deflate, br",
        "Connection: keep-alive"
    )
    @GET("https://altin.doviz.com")
    suspend fun getGoldRates(): Response<String>

    @Headers(
        "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: tr-TR,tr;q=0.9,en;q=0.8",
        "Accept-Encoding: gzip, deflate, br",
        "Connection: keep-alive"
    )
    @GET("https://kur.doviz.com")
    suspend fun getCurrencyRates(): Response<String>
}