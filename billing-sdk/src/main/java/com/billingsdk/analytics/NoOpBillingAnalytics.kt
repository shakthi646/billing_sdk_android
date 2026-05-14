package com.billingsdk.analytics

/** Default no-op implementation. */
object NoOpBillingAnalytics : BillingAnalytics {
    override fun onPaywallShown() {}
    override fun onPurchaseStarted(planId: String) {}
    override fun onPurchaseSuccess(planId: String) {}
    override fun onPurchaseFailed(planId: String?, reason: String) {}
    override fun onRestoreSuccess() {}
}
