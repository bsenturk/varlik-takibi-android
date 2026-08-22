package com.xptlabs.varliktakibi.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response model for https://finance.truncgil.com/api/today.json
 */
data class FinanceApiResponse(
    @SerializedName("Rates")
    val rates: RatesDto
)

data class RatesDto(
    @SerializedName("USD")
    val usd: CommonRateDto?,

    @SerializedName("EUR")
    val eur: CommonRateDto?,

    @SerializedName("GBP")
    val gbp: CommonRateDto?,

    @SerializedName("GRA")
    val gra: CommonRateDto?, // Gram Gold

    @SerializedName("CEYREKALTIN")
    val ceyrek: CommonRateDto?, // Quarter Gold

    @SerializedName("YARIMALTIN")
    val yarim: CommonRateDto?, // Half Gold

    @SerializedName("TAMALTIN")
    val tam: CommonRateDto?, // Full Gold

    @SerializedName("CUMHURIYETALTINI")
    val cumhuriyet: CommonRateDto?, // Republic Gold

    @SerializedName("ATAALTIN")
    val ata: CommonRateDto?, // Ata Gold

    @SerializedName("RESATALTIN")
    val resat: CommonRateDto?, // Resat Gold

    @SerializedName("HAMITALTIN")
    val hamit: CommonRateDto?, // Hamit Gold

    @SerializedName("BESLIALTIN")
    val besli: CommonRateDto?, // Five Gold

    @SerializedName("GREMSEALTIN")
    val gremse: CommonRateDto?, // Gremse Gold

    @SerializedName("14AYARALTIN")
    val ayar14: CommonRateDto?, // 14 Carat Gold

    @SerializedName("18AYARALTIN")
    val ayar18: CommonRateDto?, // 18 Carat Gold

    @SerializedName("IKIBUCUKALTIN")
    val ikiBucuk: CommonRateDto?, // Two and Half Gold

    @SerializedName("YIA")  // 22 Ayar Bilezik = YIA in API
    val ayar22: CommonRateDto?, // 22 Carat Bracelet

    @SerializedName("GUMUS")
    val gumus: CommonRateDto? // Silver
)

data class CommonRateDto(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("change")
    val change: Double? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("buying")
    val buying: Double? = null,

    @SerializedName("selling")
    val selling: Double? = null
)
