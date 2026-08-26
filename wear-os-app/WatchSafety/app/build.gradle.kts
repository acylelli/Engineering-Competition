import java.util.Properties

plugins {

    alias(
        libs.plugins.android.application
    )

    alias(
        libs.plugins.kotlin.compose
    )

    id(
        "org.jetbrains.kotlin.plugin.serialization"
    ) version "2.2.10"
}


/*
 * =========================================================
 * Firebase google-services.json 확인
 * =========================================================
 */
val hasGoogleServicesConfig =

    sequenceOf(

        file(
            "google-services.json"
        ),

        file(
            "src/google-services.json"
        ),

        file(
            "src/debug/google-services.json"
        ),

        file(
            "src/release/google-services.json"
        ),
    )
        .any {

            it.isFile
        }


if (
    hasGoogleServicesConfig
) {

    pluginManager.apply(
        "com.google.gms.google-services"
    )

} else {

    logger.warn(
        "google-services.json not found; Firebase resource generation is disabled."
    )
}


/*
 * =========================================================
 * local.properties
 * =========================================================
 */
val localProperties: Properties =

    Properties()
        .apply {

            val file =
                rootProject.file(
                    "local.properties"
                )


            if (
                file.exists()
            ) {

                file
                    .inputStream()
                    .use { stream ->

                        load(
                            stream
                        )
                    }
            }
        }


/*
 * =========================================================
 * API KEY / 서버 설정
 * =========================================================
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

    namespace =
        "com.example.watchsafety"


    compileSdk =
        36


    defaultConfig {

        applicationId =
            "com.example.watchsafety"


        minSdk =
            30


        targetSdk =
            35


        versionCode =
            1


        versionName =
            "1.0"


        /*
         * =================================================
         * BuildConfig
         * =================================================
         */

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

        compose =
            true


        buildConfig =
            true
    }
}


dependencies {

    /*
     * =====================================================
     * TMAP Android SDK
     *
     * app/libs 안의:
     *
     * tmap-sdk-3.7.aar
     * vsm-tmap-sdk-v2-eaa-2.0.14.aar
     *
     * 두 파일을 로딩한다.
     * =====================================================
     */
    implementation(
        files(
            "libs/tmap-sdk-3.7.aar",
            "libs/vsm-tmap-sdk-v2-eaa-2.0.14.aar"
        )
    )


    /*
     * =====================================================
     * Material Components
     *
     * TMAP SDK v3.7 내부 Theme.TMapLib가
     *
     * Theme.MaterialComponents.DayNight.DarkActionBar
     *
     * 를 부모 Theme으로 사용한다.
     *
     * 아래 dependency가 없으면:
     *
     * colorPrimary
     * colorPrimaryVariant
     * colorOnPrimary
     * colorSecondary
     * colorSecondaryVariant
     * colorOnSecondary
     *
     * 관련 Android resource linking 오류가 발생한다.
     * =====================================================
     */
    implementation(
        "com.google.android.material:material:1.14.0"
    )


    /*
     * =====================================================
     * Android Core
     * =====================================================
     */
    implementation(
        "androidx.core:core-ktx:1.17.0"
    )


    /*
     * =====================================================
     * Jetpack Compose
     * =====================================================
     */
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


    /*
     * =====================================================
     * Wear OS Compose
     * =====================================================
     */
    implementation(
        "androidx.wear.compose:compose-material:1.5.0"
    )


    implementation(
        "androidx.wear.compose:compose-foundation:1.5.0"
    )


    implementation(
        "androidx.wear.compose:compose-ui-tooling:1.5.0"
    )


    /*
     * =====================================================
     * Splash Screen
     * =====================================================
     */
    implementation(
        "androidx.core:core-splashscreen:1.2.0"
    )


    /*
     * =====================================================
     * Google Play Services Location
     * =====================================================
     */
    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )


    /*
     * =====================================================
     * Wear OS Health Services
     * =====================================================
     */
    implementation(
        "androidx.health:health-services-client:1.1.0-rc02"
    )


    /*
     * =====================================================
     * Coroutines
     * =====================================================
     */
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2"
    )


    /*
     * =====================================================
     * Material Icons
     *
     * ArrowUpward
     * Map
     * MyLocation
     * Close
     * =====================================================
     */
    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    /*
     * =====================================================
     * Supabase
     * =====================================================
     */
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


    implementation(
        "io.github.jan-tennert.supabase:realtime-kt"
    )


    /*
     * =====================================================
     * Ktor
     * =====================================================
     */
    implementation(
        "io.ktor:ktor-client-okhttp:3.3.1"
    )


    /*
     * =====================================================
     * Firebase
     * =====================================================
     */
    implementation(

        platform(
            "com.google.firebase:firebase-bom:34.17.0"
        )
    )


    implementation(
        "com.google.firebase:firebase-messaging"
    )


    /*
     * =====================================================
     * Debug
     * =====================================================
     */
    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )


    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}
