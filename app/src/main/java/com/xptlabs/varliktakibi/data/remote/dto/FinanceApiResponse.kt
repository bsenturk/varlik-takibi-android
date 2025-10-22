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

    @SerializedName("CEYREK")
    val ceyrek: CommonRateDto?, // Quarter Gold

    @SerializedName("YARIM")
    val yarim: CommonRateDto?, // Half Gold

    @SerializedName("TAM")
    val tam: CommonRateDto?, // Full Gold

    @SerializedName("CUMHURIYET")
    val cumhuriyet: CommonRateDto?, // Republic Gold

    @SerializedName("ATA")
    val ata: CommonRateDto?, // Ata Gold

    @SerializedName("RESAT")
    val resat: CommonRateDto?, // Resat Gold

    @SerializedName("HAMIT")
    val hamit: CommonRateDto?, // Hamit Gold

    @SerializedName("BESLI")
    val besli: CommonRateDto?, // Five Gold

    @SerializedName("GREMSE")
    val gremse: CommonRateDto?, // Gremse Gold

    @SerializedName("14AYAR")
    val ayar14: CommonRateDto?, // 14 Carat Gold

    @SerializedName("18AYAR")
    val ayar18: CommonRateDto?, // 18 Carat Gold

    @SerializedName("IKIbucuk")
    val ikiBucuk: CommonRateDto?, // Two and Half Gold

    @SerializedName("22AYAR")
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
