package com.xptlabs.varliktakibi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity
import com.xptlabs.varliktakibi.data.local.entity.PriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolios ORDER BY sortOrder")
    fun observeAll(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolios ORDER BY sortOrder")
    suspend fun getAll(): List<PortfolioEntity>

    @Query("SELECT * FROM portfolios WHERE id = :id")
    suspend fun getById(id: String): PortfolioEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM portfolios")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(portfolio: PortfolioEntity)

    @Update
    suspend fun update(portfolio: PortfolioEntity)

    @Delete
    suspend fun delete(portfolio: PortfolioEntity)
}

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY dateAdded")
    fun observeAll(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY dateAdded")
    suspend fun getAll(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE portfolioId = :portfolioId ORDER BY dateAdded")
    suspend fun getForPortfolio(portfolioId: String): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE portfolioId = :portfolioId AND symbol = :symbol LIMIT 1")
    suspend fun findInPortfolio(portfolioId: String, symbol: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: String): AssetEntity?

    /** Aynı sembolü kaç varlık kullanıyor — geçmişi silmeden önce kontrol için. */
    @Query("SELECT COUNT(*) FROM assets WHERE symbol = :symbol")
    suspend fun countBySymbol(symbol: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity)

    @Update
    suspend fun update(asset: AssetEntity)

    @Update
    suspend fun updateAll(assets: List<AssetEntity>)

    @Delete
    suspend fun delete(asset: AssetEntity)
}

@Dao
interface HistoryDao {

    // ── Fiyat geçmişi ────────────────────────────────────────────────────────

    @Query("SELECT * FROM price_history WHERE symbol = :symbol ORDER BY day")
    suspend fun priceHistory(symbol: String): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history WHERE symbol IN (:symbols) ORDER BY day")
    fun observePriceHistory(symbols: List<String>): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE symbol = :symbol AND day = :day LIMIT 1")
    suspend fun priceOn(symbol: String, day: Long): PriceHistoryEntity?

    @Upsert
    suspend fun upsertPrice(entry: PriceHistoryEntity)

    /**
     * Sembolün en eski kayıtlarını buda; ilk kaydı (maliyet çıpası) her zaman
     * korur. `LIMIT -1 OFFSET` kalıbı SQLite'ta "ilkini atla"nın tek yolu.
     */
    @Query(
        """
        DELETE FROM price_history WHERE id IN (
            SELECT id FROM price_history WHERE symbol = :symbol ORDER BY day DESC
            LIMIT -1 OFFSET :keep
        ) AND day > (SELECT MIN(day) FROM price_history WHERE symbol = :symbol)
        """
    )
    suspend fun trimPriceHistory(symbol: String, keep: Int)

    @Query("DELETE FROM price_history WHERE symbol = :symbol")
    suspend fun deletePriceHistory(symbol: String)

    // ── İşlem geçmişi ────────────────────────────────────────────────────────

    @Query("SELECT * FROM transaction_history WHERE symbol = :symbol ORDER BY date")
    suspend fun transactions(symbol: String): List<TransactionHistoryEntity>

    @Insert
    suspend fun insertTransaction(entry: TransactionHistoryEntity)

    @Query(
        """
        DELETE FROM transaction_history WHERE id IN (
            SELECT id FROM transaction_history WHERE symbol = :symbol ORDER BY date DESC
            LIMIT -1 OFFSET :keep
        )
        """
    )
    suspend fun trimTransactions(symbol: String, keep: Int)

    @Query("DELETE FROM transaction_history WHERE symbol = :symbol")
    suspend fun deleteTransactions(symbol: String)

    @Transaction
    suspend fun deleteAllForSymbol(symbol: String) {
        deletePriceHistory(symbol)
        deleteTransactions(symbol)
    }
}

@Dao
interface SnapshotDao {

    @Query("SELECT * FROM portfolio_snapshots ORDER BY day")
    fun observeAll(): Flow<List<PortfolioSnapshotEntity>>

    @Query("SELECT * FROM portfolio_snapshots WHERE portfolioId = :portfolioId ORDER BY day")
    suspend fun forPortfolio(portfolioId: String): List<PortfolioSnapshotEntity>

    @Query("SELECT MAX(day) FROM portfolio_snapshots WHERE portfolioId = :portfolioId")
    suspend fun latestDay(portfolioId: String): Long?

    @Upsert
    suspend fun upsert(snapshot: PortfolioSnapshotEntity)

    @Upsert
    suspend fun upsertAll(snapshots: List<PortfolioSnapshotEntity>)
}
