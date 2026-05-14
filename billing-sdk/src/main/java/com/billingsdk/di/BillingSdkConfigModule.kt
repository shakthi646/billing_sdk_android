package com.billingsdk.di

import com.billingsdk.core.BillingSdkConfigHolder
import com.billingsdk.model.BillingSdkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingSdkConfigModule {
    @Provides
    @Singleton
    fun provideBillingSdkConfig(): BillingSdkConfig = BillingSdkConfigHolder.require()
}
