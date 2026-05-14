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
)
