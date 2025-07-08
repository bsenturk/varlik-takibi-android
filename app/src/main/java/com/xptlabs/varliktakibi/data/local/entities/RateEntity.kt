package com.xptlabs.varliktakibi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "rates")
data class RateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val change: Double,
    val changePercent: Double,
    val lastUpdated: Date,
    val isChangePercentPositive: Boolean
)