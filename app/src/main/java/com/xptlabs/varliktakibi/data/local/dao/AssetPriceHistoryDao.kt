package com.xptlabs.varliktakibi.data.local.dao

import androidx.room.*
import com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AssetPriceHistoryDao {

    @Query("SELECT * FROM asset_price_history WHERE assetId = :assetId ORDER BY date DESC")
    fun getHistoryByAssetId(assetId: String): Flow<List<AssetPriceHistoryEntity>>

    @Query("SELECT * FROM asset_price_history WHERE assetType = :assetType ORDER BY date DESC")
    fun getHistoryByAssetType(assetType: String): Flow<List<AssetPriceHistoryEntity>>

    @Query("SELECT * FROM asset_price_history WHERE assetId = :assetId AND date = :date LIMIT 1")
    suspend fun getHistoryByAssetIdAndDate(assetId: String, date: Date): AssetPriceHistoryEntity?

    @Query("SELECT * FROM asset_price_history WHERE assetId = :assetId AND date >= :startDate ORDER BY date DESC")
    suspend fun getHistoryByAssetIdSinceDate(assetId: String, startDate: Date): List<AssetPriceHistoryEntity>

    @Query("SELECT COUNT(*) FROM asset_price_history WHERE assetId = :assetId")
    suspend fun getHistoryCountByAssetId(assetId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AssetPriceHistoryEntity)

    @Update
    suspend fun updateHistory(history: AssetPriceHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: AssetPriceHistoryEntity)

    @Query("DELETE FROM asset_price_history WHERE assetId = :assetId")
    suspend fun deleteHistoryByAssetId(assetId: String)

    @Query("DELETE FROM asset_price_history WHERE id IN (:ids)")
    suspend fun deleteHistoriesByIds(ids: List<String>)

    @Query("SELECT * FROM asset_price_history WHERE assetId = :assetId ORDER BY date ASC LIMIT 1")
    suspend fun getOldestHistoryByAssetId(assetId: String): AssetPriceHistoryEntity?
}
