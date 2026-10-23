package com.billingsdk.data.mapper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.billingsdk.model.PlanType
import com.billingsdk.model.PremiumPlan
import com.billingsdk.utils.BillingPeriodUtils
import com.billingsdk.utils.PlanIds
import com.billingsdk.utils.PriceMath
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps Play [ProductDetails] into host-facing [PremiumPlan] rows without hard-coded durations.
 */
@Singleton
class ProductDetailsMapper @Inject constructor() {

    fun mapAll(
        subscriptionProductId: String,
        lifetimeProductId: String,
        detailsList: List<ProductDetails>,
    ): List<PremiumPlan> {
        val detailsById = detailsList.associateBy { it.productId }
        val sub = detailsById[subscriptionProductId]
        val lifetimeDetails = detailsById[lifetimeProductId]

        val subs = sub?.let { mapSubscriptionProduct(it) }.orEmpty()
        val lifetime = lifetimeDetails?.let { mapLifetimeProduct(it) }.let { listOfNotNull(it) }

        val combined = subs + lifetime
        return applyDerivedBadges(combined)
    }

    /**
     * One row per offer, so a base plan carrying a promotion yields two rows: the
     * promotional price and the standing base price next to it. The user can then
     * see what the deal is worth instead of only being shown the discounted figure.
     *
     * Rows are ordered so each promotional offer sits directly above the base plan
     * it discounts.
     */
    private fun mapSubscriptionProduct(details: ProductDetails): List<PremiumPlan> {
        return details.subscriptionOfferDetails.orEmpty()
            .mapNotNull { mapOffer(details, it) }
            .groupBy { it.basePlanId }
            .values
            .flatMap { rows ->
                rows.sortedWith(
                    // Promotions first, cheapest of them leading.
                    compareBy({ !it.isPromotionalOffer }, { it.checkoutPriceAmountMicros }),
                )
            }
    }

    private fun mapOffer(
        details: ProductDetails,
        offer: ProductDetails.SubscriptionOfferDetails,
    ): PremiumPlan? {
        val phases = offer.pricingPhases.pricingPhaseList
        if (phases.isEmpty()) return null

        val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
        val paidPhases = phases.filter { it.priceAmountMicros > 0L }

        // Last paid phase = the steady-state price the plan settles at.
        val recurring = paidPhases.lastOrNull() ?: return null
        // First paid phase = what Play charges on the next payment. These differ
        // whenever the offer carries introductory pricing, and showing the
        // recurring one here is what made the card disagree with the Play sheet.
        val checkout = paidPhases.first()

        val iso = recurring.billingPeriod
        val planType = BillingPeriodUtils.planTypeFromIsoDuration(iso)
        val currency = recurring.priceCurrencyCode

        val planId = PlanIds.encode(details.productId, offer.basePlanId, offer.offerToken)

        return PremiumPlan(
            planId = planId,
            productId = details.productId,
            basePlanId = offer.basePlanId,
            offerToken = offer.offerToken,
            formattedPrice = recurring.formattedPrice,
            billingPeriod = iso,
            freeTrial = trialPhase?.let { formatTrialLabel(it) },
            offerTag = offer.offerTags.firstOrNull(),
            isPopular = false,
            planType = planType,
            priceAmountMicros = recurring.priceAmountMicros,
            perMonthFormatted = PriceMath.microsToPerMonthMicros(planType, recurring.priceAmountMicros)
                ?.let { formatMicros(it, currency) },
            discountPercent = null,
            checkoutFormattedPrice = checkout.formattedPrice,
            checkoutPriceAmountMicros = checkout.priceAmountMicros,
            introPhaseLabel = checkout
                .takeIf { it.priceAmountMicros != recurring.priceAmountMicros }
                ?.let { formatIntroLabel(it) },
            offerId = offer.offerId,
        )
    }

    private fun mapLifetimeProduct(details: ProductDetails): PremiumPlan? {
        if (details.productType != BillingClient.ProductType.INAPP) return null
        val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return null

        val planId = PlanIds.encode(details.productId, null, offer.offerToken)
        return PremiumPlan(
            planId = planId,
            productId = details.productId,
            basePlanId = null,
            offerToken = offer.offerToken,
            formattedPrice = offer.formattedPrice,
            billingPeriod = "ONE_TIME",
            freeTrial = null,
            offerTag = null,
            isPopular = false,
            planType = PlanType.LIFETIME,
            priceAmountMicros = offer.priceAmountMicros,
            perMonthFormatted = null,
            discountPercent = null,
            checkoutFormattedPrice = offer.formattedPrice,
            checkoutPriceAmountMicros = offer.priceAmountMicros,
            introPhaseLabel = null,
            offerId = null,
        )
    }

    /** e.g. "7 days free" — the zero-cost phase at the head of an offer. */
    private fun formatTrialLabel(phase: PricingPhase): String {
        val period = BillingPeriodUtils.describeIsoDuration(phase.billingPeriod, phase.billingCycleCount)
        return period?.let { "$it free" } ?: "Free trial"
    }

    /** e.g. "for the first 3 months" — the window the introductory price applies to. */
    private fun formatIntroLabel(phase: PricingPhase): String {
        val period = BillingPeriodUtils.describeIsoDuration(phase.billingPeriod, phase.billingCycleCount)
        return period?.let { "for the first $it" } ?: "introductory price"
    }

    private fun formatMicros(micros: Long, currencyCode: String): String {
        return runCatching {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode)
            format.format(micros / 1_000_000.0)
        }.getOrElse { "" }
    }

    private fun applyDerivedBadges(plans: List<PremiumPlan>): List<PremiumPlan> {
        if (plans.isEmpty()) return plans

        val subs = plans.filter { it.planType != PlanType.LIFETIME }
        val baselinePerMonthMicros = subs.mapNotNull { plan ->
            PriceMath.microsToPerMonthMicros(plan.planType, plan.priceAmountMicros)
        }.maxOrNull()

        return plans.map { plan ->
            val discount = if (plan.hasIntroOffer) {
                // For a promotional row the meaningful saving is the introductory
                // price against what the same plan renews at — not against some
                // other plan's monthly equivalent.
                PriceMath.discountPercentVsBaseline(
                    plan.checkoutPriceAmountMicros,
                    plan.priceAmountMicros,
                )
            } else {
                baselinePerMonthMicros?.let { base ->
                    val mine = PriceMath.microsToPerMonthMicros(plan.planType, plan.priceAmountMicros)
                        ?: return@let null
                    PriceMath.discountPercentVsBaseline(mine, base)
                }
            }
            val popularTags = plan.offerTag?.contains("popular", ignoreCase = true) == true
            val popular = plan.planType == PlanType.YEARLY || popularTags
            plan.copy(
                discountPercent = discount,
                isPopular = popular,
            )
        }
    }
}
