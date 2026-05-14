package com.billingsdk.utils

import com.billingsdk.model.PlanType
import kotlin.math.roundToLong

object PriceMath {
    fun microsToPerMonthMicros(planType: PlanType, priceMicros: Long): Long? {
        return when (planType) {
            PlanType.WEEKLY -> (priceMicros * 52.0 / 12.0).roundToLong()
            PlanType.MONTHLY -> priceMicros
            PlanType.YEARLY -> (priceMicros / 12.0).roundToLong()
            PlanType.LIFETIME -> null
        }
    }

    fun discountPercentVsBaseline(priceMicros: Long, baselineMicros: Long): Int? {
        if (baselineMicros <= 0L) return null
        if (priceMicros >= baselineMicros) return null
        val pct = ((baselineMicros - priceMicros) * 100.0 / baselineMicros).roundToLong().toInt()
        return pct.coerceIn(1, 99)
    }
}
