package com.billingsdk.di

import com.billingsdk.data.BillingRepositoryImpl
import com.billingsdk.domain.BillingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingRepositoryBinding {
    @Provides
    @Singleton
    fun provideBillingRepository(impl: BillingRepositoryImpl): BillingRepository = impl
}
