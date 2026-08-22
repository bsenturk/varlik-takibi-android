package com.xptlabs.varliktakibi.core.model

/** Toplam değerin gösterileceği para birimi (yalnızca sunum; hesap hep TL'de). */
enum class Currency(val code: String, val symbol: String, val flag: String) {
    TRY("TRY", "₺", "🇹🇷"),
    USD("USD", "$", "🇺🇸"),
    EUR("EUR", "€", "🇪🇺"),
    GBP("GBP", "£", "🇬🇧");

    val displayName: String get() = "$flag $code"

    companion object {
        fun fromCode(code: String?): Currency =
            entries.firstOrNull { it.code == code } ?: TRY
    }
}
