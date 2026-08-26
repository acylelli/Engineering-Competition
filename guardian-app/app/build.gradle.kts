import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val hasGoogleServicesConfig = sequenceOf(
    file("google-services.json"),
    file("src/google-services.json"),
    file("src/debug/google-services.json"),
    file("src/release/google-services.json"),
).any { it.isFile }

if (hasGoogleServicesConfig) {
    pluginManager.apply("com.google.gms.google-services")
} else {
    logger.warn("google-services.json not found; Firebase resource generation is disabled.")
}

val localProperties = Properties().apply {

    val localPropertiesFile =
        rootProject.file(
            "local.properties"
        )

    if (
        localPropertiesFile.exists()
    ) {

        localPropertiesFile
            .inputStream()
            .use(::load)
    }
}

fun buildConfigString(
    value: String
): String =
    "\"${
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }\""


android {

    namespace =
        "com.watchsafety.guardian"

    compileSdk =
        37


    defaultConfig {

        applicationId =
            "com.watchsafety.guardian"

        minSdk =
            26

        targetSdk =
            36

        versionCode =
            1

        versionName =
            "0.1.0"


        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"


        /*
         * =================================================
         * Supabase
         * =================================================
         */

        buildConfigField(

            "String",

            "SUPABASE_URL",

            buildConfigString(
                localProperties
                    .getProperty(
                        "SUPABASE_URL",
                        ""
                    )
            )
        )


        buildConfigField(

            "String",

            "SUPABASE_PUBLISHABLE_KEY",

            buildConfigString(
                localProperties
                    .getProperty(
                        "SUPABASE_PUBLISHABLE_KEY",
                        ""
                    )
            )
        )


        /*
         * =================================================
         * Kakao
         * =================================================
         */

        buildConfigField(

            "String",

            "KAKAO_NATIVE_APP_KEY",

            buildConfigString(
                localProperties
                    .getProperty(
                        "KAKAO_NATIVE_APP_KEY",
                        ""
                    )
            )
        )

        /*
         * =================================================
         * Tmap api
         * =================================================
         */

        buildConfigField(
            "String",
            "TMAP_APP_KEY",
            buildConfigString(
                localProperties.getProperty(
                    "TMAP_APP_KEY",
                    ""
                )
            )
        )
    }


    buildTypes {

        release {

            isMinifyEnabled =
                false


            proguardFiles(

                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),

                "proguard-rules.pro",
            )
        }
    }


    buildFeatures {

        compose =
            true

        buildConfig =
            true
    }


    packaging {

        resources {

            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {


    /*
     * =====================================================
     * Compose
     * =====================================================
     */

    val composeBom =
        platform(
            "androidx.compose:compose-bom:2026.08.00"
        )


    implementation(
        composeBom
    )

    androidTestImplementation(
        composeBom
    )


    implementation(
        "androidx.core:core-ktx:1.19.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.13.0"
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-graphics"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.9.5"
    )


    /*
     * =====================================================
     * Supabase
     * =====================================================
     */

    implementation(
        platform(
            "io.github.jan-tennert.supabase:bom:3.7.0"
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

    implementation(
        "io.ktor:ktor-client-okhttp:3.5.1"
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
     * Kakao Login
     * =====================================================
     */

    implementation(
        "com.kakao.sdk:v2-user:2.24.0"
    )
    /*
 * =====================================================
 * TMAP Vector Map SDK
 * =====================================================
 */

    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf(
                    "*.aar"
                ),
            )
        )
    )


    /*
     * =====================================================
     * Test
     * =====================================================
     */

    testImplementation(
        "junit:junit:4.13.2"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}
