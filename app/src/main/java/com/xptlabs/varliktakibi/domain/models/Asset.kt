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
    val purchaseRate: Double, // Rate at time of purchase (for P/L calculation)
    val dateAdded: Date,
    val lastUpdated: Date
) {
    val totalValue: Double
        get() = amount * currentPrice

    val totalInvestment: Double
        get() = amount * purchaseRate

    val profitLoss: Double
        get() = totalValue - totalInvestment

    val profitLossPercentage: Double
        get() = if (totalInvestment > 0) {
            (profitLoss / totalInvestment) * 100
        } else 0.0
}

enum class AssetType(val displayName: String, val unit: String, val apiKey: String) {
    // Gold Products
    GOLD("Gram Altın", "gram", "GRA"),
    GOLD_QUARTER("Çeyrek Altın", "adet", "CEYREK"),
    GOLD_HALF("Yarım Altın", "adet", "YARIM"),
    GOLD_FULL("Tam Altın", "adet", "TAM"),
    GOLD_REPUBLIC("Cumhuriyet Altını", "adet", "CUMHURIYET"),
    GOLD_ATA("Ata Altın", "adet", "ATA"),
    GOLD_RESAT("Reşat Altın", "adet", "RESAT"),
    GOLD_HAMIT("Hamit Altın", "adet", "HAMIT"),
    GOLD_BESLI("Beşli Altın", "adet", "BESLI"),
    GOLD_GREMSE("Gremse Altın", "adet", "GREMSE"),
    GOLD_14_CARAT("14 Ayar Altın", "gram", "14AYAR"),
    GOLD_18_CARAT("18 Ayar Altın", "gram", "18AYAR"),
    GOLD_TWO_HALF("İki Buçuk Altın", "adet", "IKIbucuk"),
    GOLD_22_CARAT_BRACELET("22 Ayar Bilezik", "gram", "22AYAR"),

    // Precious Metals
    SILVER("Gram Gümüş", "gram", "GUMUS"),

    // Currencies
    USD("Dolar", "USD", "USD"),
    EUR("Euro", "EUR", "EUR"),
    GBP("Sterlin", "GBP", "GBP"),
    TRY("Türk Lirası", "TRY", "TRY");

    companion object {
        fun fromApiKey(key: String): AssetType? {
            return entries.find { it.apiKey == key }
        }
    }
}