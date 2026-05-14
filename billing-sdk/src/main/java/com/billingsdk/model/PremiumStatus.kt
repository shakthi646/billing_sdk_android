package com.billingsdk.model

/**
 * Client-side premium snapshot persisted to DataStore and updated after Play queries.
 *
 * Note: Exact subscription expiry is not reliably available on-device without a backend
 * (Google Play Developer API / RTDN). [expiryDateMillis] is best-effort when derivable.
 */
data class PremiumStatus(
    val isPremium: Boolean,
    val activePlan: String?,
    val expiryDateMillis: Long?,
    val purchaseToken: String?,
    val isLifetime: Boolean,
    val lastSyncedMillis: Long,
)
