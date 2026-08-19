import java.util.Properties

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

val watchSafetyLocalProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

watchSafetyLocalProperties.getProperty("WATCH_SAFETY_BUILD_DIR")
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { configuredPath ->
        val externalBuildRoot = rootProject.file(configuredPath)
        layout.buildDirectory.set(externalBuildRoot.resolve("root"))
        subprojects {
            layout.buildDirectory.set(externalBuildRoot.resolve(name))
        }
    }
