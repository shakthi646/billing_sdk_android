package com.billingsdk.domain

import com.billingsdk.model.BillingUiEvent
import com.billingsdk.model.BillingUserMessage
import com.billingsdk.model.PremiumPlan
import com.billingsdk.model.PremiumStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * RevenueCat / backend migration seam: treat premium as a capability port.
 * The default implementation is Google Play Billing via [BillingRepository].
 */
interface PremiumEntitlementPort {
    fun observePremiumStatus(): Flow<PremiumStatus>
    suspend fun refreshEntitlements(): PremiumStatus
}

/**
 * Google Play–backed billing operations + dynamic catalog.
 */
interface BillingRepository : PremiumEntitlementPort {
    val plans: Flow<List<PremiumPlan>>
    val uiEvents: SharedFlow<BillingUiEvent>

    /**
     * Why the last [refreshPlans] produced nothing, or `null` when the catalog loaded.
     *
     * This is a [kotlinx.coroutines.flow.StateFlow]-backed value rather than a
     * [uiEvents] case on purpose: a paywall that subscribes after the refresh has
     * already finished still needs to learn the reason, and a replay-less
     * [SharedFlow] would have dropped it.
     */
    val planLoadError: Flow<BillingUserMessage?>

    suspend fun refreshPlans(): List<PremiumPlan>
    suspend fun restorePurchases(): PremiumStatus
    suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean
    suspend fun syncPremiumFromPlay(): PremiumStatus
    suspend fun launchPurchase(activity: android.app.Activity, planId: String)
}
