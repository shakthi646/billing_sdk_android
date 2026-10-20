package com.billingsdk.ui.paywall

import com.billingsdk.model.BillingUserMessage
import com.billingsdk.model.PremiumPlan

data class PaywallUiState(
    val loading: Boolean = true,
    val plans: List<PremiumPlan> = emptyList(),
    val selectedPlanId: String? = null,
    val purchasing: Boolean = false,
    val snackbar: BillingUserMessage? = null,
    val finish: Boolean = false,
    /**
     * Why [plans] is empty, when it is. Held in state rather than shown as a
     * transient snackbar so the paywall can explain itself instead of rendering
     * a price list with nothing in it.
     */
    val planError: BillingUserMessage? = null,
)
