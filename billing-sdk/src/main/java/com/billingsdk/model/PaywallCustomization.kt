package com.billingsdk.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Host-controlled presentation for the Compose paywall.
 */
data class PaywallCustomization(
    val appName: String,
    val premiumTitle: String,
    val subtitle: String,
    val featureLines: List<String>,
    val termsUrl: String?,
    val privacyUrl: String?,
    val gradientStart: Color,
    val gradientEnd: Color,
    val glassTint: Color,
    val accent: Color,
    val onAccent: Color,
    val headlineFontFamily: FontFamily?,
    val bodyFontFamily: FontFamily?,
    val headlineSize: TextUnit,
    val bodySize: TextUnit,
    val ctaUnlockPremium: String,
    val ctaContinue: String,
    val ctaStartTrial: String,
    val ctaLifetime: String,
    val appIconResId: Int?,
) {
    companion object {
        val Default = PaywallCustomization(
            appName = "",
            premiumTitle = "Unlock everything",
            subtitle = "Every feature, no ads — on every device you own.",
            featureLines = listOf(
                "Unlimited access",
                "No ads",
                "Faster experience",
                "Exclusive features",
                "Offline support",
            ),
            termsUrl = null,
            privacyUrl = null,
            gradientStart = Color(0xFF0B1020),
            gradientEnd = Color(0xFF1B0F3A),
            glassTint = Color.White.copy(alpha = 0.10f),
            accent = Color(0xFF7C3AED),
            onAccent = Color.White,
            headlineFontFamily = null,
            bodyFontFamily = null,
            headlineSize = 30.sp,
            bodySize = 15.sp,
            ctaUnlockPremium = "Unlock Premium",
            ctaContinue = "Continue",
            ctaStartTrial = "Start Free Trial",
            ctaLifetime = "Get Lifetime Access",
            appIconResId = null,
        )
    }
}
