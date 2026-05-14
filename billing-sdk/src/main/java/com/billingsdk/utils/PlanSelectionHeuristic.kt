package com.billingsdk.utils

import com.billingsdk.model.PlanType
import com.billingsdk.model.PremiumPlan

/**
 * Default selection rules: prefer yearly, then monthly, then weekly, else lifetime-only.
 */
object PlanSelectionHeuristic {
    fun preferredPlanId(plans: List<PremiumPlan>): String? {
        if (plans.isEmpty()) return null
        val yearly = plans.firstOrNull { it.planType == PlanType.YEARLY }
        if (yearly != null) return yearly.planId
        val monthly = plans.firstOrNull { it.planType == PlanType.MONTHLY }
        if (monthly != null) return monthly.planId
        val weekly = plans.firstOrNull { it.planType == PlanType.WEEKLY }
        if (weekly != null) return weekly.planId
        val lifetime = plans.firstOrNull { it.planType == PlanType.LIFETIME }
        return lifetime?.planId ?: plans.first().planId
    }
}
