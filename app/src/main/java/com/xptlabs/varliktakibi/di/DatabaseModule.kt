package com.xptlabs.varliktakibi.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.AssetPriceHistoryDao
import com.xptlabs.varliktakibi.data.local.dao.AssetTransactionHistoryDao
import com.xptlabs.varliktakibi.data.local.dao.RateDao
import com.xptlabs.varliktakibi.data.local.database.AssetTrackerDatabase
import com.xptlabs.varliktakibi.data.local.database.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAssetTrackerDatabase(
        @ApplicationContext context: Context
    ): AssetTrackerDatabase {
        val builder = Room.databaseBuilder(
            context,
            AssetTrackerDatabase::class.java,
            AssetTrackerDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_5_6)

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
                .allowMainThreadQueries()
        } else {
            builder.fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .enableMultiInstanceInvalidation()
        }

        return builder.build()
    }

    @Provides
    fun provideAssetDao(database: AssetTrackerDatabase): AssetDao {
        return database.assetDao()
    }

    @Provides
    fun provideRateDao(database: AssetTrackerDatabase): RateDao {
        return database.rateDao()
    }

    @Provides
    fun provideAssetPriceHistoryDao(database: AssetTrackerDatabase): AssetPriceHistoryDao {
        return database.assetPriceHistoryDao()
    }

    @Provides
    fun provideAssetTransactionHistoryDao(database: AssetTrackerDatabase): AssetTransactionHistoryDao {
        return database.assetTransactionHistoryDao()
    }
}