package com.billingsdk.model

/** High-level UI/business events surfaced from billing flows. */
sealed interface BillingUiEvent {
    data class PurchaseSuccess(val planId: String?) : BillingUiEvent
    data class PurchaseCanceled(val planId: String?) : BillingUiEvent
    data class PurchaseError(val message: BillingUserMessage) : BillingUiEvent
    data class RestoreFinished(val premiumStatus: PremiumStatus) : BillingUiEvent
}
