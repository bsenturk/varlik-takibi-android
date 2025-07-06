package com.xptlabs.varliktakibi.di

import com.xptlabs.varliktakibi.data.repository.AssetRepositoryImpl
import com.xptlabs.varliktakibi.data.repository.OnboardingRepositoryImpl
import com.xptlabs.varliktakibi.data.repository.RateRepositoryImpl
import com.xptlabs.varliktakibi.domain.repository.AssetRepository
import com.xptlabs.varliktakibi.domain.repository.OnboardingRepository
import com.xptlabs.varliktakibi.domain.repository.RateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        onboardingRepositoryImpl: OnboardingRepositoryImpl
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(
        assetRepositoryImpl: AssetRepositoryImpl
    ): AssetRepository

    @Binds
    @Singleton
    abstract fun bindRateRepository(
        rateRepositoryImpl: RateRepositoryImpl
    ): RateRepository
}