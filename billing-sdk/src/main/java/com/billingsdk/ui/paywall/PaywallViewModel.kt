package com.billingsdk.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billingsdk.domain.BillingRepository
import com.billingsdk.model.BillingSdkConfig
import com.billingsdk.model.BillingUiEvent
import com.billingsdk.model.BillingUserMessage
import com.billingsdk.model.PlanType
import com.billingsdk.model.PremiumPlan
import com.billingsdk.utils.BillingMessageResolver
import com.billingsdk.utils.PlanSelectionHeuristic
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val repository: BillingRepository,
    private val config: BillingSdkConfig,
) : ViewModel() {

    val paywallCustomization = config.paywallCustomization

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    init {
        config.analytics.onPaywallShown()

        viewModelScope.launch {
            repository.plans.collect { plans ->
                val ordered = plans.sortedBy { displayRank(it.planType) }
                _state.update { st ->
                    val selected = st.selectedPlanId ?: PlanSelectionHeuristic.preferredPlanId(ordered)
                    st.copy(plans = ordered, selectedPlanId = selected)
                }
            }
        }

        viewModelScope.launch {
            repository.planLoadError.collect { error ->
                _state.update { it.copy(planError = error) }
            }
        }

        viewModelScope.launch {
            repository.uiEvents.collect { event ->
                when (event) {
                    is BillingUiEvent.PurchaseCanceled -> {
                        _state.update { it.copy(purchasing = false) }
                    }

                    is BillingUiEvent.PurchaseError -> {
                        _state.update {
                            it.copy(
                                purchasing = false,
                                snackbar = event.message,
                            )
                        }
                    }

                    is BillingUiEvent.PurchaseSuccess -> {
                        _state.update { it.copy(purchasing = false, finish = true) }
                    }

                    is BillingUiEvent.RestoreFinished -> {
                        _state.update {
                            it.copy(
                                snackbar = if (event.premiumStatus.isPremium) {
                                    BillingUserMessage("Restored", "Your purchases were restored.")
                                } else {
                                    BillingUserMessage("Nothing to restore", "No active purchases found for this account.")
                                },
                            )
                        }
                    }
                }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, snackbar = null, planError = null) }
            runCatching { repository.refreshPlans() }
                .onFailure { throwable ->
                    // Keep it on screen: an unreachable catalog is the whole
                    // content of this screen, not a passing notification.
                    val message = BillingMessageResolver.forThrowable(throwable)
                    _state.update { it.copy(planError = message) }
                }
            _state.update { it.copy(loading = false) }
        }
    }

    fun selectPlan(planId: String) {
        _state.update { it.copy(selectedPlanId = planId) }
    }

    fun purchase(activity: Activity) {
        val planId = _state.value.selectedPlanId ?: return
        viewModelScope.launch {
            _state.update { it.copy(purchasing = true) }
            runCatching { repository.launchPurchase(activity, planId) }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            purchasing = false,
                            snackbar = BillingMessageResolver.forThrowable(throwable),
                        )
                    }
                }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repository.restorePurchases() }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            snackbar = BillingMessageResolver.forThrowable(throwable),
                        )
                    }
                }
            _state.update { it.copy(loading = false) }
        }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun consumeFinish() {
        _state.update { it.copy(finish = false) }
    }

    /** Lifetime leads the list when it exists; subscriptions follow, shortest first. */
    private fun displayRank(type: PlanType): Int = when (type) {
        PlanType.LIFETIME -> 0
        PlanType.WEEKLY -> 1
        PlanType.MONTHLY -> 2
        PlanType.YEARLY -> 3
    }

    fun ctaLabel(selected: PremiumPlan?): String {
        val c = config.paywallCustomization
        if (selected == null) return c.ctaContinue
        if (!selected.freeTrial.isNullOrBlank()) return c.ctaStartTrial
        return when (selected.planType) {
            PlanType.LIFETIME -> c.ctaLifetime
            else -> c.ctaUnlockPremium
        }
    }
}
