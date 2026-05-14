package com.billingsdk.analytics

/**
 * Analytics hooks for host apps. Provide your own implementation via [com.billingsdk.model.BillingSdkConfig].
 *
 * Designed so you can forward events to Firebase, Amplitude, Mixpanel, or a future
 * [com.billingsdk.domain.PremiumEntitlementPort] implementation (for example RevenueCat).
 */
interface BillingAnalytics {
    fun onPaywallShown()
    fun onPurchaseStarted(planId: String)
    fun onPurchaseSuccess(planId: String)
    fun onPurchaseFailed(planId: String?, reason: String)
    fun onRestoreSuccess()
}
