package com.xptlabs.varliktakibi.domain.models

import java.util.Date

data class AssetPriceHistory(
    val id: String,
    val assetId: String,
    val assetType: AssetType,
    val date: Date,
    val price: Double,
    val amount: Double,
    val totalValue: Double,
    val createdAt: Date
)
