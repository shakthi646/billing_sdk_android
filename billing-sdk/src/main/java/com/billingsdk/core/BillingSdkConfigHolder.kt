package com.billingsdk.core

import com.billingsdk.model.BillingSdkConfig

internal object BillingSdkConfigHolder {
    @Volatile
    private var config: BillingSdkConfig? = null

    fun set(config: BillingSdkConfig) {
        this.config = config
    }

    fun getOrNull(): BillingSdkConfig? = config

    fun require(): BillingSdkConfig =
        config ?: error("BillingSDK.initialize(...) must be called in Application.onCreate() before super.onCreate().")
}
