package com.xptlabs.varliktakibi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val purchasePrice: Double,
    val currentPrice: Double,
    val dateAdded: Date,
    val lastUpdated: Date
)