package com.xptlabs.varliktakibi.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xptlabs.varliktakibi.BuildConfig
import com.xptlabs.varliktakibi.data.local.dao.AssetDao
import com.xptlabs.varliktakibi.data.local.dao.RateDao
import com.xptlabs.varliktakibi.data.local.database.AssetTrackerDatabase
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
        )

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
                .allowMainThreadQueries()
        } else {
            builder.fallbackToDestructiveMigration()
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
}