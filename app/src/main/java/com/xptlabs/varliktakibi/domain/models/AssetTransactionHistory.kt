package com.xptlabs.varliktakibi.domain.models

import com.xptlabs.varliktakibi.data.local.entities.TransactionType
import java.util.Date

data class AssetTransactionHistory(
    val id: String,
    val assetId: String,
    val assetType: AssetType,
    val date: Date,
    val transactionType: TransactionType,
    val amount: Double,
    val totalAmount: Double,
    val price: Double,
    val totalValue: Double,
    val createdAt: Date
)
