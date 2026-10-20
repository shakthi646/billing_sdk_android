package com.billingsdk.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billingsdk.model.BillingUserMessage
import com.billingsdk.model.PaywallCustomization
import com.billingsdk.model.PlanType
import com.billingsdk.model.PremiumPlan

@Composable
fun PaywallRoute(
    state: PaywallUiState,
    customization: PaywallCustomization,
    onClose: () -> Unit,
    onSelectPlan: (String) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onConsumeSnackbar: () -> Unit,
    onConsumeFinish: () -> Unit,
    snackbarHostState: SnackbarHostState,
    ctaLabel: String,
) {
    val context = LocalContext.current

    LaunchedEffect(state.snackbar) {
        val msg = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg.title + (msg.detail?.let { "\n$it" } ?: ""))
        onConsumeSnackbar()
    }

    LaunchedEffect(state.finish) {
        if (state.finish) {
            onConsumeFinish()
            (context as? android.app.Activity)?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PaywallScreen(
            state = state,
            customization = customization,
            onClose = onClose,
            onSelectPlan = onSelectPlan,
            onPurchase = onPurchase,
            onRestore = onRestore,
            onRetry = onRetry,
            ctaLabel = ctaLabel,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun PaywallScreen(
    state: PaywallUiState,
    customization: PaywallCustomization,
    onClose: () -> Unit,
    onSelectPlan: (String) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    ctaLabel: String,
) {
    val gradient = Brush.verticalGradient(
        listOf(customization.gradientStart, customization.gradientEnd),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            bottomBar = {
                PaywallBottomBar(
                    state = state,
                    customization = customization,
                    ctaLabel = ctaLabel,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PremiumHero(customization, onClose)
                }

                // Above the feature list, not after the (absent) plans: when the
                // catalog is empty this card is the only actionable thing on the
                // screen, and at the bottom it sat below the fold behind the CTA bar.
                if (!state.loading && state.plans.isEmpty()) {
                    item {
                        PlansUnavailableCard(
                            message = state.planError,
                            customization = customization,
                            onRetry = onRetry,
                        )
                    }
                }

                item {
                    FeatureChecklist(customization.featureLines)
                }

                item {
                    Spacer(Modifier.size(20.dp))
                }

                itemsIndexed(state.plans, key = { _, p -> p.planId }) { _, plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan.planId == state.selectedPlanId,
                        customization = customization,
                        onClick = { onSelectPlan(plan.planId) },
                    )
                }

            }
        }

        if (state.loading && state.plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = customization.accent)
            }
        }
    }
}

/**
 * Shown when the catalog resolves to nothing.
 *
 * Without this the paywall rendered its hero and feature list above an empty
 * gap with a dead CTA, which read as a broken screen and told neither the user
 * nor the developer what had gone wrong.
 */
