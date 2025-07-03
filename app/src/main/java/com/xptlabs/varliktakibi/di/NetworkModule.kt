package com.xptlabs.varliktakibi.di

import com.xptlabs.varliktakibi.data.remote.AssetTrackerRemoteDataSource
import com.xptlabs.varliktakibi.data.remote.CurrencyWebService
import com.xptlabs.varliktakibi.data.remote.GoldWebService
import com.xptlabs.varliktakibi.data.remote.scraper.AssetTrackerWebService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @GoldRetrofit
    fun provideGoldRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://altin.doviz.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @CurrencyRetrofit
    fun provideCurrencyRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://kur.doviz.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @GoldWebService
    fun provideGoldWebService(@GoldRetrofit retrofit: Retrofit): AssetTrackerWebService {
        return retrofit.create(AssetTrackerWebService::class.java)
    }

    @Provides
    @Singleton
    @CurrencyWebService
    fun provideCurrencyWebService(@CurrencyRetrofit retrofit: Retrofit): AssetTrackerWebService {
        return retrofit.create(AssetTrackerWebService::class.java)
    }
}

// Qualifiers
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoldRetrofit

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrencyRetrofit