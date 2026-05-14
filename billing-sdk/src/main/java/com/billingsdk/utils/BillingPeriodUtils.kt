package com.billingsdk.utils

import com.billingsdk.model.PlanType

/**
 * Maps Play ISO 8601 durations (e.g. P1W, P1M, P1Y, P3M) to [PlanType] without assuming SKUs.
 */
object BillingPeriodUtils {
    fun planTypeFromIsoDuration(isoDuration: String?): PlanType {
        if (isoDuration.isNullOrBlank()) return PlanType.MONTHLY
        val d = isoDuration.trim().uppercase()
        return when {
            d.startsWith("P") && d.contains("W") -> PlanType.WEEKLY
            d.startsWith("P") && d.contains("Y") -> PlanType.YEARLY
            d.startsWith("P") && d.contains("M") && !d.contains("T") -> {
                val n = d.removePrefix("P").removeSuffix("M").toIntOrNull() ?: 1
                if (n >= 12) PlanType.YEARLY else PlanType.MONTHLY
            }
            else -> PlanType.MONTHLY
        }
    }

    fun millisForPlanType(planType: PlanType): Long? =
        when (planType) {
            PlanType.WEEKLY -> 7L * 24L * 60L * 60L * 1000L
            PlanType.MONTHLY -> 30L * 24L * 60L * 60L * 1000L
            PlanType.YEARLY -> 365L * 24L * 60L * 60L * 1000L
            PlanType.LIFETIME -> null
        }
}
