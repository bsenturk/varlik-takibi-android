package com.xptlabs.varliktakibi.data.repository

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
        assetDao.insertAsset(asset.toEntity())
    }

    override suspend fun updateAsset(asset: Asset) {
        assetDao.updateAsset(asset.toEntity())
    }

    override suspend fun deleteAsset(asset: Asset) {
        assetDao.deleteAssetById(asset.id)
    }

    override suspend fun deleteAssetById(id: String) {
        assetDao.deleteAssetById(id)
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
            dateAdded = dateAdded,
            lastUpdated = lastUpdated
        )
    }
}