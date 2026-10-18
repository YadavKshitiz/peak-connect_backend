import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { load(it) }
    }
}

val razorpayKeyIdProp = localProperties.getProperty("RAZORPAY_KEY_ID")
val razorpayKeyId = if (!razorpayKeyIdProp.isNullOrBlank()) {
    razorpayKeyIdProp
} else {
    logger.warn("WARNING: 'RAZORPAY_KEY_ID' not found in local.properties. Defaulting to empty string.")
    ""
}

val apiBaseUrlProp = localProperties.getProperty("API_BASE_URL")
val apiBaseUrl = if (!apiBaseUrlProp.isNullOrBlank()) {
    apiBaseUrlProp
} else {
    logger.warn("WARNING: 'API_BASE_URL' not found in local.properties. Defaulting to https://peak-connect-backend.onrender.com/.")
    "https://peak-connect-backend.onrender.com/"
}

android {
    namespace = "com.peakconnect.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.peakconnect.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKeyId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GITHUB_ASSETS_BASE_URL", "\"https://raw.githubusercontent.com/YadavKshitiz/peak-connect_backend/main/assets/activity_images/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
        // Pin Razorpay standard-core to resolved version 1.7.20 instead of dynamic 'latest.integration' to ensure reproducible builds
        force("com.razorpay:standard-core:1.7.20")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose BOM & Core UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Networking & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Security Crypto
    implementation(libs.androidx.security.crypto)

    // Coil 3
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // AppCompat & Material Components
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // OSMDroid
    implementation(libs.osmdroid.android)

    // Razorpay Checkout
    implementation(libs.razorpay.checkout)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    // Android Instrumented Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}