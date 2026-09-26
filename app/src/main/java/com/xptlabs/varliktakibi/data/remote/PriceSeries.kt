package com.xptlabs.varliktakibi.data.remote

import kotlinx.serialization.Serializable

/** `price-chart` yanıtı. Seri ABD hisse/ETF'lerde USD, diğerlerinde TRY. */
@Serializable
data class PriceSeries(
    val symbol: String,
    val currency: String,
    val points: List<PricePoint> = emptyList()
)

/** t: epoch saniye, c: kapanış fiyatı. */
@Serializable
data class PricePoint(val t: Double, val c: Double)

/** Grafik aralığı; [key] backend'in beklediği değer. */
enum class ChartRange(val key: String) {
    WEEK("1h"), MONTH("1a"), QUARTER("3a"), YEAR("1y");

    val label: String get() = key.uppercase(java.util.Locale.forLanguageTag("tr-TR"))
}
