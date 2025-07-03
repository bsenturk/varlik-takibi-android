package com.xptlabs.varliktakibi.data.remote.dto

data class RateResponse(
    val success: Boolean,
    val rates: List<RateDto>,
    val lastUpdate: String
)

data class RateDto(
    val name: String,
    val code: String?,
    val buyPrice: String,
    val sellPrice: String,
    val change: String,
    val changePercent: String
) {
    // Domain model'a dönüştürme
    fun toDomain(): com.xptlabs.varliktakibi.domain.models.Rate {
        return com.xptlabs.varliktakibi.domain.models.Rate(
            name = name,
            code = code,
            buyPrice = buyPrice.parsePrice(),
            sellPrice = sellPrice.parsePrice(),
            change = change.parseDouble(),
            changePercent = changePercent.parseDouble()
        )
    }

    private fun String.parsePrice(): Double {
        return this.replace(".", "")
            .replace(",", ".")
            .replace("₺", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun String.parseDouble(): Double {
        return this.replace("%", "")
            .replace("+", "")
            .replace("-", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }
}