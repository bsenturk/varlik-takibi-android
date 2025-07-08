package com.xptlabs.varliktakibi.domain.repository

import com.xptlabs.varliktakibi.domain.models.Asset
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun getAllAssets(): Flow<List<Asset>>
    suspend fun getAssetById(id: String): Asset?
    fun getAssetsByType(type: String): Flow<List<Asset>>
    suspend fun insertAsset(asset: Asset)
    suspend fun updateAsset(asset: Asset)
    suspend fun deleteAsset(asset: Asset)
    suspend fun deleteAssetById(id: String)
    suspend fun deleteAllAssets()
    suspend fun getTotalPortfolioValue(): Double
    suspend fun getAssetCount(): Int
}