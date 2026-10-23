package com.billingsdk.utils

import com.billingsdk.model.PlanType
import com.billingsdk.model.PremiumPlan

/**
 * Default selection rules: prefer lifetime when it is offered, then yearly, monthly, weekly.
 *
 * Lifetime leads because it is the headline option on this paywall — it sits at the
 * top of the list, so pre-selecting anything else would leave the tick mark away
 * from the first card the user reads.
 */
object PlanSelectionHeuristic {

    private val PREFERENCE = listOf(
        PlanType.LIFETIME,
        PlanType.YEARLY,
        PlanType.MONTHLY,
        PlanType.WEEKLY,
    )

    fun preferredPlanId(plans: List<PremiumPlan>): String? {
        if (plans.isEmpty()) return null
        PREFERENCE.forEach { type ->
            plans.firstOrNull { it.planType == type }?.let { return it.planId }
        }
        return plans.first().planId
    }
}
