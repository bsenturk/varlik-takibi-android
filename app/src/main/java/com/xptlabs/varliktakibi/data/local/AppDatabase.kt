package com.xptlabs.varliktakibi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        /** 3.2.0: portföy hedefi, işlemlerin varlığa bağlanması, varlığın yeri. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE portfolios ADD COLUMN targetValue REAL NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE assets ADD COLUMN location TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE transaction_history ADD COLUMN assetId TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transaction_history_assetId " +
                        "ON transaction_history (assetId)"
                )
                // Eski kayıtlar: sembolü tek bir varlıkta duranlar o varlığa atanır.
                // Birden fazla portföyde tutulan sembollerinki belirsiz; sembolle
                // eşleşmeye devam ederler (eski davranış).
                db.execSQL(
                    """
                    UPDATE transaction_history SET assetId =
                        (SELECT a.id FROM assets a WHERE a.symbol = transaction_history.symbol)
                    WHERE (SELECT COUNT(*) FROM assets a WHERE a.symbol = transaction_history.symbol) = 1
                    """
                )
            }
        }
    }
}
