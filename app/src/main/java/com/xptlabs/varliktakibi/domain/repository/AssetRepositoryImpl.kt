package com.xptlabs.varliktakibi.data.repository

import android.util.Log
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.entities.AssetEntity
import com.xptlabs.varliktakibi.domain.models.Asset
import com.xptlabs.varliktakibi.domain.models.AssetType
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao
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
        Log.d(TAG, "Asset inserted successfully")
    }

    override suspend fun updateAsset(asset: Asset) {
        Log.d(TAG, "Updating asset: ${asset.name}")
        assetDao.updateAsset(asset.toEntity())
        Log.d(TAG, "Asset updated successfully")
    }

    override suspend fun deleteAsset(asset: Asset) {
        Log.d(TAG, "Deleting asset: ${asset.name}")
        assetDao.deleteAssetById(asset.id)
        Log.d(TAG, "Asset deleted successfully")
    }

    override suspend fun deleteAssetById(id: String) {
        Log.d(TAG, "Deleting asset by ID: $id")
        assetDao.deleteAssetById(id)
        Log.d(TAG, "Asset deleted successfully")
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