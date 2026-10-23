package com.billingsdk.utils

import com.billingsdk.model.PlanType

/**
 * Maps Play ISO 8601 durations (e.g. P1W, P1M, P1Y, P3M) to [PlanType] without assuming SKUs.
 */
object BillingPeriodUtils {

    private val ISO_DURATION = Regex("""^P(\d+)([DWMY])$""")

    /**
     * Renders a Play ISO 8601 duration as human text — "P1M" -> "month",
     * "P3D" -> "3 days". [cycles] multiplies the period, so a P1M phase billed
     * for 3 cycles reads "3 months". Returns `null` when the duration is not a
     * plain single-unit period (e.g. anything with a time component).
     */
    fun describeIsoDuration(isoDuration: String?, cycles: Int = 1): String? {
        val match = ISO_DURATION.matchEntire(isoDuration?.trim()?.uppercase() ?: return null)
            ?: return null
        val count = (match.groupValues[1].toIntOrNull() ?: return null) * cycles.coerceAtLeast(1)
        val unit = when (match.groupValues[2]) {
            "D" -> "day"
            "W" -> "week"
            "M" -> "month"
            "Y" -> "year"
            else -> return null
        }
        return if (count == 1) unit else "$count ${unit}s"
    }
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
