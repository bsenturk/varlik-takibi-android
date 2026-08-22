package com.xptlabs.varliktakibi.domain.models

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    TRY("TRY", "₺", "Türk Lirası"),
    USD("USD", "$", "Dolar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "Sterlin");

    companion object {
        fun fromCode(code: String): Currency {
            return entries.find { it.code == code } ?: TRY
        }
    }
}
