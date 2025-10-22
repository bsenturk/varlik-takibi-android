package com.xptlabs.varliktakibi.data.repository

import android.util.Log
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.entities.AssetEntity
import com.xptlabs.varliktakibi.data.local.entities.TransactionType
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.managers.AssetHistoryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val historyManager: AssetHistoryManager
) : AssetRepository {

    companion object {
        private const val TAG = "AssetRepositoryImpl"
    }

    override fun getAllAssets(): Flow<List<Asset>> {
        return assetDao.getAllAssets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAssetById(id: String): Asset? {
        return assetDao.getAssetById(id)?.toDomain()
    }

    override fun getAssetsByType(type: String): Flow<List<Asset>> {
        return assetDao.getAssetsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertAsset(asset: Asset) {
        Log.d(TAG, "Inserting asset: ${asset.name} (${asset.type.displayName})")
        assetDao.insertAsset(asset.toEntity())

        // Record initial transaction
        historyManager.recordTransaction(asset, TransactionType.INITIAL)
        // Record initial price snapshot
        historyManager.recordDailySnapshot(asset)
        Log.d(TAG, "Asset inserted with history tracking")
    }

    override suspend fun updateAsset(asset: Asset) {
        val existingAsset = assetDao.getAssetById(asset.id)?.toDomain()
        Log.d(TAG, "Updating asset: ${asset.name}")

        assetDao.updateAsset(asset.toEntity())

        // Record transaction based on amount change
        if (existingAsset != null) {
            val transactionType = when {
                asset.amount > existingAsset.amount -> TransactionType.ADD
                asset.amount < existingAsset.amount -> TransactionType.REMOVE
                else -> TransactionType.EDIT
            }
            historyManager.recordTransaction(asset, transactionType, existingAsset.amount)
        }

        // Update daily snapshot
        historyManager.recordDailySnapshot(asset)
        Log.d(TAG, "Asset updated with history tracking")
    }

    override suspend fun deleteAsset(asset: Asset) {
        Log.d(TAG, "Deleting asset: ${asset.name}")
        assetDao.deleteAssetById(asset.id)
        // Delete all history for this asset
        historyManager.deleteAssetHistory(asset.id)
        Log.d(TAG, "Asset and its history deleted")
    }

    override suspend fun deleteAssetById(id: String) {
        Log.d(TAG, "Deleting asset by ID: $id")
        assetDao.deleteAssetById(id)
        historyManager.deleteAssetHistory(id)
        Log.d(TAG, "Asset and its history deleted")
    }

    override suspend fun deleteAllAssets() {
        assetDao.deleteAllAssets()
    }

    override suspend fun getTotalPortfolioValue(): Double {
        return assetDao.getTotalPortfolioValue() ?: 0.0
    }

    override suspend fun getAssetCount(): Int {
        return assetDao.getAssetCount()
    }

    // Mapper functions
    private fun AssetEntity.toDomain(): Asset {
        return Asset(
            id = id,
            type = AssetType.valueOf(type),
            name = name,
            amount = amount,
            unit = unit,
            purchasePrice = purchasePrice,
            currentPrice = currentPrice,
            purchaseRate = purchaseRate,
            dateAdded = dateAdded,
            lastUpdated = lastUpdated
        )
    }

    private fun Asset.toEntity(): AssetEntity {
        return AssetEntity(
            id = id,
            type = type.name,
            name = name,
            amount = amount,
            unit = unit,
            purchasePrice = purchasePrice,
            currentPrice = currentPrice,
            purchaseRate = purchaseRate,
            dateAdded = dateAdded,
            lastUpdated = lastUpdated
        )
    }
}