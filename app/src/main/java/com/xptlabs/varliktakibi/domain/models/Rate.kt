package com.xptlabs.varliktakibi.domain.models

import java.util.Date

data class Rate(
    val name: String,
    val code: String?,
    val buyPrice: Double,
    val sellPrice: Double,
    val change: Double,
    val changePercent: Double,
    val lastUpdate: Date = Date()
) {
    val isPositiveChange: Boolean
        get() = change >= 0

    val formattedBuyPrice: String
        get() = "₺%.2f".format(buyPrice)

    val formattedSellPrice: String
        get() = "₺%.2f".format(sellPrice)

    val formattedChange: String
        get() = "${if (change >= 0) "+" else ""}%.2f%%".format(changePercent)
}