@Composable
private fun PlansUnavailableCard(
    message: BillingUserMessage?,
    customization: PaywallCustomization,
    onRetry: () -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = customization.accent,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = message?.title ?: "No plans available",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            message?.detail?.let { detail ->
                Text(
                    text = detail,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onRetry) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = customization.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Try again",
                        color = customization.accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaywallBottomBar(
    state: PaywallUiState,
    customization: PaywallCustomization,
    ctaLabel: String,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Surface(
        color = customization.gradientEnd.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PurchaseButton(
                enabled = !state.loading && state.selectedPlanId != null && !state.purchasing,
                loading = state.purchasing,
                label = ctaLabel,
                customization = customization,
                onClick = onPurchase,
            )
            FooterActionsRow(
                customization = customization,
                onRestore = onRestore,
                restoreEnabled = !state.purchasing,
            )
        }
    }
}

@Composable
private fun PremiumHero(
    customization: PaywallCustomization,
    onClose: () -> Unit,
) {

    val transition = rememberInfiniteTransition(label = "badge")

    val angle by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tilt",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {

        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            // PREMIUM Badge
            Surface(
                color = customization.glassTint,
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .rotate(angle),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {

                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = customization.accent,
                        )

                        Text(
                            "PREMIUM",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            // Glassmorphism Close Button
            Surface(
                onClick = onClose,
                color = customization.glassTint,
                shape = CircleShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // Center Content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            Text(
                text = customization.premiumTitle,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = customization.headlineFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = customization.headlineSize,
                ),
                color = Color.White,
            )

            Text(
                text = customization.subtitle,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = customization.bodyFontFamily,
                    fontSize = customization.bodySize,
                ),
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun FeatureChecklist(lines: List<String>) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            lines.forEachIndexed { index, line ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(280, delayMillis = index * 60)) + slideInVertically(
                        tween(280, delayMillis = index * 60),
                    ) { it / 3 },
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFD9D5D6),
                            modifier = Modifier.size(5.dp),
                        )
                        Text(line, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    selected: Boolean,
    customization: PaywallCustomization,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selected) customization.accent else Color.White.copy(alpha = 0.18f),
        label = "border",
    )
    val borderWidth = if (selected) 2.dp else 1.dp
    val glow = if (selected) customization.accent.copy(alpha = 0.18f) else Color.Transparent

    val badgeText = planBadgeText(plan)

    Box(modifier = Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(20.dp))
                .background(glow, RoundedCornerShape(20.dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SelectionCircle(selected = selected, color = customization.accent)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = plan.planType.displayName(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = planSubtitle(plan),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Text(
                    text = plan.formattedPrice + plan.planType.priceSuffix(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        if (badgeText != null) {
            FloatingPill(
                text = badgeText,
                color = customization.accent,
                contentColor = customization.onAccent,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun SelectionCircle(selected: Boolean, color: Color) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        )
    }
}

@Composable
private fun FloatingPill(
    text: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun PurchaseButton(
    enabled: Boolean,
    loading: Boolean,
    label: String,
    customization: PaywallCustomization,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = customization.accent,
            contentColor = customization.onAccent,
            disabledContainerColor = customization.accent.copy(alpha = 0.35f),
            disabledContentColor = customization.onAccent.copy(alpha = 0.65f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = customization.onAccent,
            )
        } else {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FooterActionsRow(
    customization: PaywallCustomization,
    onRestore: () -> Unit,
    restoreEnabled: Boolean,
) {
    val context = LocalContext.current
    val linkColor = Color.White.copy(alpha = 0.75f)
    val dotColor = Color.White.copy(alpha = 0.35f)
    val linkStyle = MaterialTheme.typography.labelMedium

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Restore Purchases",
            color = linkColor,
            style = linkStyle,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = restoreEnabled, onClick = onRestore)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )

        customization.termsUrl?.let { url ->
            Text(text = "·", color = dotColor, style = linkStyle)
            Text(
                text = "Terms",
                color = linkColor,
                style = linkStyle,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        customization.privacyUrl?.let { url ->
            Text(text = "·", color = dotColor, style = linkStyle)
            Text(
                text = "Privacy",
                color = linkColor,
                style = linkStyle,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

private fun PlanType.displayName(): String =
    when (this) {
        PlanType.WEEKLY -> "Weekly"
        PlanType.MONTHLY -> "Monthly"
        PlanType.YEARLY -> "Yearly"
        PlanType.LIFETIME -> "Lifetime"
    }

private fun PlanType.priceSuffix(): String =
    when (this) {
        PlanType.WEEKLY -> "/wk"
        PlanType.MONTHLY -> "/mo"
        PlanType.YEARLY -> "/yr"
        PlanType.LIFETIME -> ""
    }

private fun planBadgeText(plan: PremiumPlan): String? = when {
    plan.discountPercent != null -> "${plan.discountPercent}% OFF"
    plan.freeTrial != null -> "FREE TRIAL"
    plan.planType == PlanType.LIFETIME -> "BEST VALUE"
    plan.isPopular -> "MOST POPULAR"
    else -> null
}

private fun planSubtitle(plan: PremiumPlan): String = when (plan.planType) {
    PlanType.WEEKLY -> "Billed weekly"
    PlanType.MONTHLY -> "Billed at ${plan.formattedPrice}/mo."
    PlanType.YEARLY -> plan.perMonthFormatted?.let { "Only $it/mo" } ?: "Billed annually"
    PlanType.LIFETIME -> "One-time purchase"
}
