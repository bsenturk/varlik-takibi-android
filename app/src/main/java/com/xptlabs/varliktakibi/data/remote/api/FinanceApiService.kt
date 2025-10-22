package com.xptlabs.varliktakibi.data.remote.api

import com.xptlabs.varliktakibi.data.remote.dto.FinanceApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface FinanceApiService {

    @GET("api/today.json")
    suspend fun getTodayRates(): Response<FinanceApiResponse>
}
