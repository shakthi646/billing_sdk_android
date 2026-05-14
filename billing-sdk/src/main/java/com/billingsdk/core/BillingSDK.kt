package com.billingsdk.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.billingsdk.model.BillingSdkConfig
import com.billingsdk.model.PaywallCustomization
import com.billingsdk.model.PremiumPlan
import com.billingsdk.model.PremiumStatus
import com.billingsdk.ui.paywall.PaywallActivity
import com.billingsdk.utils.BillingLogger
import com.billingsdk.utils.NetworkConnectivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Host-facing facade. Call [initialize] from your [Application] **before** [Application.onCreate] invokes
 * `super.onCreate()` so Hilt singletons can resolve [com.billingsdk.model.BillingSdkConfig].
 *
 * The SDK persists the latest [PremiumStatus] in DataStore (offline cache) and auto-refreshes from
 * Google Play whenever the app comes to the foreground and the device is online. Offline launches
 * use the cached status. Read it via [observePremiumStatus] or [isPremium].
 */
object BillingSDK {
    @Volatile
    private var app: Application? = null
    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lifecycleObserverInstalled = false

    fun initialize(
        context: Context,
        publicKeyBase64: String,
        enableLogging: Boolean,
        paywallCustomization: PaywallCustomization? = null,
        analytics: com.billingsdk.analytics.BillingAnalytics? = null,
    ) {
        initialize(
            BillingSdkConfig.default(
                publicKeyBase64 = publicKeyBase64,
                enableLogging = enableLogging,
                paywallCustomization = paywallCustomization ?: PaywallCustomization.Default,
                analytics = analytics ?: com.billingsdk.analytics.NoOpBillingAnalytics,
            ),
            context,
        )
    }

    fun initialize(config: BillingSdkConfig, context: Context) {
        app = context.applicationContext as Application
        BillingSdkConfigHolder.set(config)
        BillingLogger.setEnabled(config.enableLogging)
        installForegroundRefresh()
    }

    /**
     * Manually refresh the cached premium status from Google Play. Silently skipped when offline —
     * cached status from [PremiumCacheStore] remains the source of truth in that case.
     */
    fun refreshPremiumStatusIfOnline() {
        val ctx = app?.applicationContext ?: return
        sdkScope.launch {
            try {
                if (NetworkConnectivity.isOnline(ctx)) {
                    repo().syncPremiumFromPlay()
                    BillingLogger.d("Premium status refreshed from Play")
                } else {
                    BillingLogger.d("Offline — keeping cached premium status")
                }
            } catch (t: Throwable) {
                BillingLogger.w("Premium refresh failed", t)
            }
        }
    }

    fun launchPaywall(activity: Activity) {
        showPaywall(activity)
    }

    fun showPaywall(activity: Activity) {
        activity.startActivity(Intent(activity, PaywallActivity::class.java))
    }

    /**
     * Best-effort synchronous read of the persisted cache. Prefer [observePremiumStatus] in
     * coroutine / Compose code.
     */
    fun isPremium(): Boolean = runBlocking {
        repo().observePremiumStatus().first().isPremium
    }

    suspend fun restorePurchases() {
        repo().restorePurchases()
    }

    suspend fun purchase(activity: Activity, planId: String) {
        repo().launchPurchase(activity, planId)
    }

    suspend fun getAvailablePlans(): List<PremiumPlan> = repo().refreshPlans()

    fun observePremiumStatus(): Flow<PremiumStatus> = repo().observePremiumStatus()

    private fun installForegroundRefresh() {
        if (lifecycleObserverInstalled) return
        lifecycleObserverInstalled = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    refreshPremiumStatusIfOnline()
                }
            },
        )
    }

    private fun repo() = EntryPointAccessors.fromApplication(
        requireNotNull(app) { "BillingSDK.initialize(...) was not called." },
        BillingSdkEntryPoint::class.java,
    ).billingRepository()
}
