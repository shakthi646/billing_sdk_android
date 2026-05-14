# `billing-sdk`

Drop-in **Google Play Billing 8.1+** module for Android. Subscriptions, lifetime in-app purchases, dynamic offers from Play Console, a Compose paywall, and **offline-first premium status** — wired with Hilt, Coroutines + Flow, and DataStore.

This document covers the three things you'll do in 99% of cases:

1. **[Install + initialize](#1-installation)** — wire the module into your app.
2. **[Check if the user is premium](#2-check-if-the-user-is-premium-pro-active)** — gate features, build UI states.
3. **[Show the subscription page](#2-check-if-the-user-is-premium-pro-active)** — open the paywall, handle the result.

Plus reference sections for customization, Play Console setup, architecture, and testing.

---

## 1. Installation

The SDK lives as a **standalone Gradle project** that any number of host apps can consume by path-based include. There is no Maven publish step.

### 1.1 Module layout

```
.../Workspace/Android/
├── billing-sdk-project/         ← this standalone project
│   ├── billing-sdk/             ← the module
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── ...
└── YourApp/                     ← host app (one or many)
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── app/build.gradle.kts
```

### 1.2 Host project — root `build.gradle.kts`

Declare the plugins the SDK needs with `apply false`. The SDK applies them in its own module:

```kotlin
plugins {
    id("com.android.application")                 version "8.5.2"        apply false
    id("com.android.library")                     version "8.5.2"        apply false
    id("org.jetbrains.kotlin.android")            version "2.2.10"       apply false
    id("org.jetbrains.kotlin.plugin.compose")     version "2.2.10"       apply false
    id("com.google.dagger.hilt.android")          version "2.57.1"       apply false
    id("com.google.devtools.ksp")                 version "2.2.10-2.0.2" apply false
}
```

> Hilt **must be ≥ 2.57** for Kotlin 2.2.x — earlier versions fail with a metadata-version mismatch.

### 1.3 Host project — `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "YourApp"
include(":app")

// Point at the standalone billing-sdk-project. Adjust the relative path
// based on where the standalone project lives relative to this host.
include(":billing-sdk")
project(":billing-sdk").projectDir = file("../billing-sdk-project/billing-sdk")
```

### 1.4 Host app — `app/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":billing-sdk"))
    // ... your other dependencies
}
```

Your app module must already use Hilt (`@HiltAndroidApp`, `kspKotlin` with `hilt-android-compiler`). The SDK exposes Hilt singletons that resolve into the host's component graph.

### 1.5 Initialize from the host `Application`

`BillingSDK.initialize(...)` **must run before `super.onCreate()`** so Hilt singletons that read `BillingSdkConfig` see a populated holder. This single call also installs the **process-lifecycle auto-refresh** (see §3).

```kotlin
@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        BillingSDK.initialize(
            context = this,
            publicKeyBase64 = "",                          // reserved for server verification
            enableLogging   = BuildConfig.DEBUG,
            paywallCustomization = PaywallCustomization.Default.copy(
                appName       = "My App",
                premiumTitle  = "Unlock everything",
                subtitle      = "Every feature, no ads — on every device you own.",
                featureLines  = listOf(
                    "Unlimited reminders",
                    "Advanced reports",
                    "No ads, ever",
                ),
                termsUrl   = "https://example.com/terms",
                privacyUrl = "https://example.com/privacy",
            ),
        )
        super.onCreate()
    }
}
```

#### Custom product ids / analytics

Default ids: `premium_plan` (subscription) and `lifetime_premium` (one-time). Override when your Play Console products are named differently:

```kotlin
BillingSDK.initialize(
    config = BillingSdkConfig.default(
        publicKeyBase64 = "",
        enableLogging   = BuildConfig.DEBUG,
        paywallCustomization = PaywallCustomization.Default.copy( /* ... */ ),
        analytics = object : BillingAnalytics {
            override fun onPaywallShown() { /* forward to Firebase */ }
            override fun onPurchaseStarted(planId: String) { /* ... */ }
            override fun onPurchaseSucceeded(planId: String) { /* ... */ }
            override fun onPurchaseFailed(planId: String, reason: String) { /* ... */ }
        },
    ).copy(
        subscriptionProductId = "pro_subscription",     // ← your Play subscription id
        lifetimeProductId     = "pro_lifetime",         // ← your Play one-time id
    ),
    context = this,
)
```

#### Manifest

The SDK manifest already declares everything needed:

- `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` — for offline-aware refresh.
- `<uses-permission android:name="com.android.vending.BILLING" />` — Play Billing.
- `<activity android:name="com.billingsdk.ui.paywall.PaywallActivity" android:exported="false" />`.

No extra entries are required in the host manifest.

---

## 2. Check if the user is premium (pro / active)

The SDK exposes premium status as a **`Flow<PremiumStatus>`** backed by DataStore. It is **offline-first** — the latest known status is persisted on every change, so cold starts and offline launches return immediately.

### 2.1 `PremiumStatus` model

```kotlin
data class PremiumStatus(
    val isPremium: Boolean,        // true if any active subscription OR lifetime owned
    val activePlan: String?,       // e.g. "premium_plan:p1m" — Play's basePlanId
    val expiryDateMillis: Long?,   // subscriptions only; null for lifetime / free
    val purchaseToken: String?,    // current token; useful for server verification
    val isLifetime: Boolean,       // true if user owns the one-time lifetime product
    val lastSyncedMillis: Long,    // wall-clock of last successful Play refresh
)
```

### 2.2 In a Composable — preferred

```kotlin
@Composable
fun MyScreen() {
    val status by BillingSDK.observePremiumStatus().collectAsState(
        initial = PremiumStatus(
            isPremium        = false,
            activePlan       = null,
            expiryDateMillis = null,
            purchaseToken    = null,
            isLifetime       = false,
            lastSyncedMillis = 0L,
        ),
    )

    if (status.isPremium) {
        ProFeatureUi()
    } else {
        UpgradeCta(onClick = { /* see §3 */ })
    }
}
```

### 2.3 In a ViewModel — reactive

Inject Hilt-managed singletons in your VM and combine with other flows:

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    data class UiState(val isPremium: Boolean = false, /* ... */)

    val state: StateFlow<UiState> = BillingSDK.observePremiumStatus()
        .map { UiState(isPremium = it.isPremium) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
```

