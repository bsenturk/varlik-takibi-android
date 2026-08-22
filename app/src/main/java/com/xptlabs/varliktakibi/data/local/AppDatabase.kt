package com.xptlabs.varliktakibi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.HistoryDao
import com.xptlabs.varliktakibi.data.local.dao.PortfolioDao
import com.xptlabs.varliktakibi.data.local.dao.SnapshotDao
import com.xptlabs.varliktakibi.data.local.entity.AssetEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.PortfolioSnapshotEntity
import com.xptlabs.varliktakibi.data.local.entity.PriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionHistoryEntity
import com.xptlabs.varliktakibi.data.local.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EDIT)
}

@Database(
    entities = [
        PortfolioEntity::class,
        AssetEntity::class,
        PriceHistoryEntity::class,
        TransactionHistoryEntity::class,
        PortfolioSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun portfolioDao(): PortfolioDao
    abstract fun assetDao(): AssetDao
    abstract fun historyDao(): HistoryDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        /**
         * Eski (v6) şemayla uyumsuz olduğu için yeni dosya adı kullanılıyor.
         * Eski "asset_tracker_db" ilk açılışta [DatabaseModule] tarafından siliniyor.
         */
        const val NAME = "varlik_takibi.db"
        const val LEGACY_NAME = "asset_tracker_db"
    }
}
