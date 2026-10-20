package com.billingsdk.ui.paywall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaywallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: PaywallViewModel = hiltViewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val customization = vm.paywallCustomization

            val scheme = darkColorScheme(
                primary = customization.accent,
                onPrimary = customization.onAccent,
                background = customization.gradientStart,
                surface = Color(0xFF121212),
            )

            MaterialTheme(colorScheme = scheme) {
                PaywallRoute(
                    state = state,
                    customization = customization,
                    onClose = { finish() },
                    onSelectPlan = vm::selectPlan,
                    onPurchase = { vm.purchase(this@PaywallActivity) },
                    onRestore = vm::restorePurchases,
                    onRetry = vm::refresh,
                    onConsumeSnackbar = vm::consumeSnackbar,
                    onConsumeFinish = vm::consumeFinish,
                    snackbarHostState = snackbarHostState,
                    ctaLabel = vm.ctaLabel(state.plans.firstOrNull { it.planId == state.selectedPlanId }),
                )
            }
        }
    }
}
