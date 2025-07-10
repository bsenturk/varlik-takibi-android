package com.xptlabs.varliktakibi.di

import android.content.Context
import com.xptlabs.varliktakibi.data.analytics.FirebaseAnalyticsManager
import com.xptlabs.varliktakibi.notifications.AppNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        analyticsManager: FirebaseAnalyticsManager
    ): AppNotificationManager {
        return AppNotificationManager(context, analyticsManager)
    }
}