package com.xptlabs.varliktakibi.managers

import com.xptlabs.varliktakibi.data.local.dao.AssetPriceHistoryDao
import com.xptlabs.varliktakibi.data.local.dao.AssetTransactionHistoryDao
import com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.TransactionType
import com.xptlabs.varliktakibi.domain.models.Asset
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for tracking asset price and transaction history
 * Similar to iOS AssetHistoryManager
 */
@Singleton
class AssetHistoryManager @Inject constructor(
    private val priceHistoryDao: AssetPriceHistoryDao,
    private val transactionHistoryDao: AssetTransactionHistoryDao
) {

    companion object {
        private const val MAX_PRICE_HISTORY = 30
        private const val MAX_TRANSACTION_HISTORY = 10
    }

    /**
     * Record daily snapshot of asset price
     * If today's snapshot exists, update it. Otherwise create new one.
     */
    suspend fun recordDailySnapshot(asset: Asset) {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val today = calendar.time

        // Check if today's snapshot exists
        val existingSnapshot = priceHistoryDao.getHistoryByAssetIdAndDate(asset.id, today)

        if (existingSnapshot != null) {
            // Same day - update existing snapshot
            val updated = existingSnapshot.copy(
                price = asset.currentPrice,
                amount = asset.amount,
                totalValue = asset.totalValue,
                createdAt = Date()
            )
            priceHistoryDao.updateHistory(updated)
        } else {
            // New day - create new snapshot
            val newSnapshot = AssetPriceHistoryEntity(
                assetId = asset.id,
                assetType = asset.type.name,
                date = today,
                price = asset.currentPrice,
                amount = asset.amount,
                totalValue = asset.totalValue,
                createdAt = Date()
            )
            priceHistoryDao.insertHistory(newSnapshot)

            // Enforce history limit
            enforceHistoryLimit(asset.id)
        }
    }

    /**
     * Record transaction history when asset is added, updated, or deleted
     */
    suspend fun recordTransaction(
        asset: Asset,
        transactionType: TransactionType,
        previousAmount: Double = 0.0
    ) {
        val transaction = AssetTransactionHistoryEntity(
            assetId = asset.id,
            assetType = asset.type.name,
            date = Date(),
            transactionType = transactionType,
            amount = when (transactionType) {
                TransactionType.ADD -> asset.amount - previousAmount
                TransactionType.REMOVE -> previousAmount - asset.amount
                else -> asset.amount
            },
            totalAmount = asset.amount,
            price = asset.currentPrice,
            totalValue = asset.totalValue,
            createdAt = Date()
        )

        transactionHistoryDao.insertTransaction(transaction)

        // Enforce transaction history limit
        enforceTransactionLimit(asset.id)
    }

    /**
     * Enforce max 30 price history records per asset
     * Keep initial + 29 most recent
     */
    private suspend fun enforceHistoryLimit(assetId: String) {
        val count = priceHistoryDao.getHistoryCountByAssetId(assetId)

        if (count > MAX_PRICE_HISTORY) {
            // Get oldest record (initial)
            val oldest = priceHistoryDao.getOldestHistoryByAssetId(assetId)

            // Get all records except oldest
            val allRecords = priceHistoryDao.getHistoryByAssetIdSinceDate(
                assetId,
                oldest?.date ?: Date(0)
            ).filter { it.id != oldest?.id }

            // If we have more than 29 records (excluding initial), delete the excess
            if (allRecords.size > MAX_PRICE_HISTORY - 1) {
                val toDelete = allRecords.sortedBy { it.date }
                    .take(allRecords.size - (MAX_PRICE_HISTORY - 1))
                priceHistoryDao.deleteHistoriesByIds(toDelete.map { it.id })
            }
        }
    }

    /**
     * Enforce max 10 transaction history records per asset
     * Keep initial + 9 most recent
     */
    private suspend fun enforceTransactionLimit(assetId: String) {
        val count = transactionHistoryDao.getTransactionCountByAssetId(assetId)

        if (count > MAX_TRANSACTION_HISTORY) {
            // Get initial transaction
            val initial = transactionHistoryDao.getInitialTransactionByAssetId(assetId)

            // Get all transactions except initial
            val allTransactions = transactionHistoryDao
                .getRecentTransactionsByAssetId(assetId, Int.MAX_VALUE)
                .filter { it.id != initial?.id }

            // If we have more than 9 transactions (excluding initial), delete the excess
            if (allTransactions.size > MAX_TRANSACTION_HISTORY - 1) {
                val toDelete = allTransactions.sortedBy { it.date }
                    .take(allTransactions.size - (MAX_TRANSACTION_HISTORY - 1))
                transactionHistoryDao.deleteTransactionsByIds(toDelete.map { it.id })
            }
        }
    }

    /**
     * Delete all history for an asset
     */
    suspend fun deleteAssetHistory(assetId: String) {
        priceHistoryDao.deleteHistoryByAssetId(assetId)
        transactionHistoryDao.deleteTransactionsByAssetId(assetId)
    }

    /**
     * Get price history for charts
     * Returns history for the last N days
     */
    suspend fun getPriceHistory(assetId: String, days: Int): List<AssetPriceHistoryEntity> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time

        return priceHistoryDao.getHistoryByAssetIdSinceDate(assetId, startDate)
            .sortedBy { it.date }
    }

    /**
     * Get recent transactions (sorted ascending - initial transaction first)
     */
    suspend fun getRecentTransactions(assetId: String, limit: Int = 10): List<AssetTransactionHistoryEntity> {
        return transactionHistoryDao.getRecentTransactionsByAssetId(assetId, limit).reversed()
    }
}
