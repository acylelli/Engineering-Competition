import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {

    val localPropertiesFile =
        rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {

        localPropertiesFile
            .inputStream()
            .use {
                load(it)
            }
    }
}

val tmapAppKey =
    localProperties.getProperty(
        "TMAP_APP_KEY",
        ""
    )

android {

    namespace = "com.example.watchsafety"

    compileSdk = 36

    defaultConfig {

        applicationId =
            "com.example.watchsafety"

        minSdk = 30

        // Galaxy Watch 4 테스트 기준
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "TMAP_APP_KEY",
            "\"$tmapAppKey\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // ----------------------------------------
    // Android 기본
    // ----------------------------------------

    implementation(
        "androidx.core:core-ktx:1.17.0"
    )

    // ----------------------------------------
    // Compose
    // ----------------------------------------

    implementation(
        platform(
            "androidx.compose:compose-bom:2025.08.01"
        )
    )

    implementation(
        "androidx.compose.runtime:runtime"
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    // setContent()
    // rememberLauncherForActivityResult()
    implementation(
        "androidx.activity:activity-compose:1.11.0"
    )

    // ----------------------------------------
    // Wear OS Compose
    // ----------------------------------------

    implementation(
        "androidx.wear.compose:compose-material:1.5.0"
    )

    implementation(
        "androidx.wear.compose:compose-foundation:1.5.0"
    )

    // ----------------------------------------
    // Splash Screen
    // ----------------------------------------

    implementation(
        "androidx.core:core-splashscreen:1.2.0"
    )

    // ----------------------------------------
    // GPS
    // ----------------------------------------

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    // ----------------------------------------
    // Health Services
    // ----------------------------------------

    implementation(
        "androidx.health:health-services-client:1.1.0-rc02"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2"
    )

    // ----------------------------------------
    // Compose Debug
    // ----------------------------------------

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )

    // ----------------------------------------
    // Compose Icon
    // ----------------------------------------

    implementation(
        "androidx.compose.material:material-icons-extended"
    )
}