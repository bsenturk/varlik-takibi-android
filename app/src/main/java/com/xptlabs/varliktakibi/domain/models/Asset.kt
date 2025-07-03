package com.xptlabs.varliktakibi.domain.models

import java.util.Date

data class Asset(
    val id: String,
    val type: AssetType,
    val name: String,
    val amount: Double,
    val unit: String,
    val purchasePrice: Double,
    val currentPrice: Double,
    val dateAdded: Date,
    val lastUpdated: Date
) {
    val totalValue: Double
        get() = amount * currentPrice

    val totalInvestment: Double
        get() = amount * purchasePrice

    val profitLoss: Double
        get() = totalValue - totalInvestment

    val profitLossPercentage: Double
        get() = if (totalInvestment > 0) {
            (profitLoss / totalInvestment) * 100
        } else 0.0
}

enum class AssetType(val displayName: String, val unit: String) {
    GOLD("Gram Altın", "gram"),
    GOLD_QUARTER("Çeyrek Altın", "adet"),
    GOLD_HALF("Yarım Altın", "adet"),
    GOLD_FULL("Tam Altın", "adet"),
    GOLD_REPUBLIC("Cumhuriyet Altını", "adet"),
    GOLD_ATA("Ata Altın", "adet"),
    GOLD_RESAT("Reşat Altın", "adet"),
    GOLD_HAMIT("Hamit Altın", "adet"),
    USD("Dolar", "USD"),
    EUR("Euro", "EUR"),
    GBP("Sterlin", "GBP"),
    TRY("Türk Lirası", "TRY")
}