package com.billingsdk.model

/**
 * A purchasable premium option discovered from Play (never hard-coded durations).
 *
 * @param planId Stable identifier for SDK purchase APIs (not the Play product id alone).
 * @param productId Play product id (subscription base product or one-time product).
 * @param basePlanId Play subscription base plan id when applicable.
 * @param offerToken Required token for launching subscription purchase flows.
 * @param formattedPrice Primary formatted price string from Play.
 * @param billingPeriod ISO 8601 duration from Play (e.g. P1Y) or a display string for lifetime.
 * @param freeTrial Human-readable trial description when present.
 * @param offerTag Optional tag from Play Console offer configuration.
 * @param isPopular Derived badge (e.g. yearly preference / offer tags).
 * @param planType Derived classification from Play billing periods (not hard-coded SKUs).
 * @param priceAmountMicros Used for comparisons (discount / per-month math).
 * @param perMonthFormatted Optional derived monthly equivalent for display.
 * @param discountPercent Optional derived savings label vs the most expensive monthly-equivalent plan.
 */
data class PremiumPlan(
    val planId: String,
    val productId: String,
    val basePlanId: String?,
    val offerToken: String?,
    val formattedPrice: String,
    val billingPeriod: String,
    val freeTrial: String?,
    val offerTag: String?,
    val isPopular: Boolean,
    val planType: PlanType,
    val priceAmountMicros: Long,
    val perMonthFormatted: String?,
    val discountPercent: Int?,
)
