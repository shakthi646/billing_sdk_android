package com.billingsdk.model

/**
 * A purchasable premium option discovered from Play (never hard-coded durations).
 *
 * @param planId Stable identifier for SDK purchase APIs (not the Play product id alone).
 * @param productId Play product id (subscription base product or one-time product).
 * @param basePlanId Play subscription base plan id when applicable.
 * @param offerToken Required token for launching subscription purchase flows.
 * @param formattedPrice Recurring formatted price from Play — the steady-state price once
 *   any introductory phase has run out. Used for per-month and discount comparisons.
 * @param billingPeriod ISO 8601 duration from Play (e.g. P1Y) or a display string for lifetime.
 * @param freeTrial Human-readable trial description when present.
 * @param offerTag Optional tag from Play Console offer configuration.
 * @param isPopular Derived badge (e.g. yearly preference / offer tags).
 * @param planType Derived classification from Play billing periods (not hard-coded SKUs).
 * @param priceAmountMicros Used for comparisons (discount / per-month math).
 * @param perMonthFormatted Optional derived monthly equivalent for display.
 * @param discountPercent Optional derived savings label vs the most expensive monthly-equivalent plan.
 * @param checkoutFormattedPrice What Google Play actually charges on the next payment —
 *   the first *paid* pricing phase of the offer. For a plan with no introductory
 *   pricing this equals [formattedPrice]; for an offer plan it is the discounted
 *   amount, which is what the Play purchase sheet shows after the user taps the CTA.
 * @param checkoutPriceAmountMicros Micros counterpart of [checkoutFormattedPrice].
 * @param introPhaseLabel Human-readable duration of the introductory phase (e.g.
 *   "for the first 3 months"), or `null` when the offer has no introductory pricing.
 * @param offerId Play offer id. `null` marks the base plan's own plain offer — the
 *   "real" price. A non-null value is a promotional offer layered on that base plan;
 *   both are listed so the user can compare the deal against the standing price.
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
    val checkoutFormattedPrice: String = formattedPrice,
    val checkoutPriceAmountMicros: Long = priceAmountMicros,
    val introPhaseLabel: String? = null,
    val offerId: String? = null,
) {
    /** True when Play will charge less on the next payment than the recurring price. */
    val hasIntroOffer: Boolean
        get() = checkoutPriceAmountMicros != priceAmountMicros

    /** A promotional offer on a base plan, as opposed to the base plan's own price. */
    val isPromotionalOffer: Boolean
        get() = !offerId.isNullOrBlank()
}
