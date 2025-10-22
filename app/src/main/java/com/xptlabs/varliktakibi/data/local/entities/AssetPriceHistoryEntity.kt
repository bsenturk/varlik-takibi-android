package com.xptlabs.varliktakibi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "asset_price_history",
    indices = [
        Index(value = ["assetId", "date"], unique = true),
        Index(value = ["assetType"]),
        Index(value = ["date"])
    ]
)
data class AssetPriceHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val assetType: String,
    val date: Date, // Start of day
    val price: Double,
    val amount: Double,
    val totalValue: Double,
    val createdAt: Date
)
