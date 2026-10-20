package com.billingsdk.data

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.UnfetchedProduct
import com.billingsdk.billing.BillingClientManager
import com.billingsdk.billing.BillingSdkException
import com.billingsdk.data.mapper.ProductDetailsMapper
import com.billingsdk.datastore.PremiumCacheStore
import com.billingsdk.domain.BillingRepository
import com.billingsdk.model.BillingSdkConfig
import com.billingsdk.model.BillingUiEvent
import com.billingsdk.model.BillingUserMessage
import com.billingsdk.model.PremiumPlan
import com.billingsdk.model.PremiumStatus
import com.billingsdk.utils.BillingLogger
import com.billingsdk.utils.BillingMessageResolver
import com.billingsdk.utils.PlanIds
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default Play Billing implementation: dynamic catalog, acknowledgement, and premium caching.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val client: BillingClientManager,
    private val cache: PremiumCacheStore,
    private val mapper: ProductDetailsMapper,
    private val config: BillingSdkConfig,
) : BillingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()

    private val lastAttemptedPlanId = AtomicReference<String?>(null)
    private var lastProductDetails: List<ProductDetails> = emptyList()

    private val _plans = MutableStateFlow<List<PremiumPlan>>(emptyList())
    override val plans: Flow<List<PremiumPlan>> = _plans.asStateFlow()

    private val _planLoadError = MutableStateFlow<BillingUserMessage?>(null)
    override val planLoadError: Flow<BillingUserMessage?> = _planLoadError.asStateFlow()

    private val _uiEvents = MutableSharedFlow<BillingUiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val uiEvents: SharedFlow<BillingUiEvent> = _uiEvents.asSharedFlow()

    init {
        scope.launch {
            client.purchaseUpdates.collect { update ->
                handlePurchaseUpdate(update)
            }
        }
    }

    override fun observePremiumStatus(): Flow<PremiumStatus> = cache.premiumStatus

    override suspend fun refreshEntitlements(): PremiumStatus = syncPremiumFromPlay()

    override suspend fun refreshPlans(): List<PremiumPlan> = refreshMutex.withLock {
        val subId = config.subscriptionProductId
        val lifetimeId = config.lifetimeProductId

        if (!client.startConnection()) {
            BillingLogger.w("refreshPlans aborted: could not connect to Google Play")
            _planLoadError.value = BillingUserMessage(
                title = "Can't reach Google Play",
                detail = "Couldn't connect to the Play Store on this device. Check that the " +
                    "Play Store is installed, up to date and signed in, then try again.",
            )
            return@withLock _plans.value
        }

        val subQuery = runCatching {
            client.queryProductDetails(listOf(subId), BillingClient.ProductType.SUBS)
        }.onFailure { BillingLogger.w("Subscription query failed for '$subId'", it) }

        val inAppQuery = runCatching {
            client.queryProductDetails(listOf(lifetimeId), BillingClient.ProductType.INAPP)
        }.onFailure { BillingLogger.w("INAPP query failed for '$lifetimeId'", it) }

        val subDetails = subQuery.getOrNull()?.details.orEmpty()
        val inAppDetails = inAppQuery.getOrNull()?.details.orEmpty()

        lastProductDetails = subDetails + inAppDetails
        val mapped = mapper.mapAll(subId, lifetimeId, lastProductDetails)
        _plans.value = mapped

        _planLoadError.value = if (mapped.isEmpty()) {
            diagnoseEmptyCatalog(subId, subQuery, inAppQuery).also {
                BillingLogger.w("Paywall catalog is empty: ${it.title} — ${it.detail}")
            }
        } else {
            null
        }

        mapped
    }

    /**
     * Works out why the catalog came back empty, in the order the causes actually occur.
     *
     * Play reports a product it will not sell to this caller through
     * `unfetchedProductList` with `OK` as the overall response code, so an empty
     * catalog is not an error path — it has to be diagnosed explicitly.
     */
    private fun diagnoseEmptyCatalog(
        subId: String,
        subQuery: Result<BillingClientManager.ProductQueryResult>,
        inAppQuery: Result<BillingClientManager.ProductQueryResult>,
    ): BillingUserMessage {
        // 1. The subscription query itself failed — Play told us the response code.
        (subQuery.exceptionOrNull() ?: inAppQuery.exceptionOrNull())?.let { failure ->
            return when (failure) {
                is BillingSdkException -> BillingMessageResolver.forResponseCode(
                    failure.responseCode,
                    failure.message,
                )

                else -> BillingMessageResolver.forThrowable(failure)
            }
        }

        val subResult = subQuery.getOrNull()
        val subDetails = subResult?.details?.firstOrNull { it.productId == subId }

        // 2. Play withheld the product and said why.
        subResult?.unfetched?.firstOrNull { it.productId == subId }?.let { unfetched ->
            return unavailableProductMessage(subId, unfetched)
        }

        // 3. The product exists and was returned, but carries no purchasable offer.
        if (subDetails != null && subDetails.subscriptionOfferDetails.orEmpty().isEmpty()) {
            return BillingUserMessage(
                title = "No plans available",
                detail = "Google Play returned '$subId' with no active base plan. Activate a " +
                    "base plan for this subscription in the Play Console and make sure it is " +
                    "available in this country.",
            )
        }

        // 4. Play simply does not know this product for this package and signing key.
        return BillingUserMessage(
            title = "No plans available",
            detail = "Google Play returned no products for this app. Usually this means the " +
                "build isn't the one Play distributes (install it from a Play track rather " +
                "than sideloading), or '$subId' has no active base plan in the Play Console.",
        )
    }

    private fun unavailableProductMessage(
        productId: String,
        unfetched: UnfetchedProduct,
    ): BillingUserMessage = BillingUserMessage(
        title = "No plans available",
        detail = "Google Play could not return '$productId' (status ${unfetched.statusCode}). " +
            "Check that the subscription and at least one base plan are active in the Play " +
            "Console, that this build is distributed by Play, and that the plan is available " +
            "in this country.",
    )

    override suspend fun restorePurchases(): PremiumStatus {
        val status = syncPremiumFromPlay()
        config.analytics.onRestoreSuccess()
        _uiEvents.emit(BillingUiEvent.RestoreFinished(status))
        return status
    }

    override suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean {
        val result = client.acknowledgePurchase(purchaseToken)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun syncPremiumFromPlay(): PremiumStatus {
        if (!client.startConnection()) {
            BillingLogger.w("Entitlement sync skipped: could not connect to Google Play")
            return cache.premiumStatus.first()
        }

        val subs = runCatching { client.queryPurchases(BillingClient.ProductType.SUBS) }
            .onFailure { BillingLogger.w("queryPurchases SUBS failed", it) }
        val inapps = runCatching { client.queryPurchases(BillingClient.ProductType.INAPP) }
            .onFailure { BillingLogger.w("queryPurchases INAPP failed", it) }

        // A failed query is not evidence that nothing is owned. Writing a
        // "not premium" snapshot from a partial answer would evict a paying
        // user's cached entitlement and lock them out of an app that is
        // supposed to keep working offline, so only a complete answer is cached.
        if (subs.isFailure || inapps.isFailure) {
            BillingLogger.w("Entitlement sync incomplete — keeping the cached premium status")
            return cache.premiumStatus.first()
        }

        val status = buildPremiumStatus(subs.getOrDefault(emptyList()) + inapps.getOrDefault(emptyList()))
        cache.write(status)
        return status
    }

    override suspend fun launchPurchase(activity: Activity, planId: String) {
        lastAttemptedPlanId.set(planId)
        config.analytics.onPurchaseStarted(planId)

        client.startConnection()

        if (lastProductDetails.isEmpty()) {
            refreshPlans()
        }

        val (productId, _, offerToken) = PlanIds.decode(planId)
        val details = lastProductDetails.firstOrNull { it.productId == productId }
            ?: run {
                refreshPlans()
                lastProductDetails.firstOrNull { it.productId == productId }
            }
            ?: run {
                // Reuse the catalog diagnosis rather than the generic message: the
                // refresh above has already worked out exactly why Play has nothing.
                val msg = _planLoadError.value ?: BillingUserMessage(
                    title = "Offer unavailable",
                    detail = "Could not load product details from Google Play.",
                )
                _uiEvents.emit(BillingUiEvent.PurchaseError(msg))
                config.analytics.onPurchaseFailed(planId, msg.title)
                return
            }

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        var subscriptionUpdate: BillingFlowParams.SubscriptionUpdateParams? = null

        if (details.productType == BillingClient.ProductType.SUBS) {
            if (offerToken.isNullOrBlank()) {
                val msg = BillingUserMessage(
                    title = "Offer unavailable",
                    detail = "Missing subscription offer token from Play.",
                )
                _uiEvents.emit(BillingUiEvent.PurchaseError(msg))
                config.analytics.onPurchaseFailed(planId, msg.title)
                return
            }
            paramsBuilder.setOfferToken(offerToken)

            val existing = runCatching { client.queryPurchases(BillingClient.ProductType.SUBS) }
                .getOrDefault(emptyList())
                .firstOrNull { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(config.subscriptionProductId)
                }
            subscriptionUpdate = existing?.purchaseToken?.takeIf { it.isNotBlank() }?.let { oldToken ->
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldToken)
                    .setSubscriptionReplacementMode(
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE,
                    )
                    .build()
            }
        } else {
            if (!offerToken.isNullOrBlank()) {
                paramsBuilder.setOfferToken(offerToken)
            }
        }

        val productParams = paramsBuilder.build()

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
        subscriptionUpdate?.let { flowBuilder.setSubscriptionUpdateParams(it) }

        val result = client.launchBillingFlow(activity, flowBuilder.build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = BillingMessageResolver.forResponseCode(result.responseCode, result.debugMessage)
            _uiEvents.emit(BillingUiEvent.PurchaseError(msg))
            config.analytics.onPurchaseFailed(planId, msg.title)
        }
    }

    private suspend fun handlePurchaseUpdate(update: BillingClientManager.PurchaseUpdate) {
        val code = update.billingResult.responseCode
        val planId = lastAttemptedPlanId.get()

        when (code) {
            BillingClient.BillingResponseCode.OK -> {
                for (purchase in update.purchases) {
                    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
                    if (!purchase.isAcknowledged) {
                        val ack = client.acknowledgePurchase(purchase.purchaseToken)
                        BillingLogger.d("acknowledge token=${purchase.purchaseToken} code=${ack.responseCode}")
                    }
                }
                val status = syncPremiumFromPlay()
                if (status.isPremium) {
                    _uiEvents.emit(BillingUiEvent.PurchaseSuccess(planId))
                    if (planId != null) config.analytics.onPurchaseSuccess(planId)
                } else {
                    // Pending purchase / not granted yet
                    val msg = BillingUserMessage(
                        title = "Purchase pending",
                        detail = "Google Play is still processing this purchase.",
                    )
                    _uiEvents.emit(BillingUiEvent.PurchaseError(msg))
                    config.analytics.onPurchaseFailed(planId, msg.title)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiEvents.emit(BillingUiEvent.PurchaseCanceled(planId))
                config.analytics.onPurchaseFailed(planId, "USER_CANCELED")
            }

            else -> {
                val msg = BillingMessageResolver.forResponseCode(code, update.billingResult.debugMessage)
                _uiEvents.emit(BillingUiEvent.PurchaseError(msg))
                config.analytics.onPurchaseFailed(planId, msg.title)
            }
        }
    }

    private fun buildPremiumStatus(purchases: List<Purchase>): PremiumStatus {
        fun isPurchased(p: Purchase): Boolean =
            p.purchaseState == Purchase.PurchaseState.PURCHASED

        val lifetimePurchase = purchases.firstOrNull { p ->
            isPurchased(p) && p.products.contains(config.lifetimeProductId)
        }
        val subPurchase = purchases.firstOrNull { p ->
            isPurchased(p) && p.products.contains(config.subscriptionProductId)
        }

        val lifetimeOwned = lifetimePurchase != null
        val subOwned = subPurchase != null
        val isPremium = lifetimeOwned || subOwned

        val token = lifetimePurchase?.purchaseToken ?: subPurchase?.purchaseToken
        val activePlan = when {
            lifetimeOwned -> config.lifetimeProductId
            subOwned -> config.subscriptionProductId
            else -> null
        }

        return PremiumStatus(
            isPremium = isPremium,
            activePlan = activePlan,
            expiryDateMillis = null,
            purchaseToken = token,
            isLifetime = lifetimeOwned,
            lastSyncedMillis = System.currentTimeMillis(),
        )
    }
}
