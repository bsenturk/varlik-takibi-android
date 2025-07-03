package com.xptlabs.varliktakibi.data.remote.scraper

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface AssetTrackerWebService {

    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: tr-TR,tr;q=0.9,en;q=0.8",
        "Accept-Encoding: gzip, deflate, br",
        "Connection: keep-alive",
        "Upgrade-Insecure-Requests: 1"
    )
    @GET("/")
    suspend fun getGoldRates(): Response<String>

    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: tr-TR,tr;q=0.9,en;q=0.8",
        "Accept-Encoding: gzip, deflate, br",
        "Connection: keep-alive",
        "Upgrade-Insecure-Requests: 1"
    )
    @GET("/")
    suspend fun getCurrencyRates(): Response<String>
}