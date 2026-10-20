package com.billingsdk.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.UnfetchedProduct
import com.billingsdk.utils.BillingLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BillingSdkException(
    val responseCode: Int,
    message: String?,
) : Exception("Billing($responseCode): $message")

/**
 * Owns a single [BillingClient], automatic reconnection, and purchase update streaming.
 */
@Singleton
class BillingClientManager @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val clientLock = Mutex()
    private val clientRef = AtomicReference<BillingClient?>(null)

    private val _purchaseUpdates = MutableSharedFlow<PurchaseUpdate>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val purchaseUpdates: SharedFlow<PurchaseUpdate> = _purchaseUpdates.asSharedFlow()

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        BillingLogger.d("onPurchasesUpdated code=${billingResult.responseCode} count=${purchases?.size}")
        scope.launch {
            _purchaseUpdates.emit(
                PurchaseUpdate(
                    billingResult = billingResult,
                    purchases = purchases.orEmpty(),
                ),
            )
        }
    }

    suspend fun startConnection(): Boolean = clientLock.withLock {
        withContext(Dispatchers.Main) {
            val existing = clientRef.get()
            if (existing?.isReady == true) return@withContext true

            existing?.endConnection()

            val client = BillingClient.newBuilder(appContext)
                .setListener(this@BillingClientManager)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build(),
                )
                .enableAutoServiceReconnection()
                .build()

            clientRef.set(client)

            suspendCancellableCoroutine { cont ->
                val resumed = AtomicBoolean(false)
                fun resumeOnce(value: Boolean) {
                    if (resumed.compareAndSet(false, true) && cont.isActive) {
                        cont.resume(value)
                    }
                }

                client.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                            BillingLogger.d("Billing setup finished ok=$ok code=${billingResult.responseCode}")
                            if (!ok) {
                                BillingLogger.w("Billing setup failed: ${billingResult.debugMessage}")
                            }
                            resumeOnce(ok)
                        }

                        override fun onBillingServiceDisconnected() {
                            BillingLogger.w("Billing service disconnected (Play auto-reconnect enabled)")
                            resumeOnce(false)
                        }
                    },
                )
            }
        }
    }

    suspend fun isProductDetailsSupported(): Boolean =
        withContext(Dispatchers.Main) {
            val c = clientRef.get() ?: return@withContext false
            c.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS).responseCode ==
                BillingClient.BillingResponseCode.OK
        }

    /**
     * Queries Play for [productIds], returning both what came back and what did not.
     *
     * Play answers `OK` with an empty list when a product is configured but not
     * purchasable by this caller, so the response code alone cannot distinguish
     * "working, nothing to sell" from "misconfigured". [ProductQueryResult.unfetched]
     * carries Play's per-product status code, which is what actually explains it.
     */
    suspend fun queryProductDetails(
        productIds: List<String>,
        productType: String,
    ): ProductQueryResult =
        withContext(Dispatchers.Main) {
            val client = clientRef.get() ?: throw BillingSdkException(-1, "BillingClient not created")
            if (!client.isReady) {
                throw BillingSdkException(
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                    "not ready",
                )
            }

            suspendCancellableCoroutine { cont ->
                val products = productIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(productType)
                        .build()
                }
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(products)
                    .build()

                client.queryProductDetailsAsync(params) { result, queryResult ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val details = queryResult.productDetailsList.orEmpty()
                        val unfetched = queryResult.unfetchedProductList.orEmpty()
                        if (unfetched.isNotEmpty()) {
                            BillingLogger.w(
                                "queryProductDetails($productType) unfetched=" +
                                    unfetched.joinToString { "${it.productId}:status=${it.statusCode}" },
                            )
                        }
                        cont.resume(ProductQueryResult(details = details, unfetched = unfetched))
                    } else {
                        cont.resumeWithException(BillingSdkException(result.responseCode, result.debugMessage))
                    }
                }
            }
        }

    suspend fun queryPurchases(productType: String): List<Purchase> =
        withContext(Dispatchers.Main) {
            val client = clientRef.get() ?: throw BillingSdkException(-1, "BillingClient not created")
            if (!client.isReady) {
                throw BillingSdkException(
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                    "not ready",
                )
            }

            suspendCancellableCoroutine { cont ->
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchases.orEmpty())
                    } else {
                        cont.resumeWithException(BillingSdkException(result.responseCode, result.debugMessage))
                    }
                }
            }
        }

    suspend fun acknowledgePurchase(purchaseToken: String): BillingResult =
        withContext(Dispatchers.Main) {
            val client = clientRef.get() ?: throw BillingSdkException(-1, "BillingClient not created")
            suspendCancellableCoroutine { cont ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
                client.acknowledgePurchase(params) { result ->
                    cont.resume(result)
                }
            }
        }

    fun launchBillingFlow(activity: Activity, params: BillingFlowParams): BillingResult {
        val client = clientRef.get() ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .setDebugMessage("BillingClient missing")
            .build()
        return client.launchBillingFlow(activity, params)
    }

    fun endConnection() {
        clientRef.getAndSet(null)?.endConnection()
    }

    data class PurchaseUpdate(
        val billingResult: BillingResult,
        val purchases: List<Purchase>,
    )

    /** Outcome of one [queryProductDetails] call: what Play returned, and what it withheld. */
    data class ProductQueryResult(
        val details: List<ProductDetails>,
        val unfetched: List<UnfetchedProduct>,
    )
}
