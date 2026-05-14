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

    private fun mapSubscriptionProduct(details: ProductDetails): List<PremiumPlan> {
        val offers = details.subscriptionOfferDetails.orEmpty()
        return offers.mapNotNull { offer ->
            val phases = offer.pricingPhases.pricingPhaseList
            if (phases.isEmpty()) return@mapNotNull null

            val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
            val paidPhases = phases.filter { it.priceAmountMicros > 0L }
            val recurring = paidPhases.lastOrNull() ?: return@mapNotNull null

            val iso = recurring.billingPeriod
            val planType = BillingPeriodUtils.planTypeFromIsoDuration(iso)
            val formattedPrice = recurring.formattedPrice
            val priceMicros = recurring.priceAmountMicros
            val currency = recurring.priceCurrencyCode

            val freeTrial = trialPhase?.let { formatTrialLabel(it) }
            val offerTag = offer.offerTags.firstOrNull()

            val planId = PlanIds.encode(details.productId, offer.basePlanId, offer.offerToken)

            PremiumPlan(
                planId = planId,
                productId = details.productId,
                basePlanId = offer.basePlanId,
                offerToken = offer.offerToken,
                formattedPrice = formattedPrice,
                billingPeriod = iso,
                freeTrial = freeTrial,
                offerTag = offerTag,
                isPopular = false,
                planType = planType,
                priceAmountMicros = priceMicros,
                perMonthFormatted = PriceMath.microsToPerMonthMicros(planType, priceMicros)?.let {
                    formatMicros(it, currency)
                },
                discountPercent = null,
            )
        }
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
        )
    }

    private fun formatTrialLabel(phase: PricingPhase): String {
        val cycles = phase.billingCycleCount
        val period = phase.billingPeriod
        return if (cycles > 0) "$cycles× $period trial" else "Trial ($period)"
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
            val discount = baselinePerMonthMicros?.let { base ->
                val mine = PriceMath.microsToPerMonthMicros(plan.planType, plan.priceAmountMicros) ?: return@let null
                PriceMath.discountPercentVsBaseline(mine, base)
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
