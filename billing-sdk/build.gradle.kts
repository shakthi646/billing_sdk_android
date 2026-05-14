// ---------------------------------------------------------------------------
// billing-sdk — self-contained module.
//
// All library versions are pinned in this file so the module can be dropped
// into any Android project without depending on the host's version catalog.
//
// Required plugins (the host project must apply these — same versions):
//   com.android.library                : 8.5.2
//   org.jetbrains.kotlin.android       : 2.2.10
//   org.jetbrains.kotlin.plugin.compose: 2.2.10
//   com.google.dagger.hilt.android     : 2.57.1
//   com.google.devtools.ksp            : 2.2.10-2.0.2
//
// Required repositories in settings.gradle.kts:
//   google(), mavenCentral()
// ---------------------------------------------------------------------------

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// Pinned dependency versions for this SDK.
object SdkVersions {
    const val COMPILE_SDK = 35
    const val MIN_SDK = 26

    const val CORE_KTX = "1.13.1"
    const val BILLING = "8.1.0"
    const val COROUTINES = "1.9.0"
    const val LIFECYCLE = "2.8.4"
    const val ACTIVITY_COMPOSE = "1.9.1"
    const val COMPOSE_BOM = "2024.08.00"
    const val DATASTORE = "1.1.1"
    const val HILT = "2.57.1"
    const val HILT_NAV_COMPOSE = "1.2.0"
}

android {
    namespace = "com.billingsdk"
    compileSdk = SdkVersions.COMPILE_SDK

    defaultConfig {
        minSdk = SdkVersions.MIN_SDK
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:${SdkVersions.CORE_KTX}")
    implementation("com.android.billingclient:billing-ktx:${SdkVersions.BILLING}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${SdkVersions.COROUTINES}")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${SdkVersions.LIFECYCLE}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${SdkVersions.LIFECYCLE}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${SdkVersions.LIFECYCLE}")
    implementation("androidx.lifecycle:lifecycle-process:${SdkVersions.LIFECYCLE}")
    implementation("androidx.activity:activity-compose:${SdkVersions.ACTIVITY_COMPOSE}")

    implementation(platform("androidx.compose:compose-bom:${SdkVersions.COMPOSE_BOM}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:${SdkVersions.DATASTORE}")

    implementation("com.google.dagger:hilt-android:${SdkVersions.HILT}")
    ksp("com.google.dagger:hilt-android-compiler:${SdkVersions.HILT}")
    implementation("androidx.hilt:hilt-navigation-compose:${SdkVersions.HILT_NAV_COMPOSE}")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
