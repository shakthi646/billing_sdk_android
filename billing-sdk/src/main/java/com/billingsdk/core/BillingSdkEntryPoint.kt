package com.billingsdk.core

import com.billingsdk.domain.BillingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BillingSdkEntryPoint {
    fun billingRepository(): BillingRepository
}
