package com.billingsdk.utils

/** Stable plan id encoding for purchase APIs (internal format). */
object PlanIds {
    private const val SEP = "\u001e"

    fun encode(productId: String, basePlanId: String?, offerToken: String?): String =
        listOf(productId, basePlanId.orEmpty(), offerToken.orEmpty()).joinToString(SEP)

    fun decode(planId: String): Triple<String, String?, String?> {
        val parts = planId.split(SEP)
        val pid = parts.getOrElse(0) { "" }
        val base = parts.getOrNull(1)?.ifBlank { null }
        val token = parts.getOrNull(2)?.ifBlank { null }
        return Triple(pid, base, token)
    }
}