### 2.4 In a coroutine — snapshot

```kotlin
viewModelScope.launch {
    val status = BillingSDK.observePremiumStatus().first()
    if (status.isPremium) doProThing() else openPaywall()
}
```

Or, when calling from non-coroutine code:

```kotlin
val isPremium: Boolean = BillingSDK.isPremium()   // runBlocking — avoid on the main thread
```

### 2.5 Refreshing the cache

The cache **auto-refreshes** whenever:

| Trigger | What happens |
|---|---|
| App cold start | `ProcessLifecycleOwner.onStart` fires → `refreshPremiumStatusIfOnline()` |
| App returns to foreground | Same as above |
| Online | SDK queries Play (`queryPurchases` for SUBS + INAPP) → writes `PremiumStatus` to DataStore |
| Offline | Refresh is skipped silently → cached value remains the source of truth |
| Purchase / restore succeeds | Cache updated → every `observePremiumStatus()` collector flips instantly |

You rarely need to call refresh yourself, but you can:

```kotlin
BillingSDK.refreshPremiumStatusIfOnline()   // no-op when offline
```

### 2.6 Common gating patterns

**Feature card in Settings**

```kotlin
@Composable
fun PremiumCard() {
    val status by BillingSDK.observePremiumStatus().collectAsState(initial = /* default */)
    val activity = LocalContext.current as? ComponentActivity
    if (status.isPremium) {
        PremiumActiveCard(status)
    } else {
        UpgradeCard(onClick = { activity?.let { BillingSDK.showPaywall(it) } })
    }
}
```

**Tap-to-record gate (with trial)**

```kotlin
// ViewModel
fun onRecordTap() {
    viewModelScope.launch {
        val premium = BillingSDK.observePremiumStatus().first().isPremium
        val inTrial = trialRepository.isInTrial()
        if (premium || inTrial) {
            performRecord()
        } else {
            _paywallEvent.tryEmit(Unit)   // screen collects this and opens paywall
        }
    }
}
```

---

## 3. Show the subscription / paywall page

The paywall is a full-screen Compose `Activity` (`PaywallActivity`) packaged in the SDK. You launch it via a single call.

### 3.1 Launch

```kotlin
// From any Activity:
BillingSDK.showPaywall(activity)

// From a Composable that holds an Activity:
val activity = LocalContext.current as? ComponentActivity
Button(onClick = { activity?.let { BillingSDK.showPaywall(it) } }) {
    Text("Upgrade")
}
```

