package com.xptlabs.varliktakibi.di

import android.app.Application
import android.content.Context
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdMobModule {

    @Provides
    @Singleton
    fun provideAdMobManager(
        @ApplicationContext context: Context,
        analyticsManager: FirebaseAnalyticsManager
    ): AdMobManager {
        return AdMobManager(context, analyticsManager)
    }
}