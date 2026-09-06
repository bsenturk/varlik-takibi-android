package com.xptlabs.varliktakibi.di

import android.content.Context
import androidx.room.Room
import com.xptlabs.varliktakibi.data.local.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // v1.0'ın şeması yeni modelle uyumsuz (portföy yok, sembol yok). Temiz
        // başlangıç kararı gereği eski dosya bir kez siliniyor; deleteDatabase
        // -shm/-wal yan dosyalarını da temizler ve dosya yoksa no-op.
        context.deleteDatabase(AppDatabase.LEGACY_NAME)

        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()
    }

    @Provides fun providePortfolioDao(db: AppDatabase) = db.portfolioDao()
    @Provides fun provideAssetDao(db: AppDatabase) = db.assetDao()
    @Provides fun provideHistoryDao(db: AppDatabase) = db.historyDao()
    @Provides fun provideSnapshotDao(db: AppDatabase) = db.snapshotDao()
}
