package com.billingsdk.utils

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.billingsdk.model.BillingUserMessage

object BillingMessageResolver {
    fun forResponseCode(@BillingResponseCode code: Int, debugMessage: String? = null): BillingUserMessage {
        return when (code) {
            BillingResponseCode.OK -> BillingUserMessage("Success", null)
            BillingResponseCode.USER_CANCELED -> BillingUserMessage(
                title = "Purchase canceled",
                detail = "You canceled the purchase flow.",
            )
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.NETWORK_ERROR,
            -> BillingUserMessage(
                title = "Network issue",
                detail = "Please check your connection and try again.",
            )
            BillingResponseCode.BILLING_UNAVAILABLE -> BillingUserMessage(
                title = "Billing unavailable",
                detail = "Google Play billing is not available on this device right now.",
            )
            BillingResponseCode.ITEM_UNAVAILABLE -> BillingUserMessage(
                title = "Offer unavailable",
                detail = "This offer is not available anymore. Pull to refresh or try again later.",
            )
            BillingResponseCode.ITEM_ALREADY_OWNED -> BillingUserMessage(
                title = "Already owned",
                detail = "This purchase is already active on your account.",
            )
            BillingResponseCode.DEVELOPER_ERROR -> BillingUserMessage(
                title = "Configuration error",
                detail = debugMessage ?: "Developer error. Verify Play Console product setup and signing.",
            )
            BillingResponseCode.SERVICE_DISCONNECTED -> BillingUserMessage(
                title = "Disconnected",
                detail = "Lost connection to Google Play. Retrying…",
            )
            else -> BillingUserMessage(
                title = "Something went wrong",
                detail = debugMessage ?: "Error code: $code",
            )
        }
    }

    fun forThrowable(t: Throwable): BillingUserMessage =
        BillingUserMessage(
            title = "Unexpected error",
            detail = t.message,
        )
}
