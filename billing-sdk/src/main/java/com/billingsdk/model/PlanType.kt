package com.billingsdk.model

/**
 * High-level plan classification derived from Play pricing phases (ISO 8601 billing periods).
 * [PlanType.LIFETIME] is used for one-time (INAPP) products such as `lifetime_premium`.
 */
enum class PlanType {
    WEEKLY,
    MONTHLY,
    YEARLY,
    LIFETIME,
}