That's it — the SDK handles plan fetch, selection, purchase, restore, and result persistence internally. The activity closes itself after a successful purchase.

### 3.2 What the paywall shows

```
┌──────────────────────────────────────────┐
│  [App name]                         [X]  │
│  ┌──────────────┐                        │
│  │ ★ PREMIUM    │  (animated tilt)       │
│  └──────────────┘                        │
│  Unlock everything                       │
│  Every feature, no ads — on every device │
│  ┌──────────────────────────────────┐    │
│  │ ✓ Unlimited reminders            │    │
│  │ ✓ Advanced reports               │    │
│  │ ✓ No ads, ever                   │    │
│  └──────────────────────────────────┘    │
│  Choose your plan                        │
│  ┌─ Weekly  ────────────────────────┐    │
│  │ ₹49 / week                       │    │
│  └──────────────────────────────────┘    │
│  ┌─ Monthly ────────────────────────┐    │
│  │ ₹149 / mo                        │    │
│  └──────────────────────────────────┘    │
│  ┌─ Yearly ─────────── MOST POPULAR ┐    │ ← yearly auto-promoted
│  │ ₹999 / year   ·  ₹83 / mo        │    │
│  │ [FREE TRIAL] [SAVE 60%]          │    │
│  └──────────────────────────────────┘    │
│  ┌─ Lifetime ──────────── BEST VALUE ┐   │
│  │ ₹1,999 once                       │   │
│  └──────────────────────────────────┘    │
│  Terms · Privacy · Cancel anytime…       │
├──────────────────────────────────────────┤  ← sticky bottom bar
│        [  Unlock Premium  ]              │
│            Restore purchases             │
└──────────────────────────────────────────┘
```

Order is fixed: **Weekly → Monthly → Yearly → Lifetime** (any plan absent in Play Console is just hidden). Yearly is pre-selected via `PlanSelectionHeuristic` and shown with auto-computed `SAVE X%` and `MOST POPULAR` badges.

### 3.3 When should you show it?

