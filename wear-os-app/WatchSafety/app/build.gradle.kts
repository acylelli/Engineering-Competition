import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // 프로젝트 Kotlin 버전과 동일하게 맞춤
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}


/*
 * ---------------------------------------------------------
 * local.properties
 * ---------------------------------------------------------
 */

val localProperties: Properties = Properties().apply {

    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use { stream ->
            load(stream)
        }
    }
}


/*
 * ---------------------------------------------------------
 * API Key / URL
 * ---------------------------------------------------------
 */

val tmapAppKey: String =
    localProperties.getProperty(
        "TMAP_APP_KEY",
        ""
    )

val supabaseUrl: String =
    localProperties.getProperty(
        "SUPABASE_URL",
        ""
    )

val supabasePublishableKey: String =
    localProperties.getProperty(
        "SUPABASE_PUBLISHABLE_KEY",
        ""
    )


android {

    namespace = "com.example.watchsafety"

    compileSdk = 36

    defaultConfig {

        applicationId = "com.example.watchsafety"

        minSdk = 30
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "TMAP_APP_KEY",
            "\"$tmapAppKey\""
        )

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"$supabasePublishableKey\""
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
    // Icons
    // ----------------------------------------

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    // ----------------------------------------
    // Supabase
    // ----------------------------------------

    implementation(
        platform(
            "io.github.jan-tennert.supabase:bom:3.2.6"
        )
    )

    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    implementation(
        "io.github.jan-tennert.supabase:postgrest-kt"
    )


    // Supabase HTTP 통신용 Ktor Engine
    implementation(
        "io.ktor:ktor-client-okhttp:3.3.1"
    )


    // ----------------------------------------
    // Debug
    // ----------------------------------------

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}