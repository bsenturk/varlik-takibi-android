package com.xptlabs.varliktakibi.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xptlabs.varliktakibi.data.local.converters.Converters
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.RateDao
import com.xptlabs.varliktakibi.data.local.entities.AssetEntity
import com.xptlabs.varliktakibi.data.local.entities.RateEntity

@Database(
    entities = [AssetEntity::class, RateEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AssetTrackerDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao
    abstract fun rateDao(): RateDao

    companion object {
        const val DATABASE_NAME = "asset_tracker_db"
    }
}