| Scenario | Action |
|---|---|
| User taps an "Upgrade" / "Go Pro" CTA | `BillingSDK.showPaywall(activity)` directly. |
| User taps a gated feature and is not premium | Open paywall **immediately** (don't show a separate "this is pro" dialog). |
| User is already premium | Do **not** show the paywall — show a "Premium active" UI in your screen instead (see §2.6). |
| Free trial expired and user is not premium | Open paywall and treat the back-press as a re-prompt (you decide UX). |

### 3.4 Reacting to a successful purchase

You don't need a result callback. Anywhere in your app that collects `observePremiumStatus()` will receive a fresh `PremiumStatus(isPremium = true, ...)` automatically once Play acknowledges the purchase. The paywall activity finishes itself.

If you need to do extra side-effects (analytics, server sync, navigate somewhere), watch the flow:

```kotlin
LaunchedEffect(Unit) {
    BillingSDK.observePremiumStatus()
        .map { it.isPremium }
        .distinctUntilChanged()
        .filter { it }                 // edge: false → true
        .collect {
            analytics.logUpgrade()
            navController.navigate("welcome_to_pro")
        }
}
```

### 3.5 Restore purchases

The paywall has a built-in **Restore purchases** button in the sticky footer. To trigger it programmatically (e.g. from your own Settings screen):

```kotlin
viewModelScope.launch {
    BillingSDK.restorePurchases()
    // Status flows through observePremiumStatus() — no return value.
}
```

### 3.6 Manual purchase without the paywall

If you want a custom UI and call Play yourself:

```kotlin
viewModelScope.launch {
    val plans: List<PremiumPlan> = BillingSDK.getAvailablePlans()
    val chosen = plans.first { it.planType == PlanType.YEARLY }
    BillingSDK.purchase(activity, chosen.planId)
    // Premium status flips automatically on success.
}
```

`PremiumPlan` carries everything the paywall renders: `planId`, `planType`, `formattedPrice`, `perMonthFormatted`, `freeTrial`, `discountPercent`, `isPopular`.

---

## 4. Customize the paywall

Every visible element on the paywall comes from `PaywallCustomization`. Pass an instance to `BillingSDK.initialize(...)`:

| Field | Effect |
|---|---|
| `appName` | Top-left label (hidden when blank) |
| `premiumTitle` / `subtitle` | Hero copy |
| `featureLines` | Bulleted "what you get" card |
| `gradientStart` / `gradientEnd` | Background gradient |
| `glassTint` / `accent` / `onAccent` | Surface + button colors |
| `termsUrl` / `privacyUrl` | Footer legal links (omit ⇒ link hidden) |
| `ctaUnlockPremium` / `ctaContinue` / `ctaStartTrial` / `ctaLifetime` | Button copy per selected plan type |
| `headlineFontFamily` / `bodyFontFamily` / `headlineSize` / `bodySize` | Typography overrides |

```kotlin
PaywallCustomization.Default.copy(
    accent           = Color(0xFF22D3EE),
    gradientStart    = Color(0xFF0F172A),
    gradientEnd      = Color(0xFF1E293B),
    ctaUnlockPremium = "Subscribe now",
    ctaLifetime      = "Buy lifetime",
    ctaStartTrial    = "Start free trial",
)
```

---

## 5. Public API surface

| API | Purpose |
|---|---|
| `BillingSDK.initialize(...)` | Configure ids, theming, logging, analytics. Installs foreground refresh. |
| `BillingSDK.showPaywall(activity)` / `launchPaywall(activity)` | Opens `PaywallActivity`. |
| `BillingSDK.observePremiumStatus(): Flow<PremiumStatus>` | Reactive premium status from DataStore. |
| `BillingSDK.isPremium(): Boolean` | Snapshot read. `runBlocking` internally — avoid on the main thread. |
| `BillingSDK.refreshPremiumStatusIfOnline()` | Manual refresh. Silently skipped when offline. |
| `suspend BillingSDK.getAvailablePlans(): List<PremiumPlan>` | Force `queryProductDetails` from Play. |
| `suspend BillingSDK.purchase(activity, planId)` | Start the Play billing flow for a `PremiumPlan.planId`. |
| `suspend BillingSDK.restorePurchases()` | Re-sync purchases. Result flows via `observePremiumStatus()`. |

---

## 6. Play Console product setup

1. **Monetize → Products → Subscriptions**: create one product (e.g. `premium_plan`) with one or more **base plans** (weekly / monthly / yearly) and optional offers (free trial, intro pricing, the `popular` tag).
2. **Monetize → Products → In-app products**: create one **managed product** (e.g. `lifetime_premium`) for lifetime access.
3. Activate billing, complete the **payments profile**, and publish the app to at least an **Internal testing** track.
4. Make sure the **signing key** on the test device matches the upload key — otherwise billing fails silently.
5. (Optional) Hook **Real-time developer notifications** to a backend for authoritative expiry / grace state.

If you delete an offer in Console, the next `refreshPlans()` hides it automatically — no code change needed.

---

## 7. Testing purchases

1. Upload a signed build to **Internal testing** (or use **License testing** accounts).
2. Add license testers in Play Console → **Settings → License testing**.
3. Install from Play Store or **Internal app sharing** — plain `adb install` won't resolve real SKUs.
4. Use **Play test cards** for renewals / declines as documented by Google.
5. Toggle airplane mode mid-session to verify the offline cache works: premium UI should keep working without a network connection.

---

## 8. Architecture notes

- **Upgrade / downgrade**: when an active subscription token exists for `subscriptionProductId`, the SDK attaches `SubscriptionUpdateParams` (PBL 8.1+).
- **Auto reconnect**: `BillingClient` is built with `enableAutoServiceReconnection()`.
- **Pending purchases**: `enablePendingPurchases` includes one-time pending products.
- **Persisted fields**: `isPremium`, `activePlan`, `expiryDateMillis` (best-effort, nullable), `purchaseToken`, `isLifetime`, `lastSyncedMillis`.
- **Plan ordering on screen**: weekly → monthly → yearly → lifetime, sorted in the ViewModel.
- **Yearly preferred by default**: `PlanSelectionHeuristic` pre-selects yearly when present.

---

## 9. ProGuard / R8

The SDK ships `consumer-rules.pro` that keeps Play Billing types. Apps with minification rely on the merged ruleset — no extra rules needed in your host module.

---

## 10. Migration path

If you later move to RevenueCat / your own server-side entitlement service, keep host code on `observePremiumStatus()` + `BillingAnalytics`. Swap the `BillingRepository` Hilt binding to a new implementation that publishes the same `PremiumPlan` / `PremiumStatus` models. No host-app changes required.
