import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? = keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { it.name.contains("Release") }
    if (releaseRequested) {
        val required = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        val missing = required.filter { signingProperty(it) == null }
        if (missing.isNotEmpty()) {
            throw GradleException("Release signing requires local keystore.properties with: ${missing.joinToString(", ")}")
        }
    }
}

android {
    namespace = "com.paycheckpilot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.paycheckpilot"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.6-release-signed"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug")
        create("release") {
            signingProperty("storeFile")?.let { storeFile = file(it) }
            storePassword = signingProperty("storePassword")
            keyAlias = signingProperty("keyAlias")
            keyPassword = signingProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)

    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
