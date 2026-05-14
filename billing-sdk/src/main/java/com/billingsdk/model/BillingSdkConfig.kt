package com.billingsdk.model

import com.billingsdk.analytics.BillingAnalytics
import com.billingsdk.analytics.NoOpBillingAnalytics

/**
 * Host-configurable SDK settings. Product identifiers match Play Console configuration.
 * Offer discovery is always dynamic; only the *product ids* are configured here.
 */
data class BillingSdkConfig(
    val subscriptionProductId: String,
    val lifetimeProductId: String,
    /** Reserved for host verification flows (local/server). Not required for BillingClient itself. */
    val publicKeyBase64: String,
    val enableLogging: Boolean,
    val paywallCustomization: PaywallCustomization,
    val analytics: BillingAnalytics,
) {
    companion object {
        fun default(
            publicKeyBase64: String,
            enableLogging: Boolean,
            paywallCustomization: PaywallCustomization = PaywallCustomization.Default,
            analytics: BillingAnalytics = NoOpBillingAnalytics,
        ): BillingSdkConfig = BillingSdkConfig(
            subscriptionProductId = "premium_plan",
            lifetimeProductId = "lifetime_premium",
            publicKeyBase64 = publicKeyBase64,
            enableLogging = enableLogging,
            paywallCustomization = paywallCustomization,
            analytics = analytics,
        )
    }
}
