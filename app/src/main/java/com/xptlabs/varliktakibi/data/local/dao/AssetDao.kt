package com.xptlabs.varliktakibi.data.local.dao

import androidx.room.*
import com.xptlabs.varliktakibi.data.local.entities.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY dateAdded DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE type = :type")
    fun getAssetsByType(type: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()

    @Query("SELECT SUM(amount * currentPrice) FROM assets")
    suspend fun getTotalPortfolioValue(): Double?

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetCount(): Int
}