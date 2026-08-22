package com.xptlabs.varliktakibi.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xptlabs.varliktakibi.data.local.converters.Converters
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.AssetPriceHistoryDao
import com.xptlabs.varliktakibi.data.local.dao.AssetTransactionHistoryDao
import com.xptlabs.varliktakibi.data.local.dao.RateDao
import com.xptlabs.varliktakibi.data.local.entities.AssetEntity
import com.xptlabs.varliktakibi.data.local.entities.AssetPriceHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.AssetTransactionHistoryEntity
import com.xptlabs.varliktakibi.data.local.entities.RateEntity

@Database(
    entities = [
        AssetEntity::class,
        RateEntity::class,
        AssetPriceHistoryEntity::class,
        AssetTransactionHistoryEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AssetTrackerDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao
    abstract fun rateDao(): RateDao
    abstract fun assetPriceHistoryDao(): AssetPriceHistoryDao
    abstract fun assetTransactionHistoryDao(): AssetTransactionHistoryDao

    companion object {
        const val DATABASE_NAME = "asset_tracker_db"
    }
}