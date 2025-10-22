package com.xptlabs.varliktakibi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "asset_transaction_history",
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["assetType"]),
        Index(value = ["date"])
    ]
)
data class AssetTransactionHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val assetType: String,
    val date: Date,
    val transactionType: TransactionType,
    val amount: Double,
    val totalAmount: Double,
    val price: Double,
    val totalValue: Double,
    val createdAt: Date
)

enum class TransactionType {
    INITIAL,  // Initial addition
    ADD,      // Amount increase
    REMOVE,   // Amount decrease
    EDIT      // Edit transaction
}
