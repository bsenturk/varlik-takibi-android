package com.xptlabs.varliktakibi.data.local.dao

import androidx.room.*
import com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetTransactionHistoryDao {

    @Query("SELECT * FROM asset_transaction_history WHERE assetId = :assetId ORDER BY date DESC")
    fun getTransactionsByAssetId(assetId: String): Flow<List<AssetTransactionHistoryEntity>>

    @Query("SELECT * FROM asset_transaction_history WHERE assetType = :assetType ORDER BY date DESC")
    fun getTransactionsByAssetType(assetType: String): Flow<List<AssetTransactionHistoryEntity>>

    @Query("SELECT * FROM asset_transaction_history WHERE assetId = :assetId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentTransactionsByAssetId(assetId: String, limit: Int = 10): List<AssetTransactionHistoryEntity>

    @Query("SELECT COUNT(*) FROM asset_transaction_history WHERE assetId = :assetId")
    suspend fun getTransactionCountByAssetId(assetId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AssetTransactionHistoryEntity)

    @Update
    suspend fun updateTransaction(transaction: AssetTransactionHistoryEntity)

    @Delete
    suspend fun deleteTransaction(transaction: AssetTransactionHistoryEntity)

    @Query("DELETE FROM asset_transaction_history WHERE assetId = :assetId")
    suspend fun deleteTransactionsByAssetId(assetId: String)

    @Query("DELETE FROM asset_transaction_history WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<String>)

    @Query("SELECT * FROM asset_transaction_history WHERE assetId = :assetId ORDER BY date ASC LIMIT 1")
    suspend fun getInitialTransactionByAssetId(assetId: String): AssetTransactionHistoryEntity?
}
