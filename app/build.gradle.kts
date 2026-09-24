import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.screenshot)
}

// Release signing config is read from keystore.properties (git-ignored) if present.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.stillhere.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stillhere.app"
        minSdk = 26
        targetSdk = 36
        // A new identity starts a new line. The previous app was
        // com.ttldownloader.app at versionCode 5 / 1.4; to Android this is a
        // different app entirely, with no upgrade path and no shared data.
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "engine"
    productFlavors {
        // Today's app: the backend does the work. No suffix, so installs keep upgrading.
        create("server") {
            dimension = "engine"
        }
        // Does the work on the phone; installs beside the server app.
        create("local") {
            dimension = "engine"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
        }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Sign with the release key when keystore.properties is present.
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // Both this and the matching flag in gradle.properties are required by the
    // screenshot plugin — it checks each one separately.
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    packaging {
        // youtubedl-android runs Python and ffmpeg as executables unpacked from
        // the APK's native libraries, which only works when they are extracted
        // at install. The server flavor has no native code, so this costs it nothing.
        jniLibs { useLegacyPackaging = true }
    }

    splits {
        abi {
            // Splits cannot be set per flavor, and the server flavor has no
            // native code to split. So split only when a Local build was asked for.
            // A task name without "Local" (a bare `assemble` or `build`) packages
            // Local unsplit, so always build Local with its own task
            // (assembleLocalDebug, assembleLocalRelease).
            isEnable = gradle.startParameter.taskNames.any { it.contains("Local") }
            reset()
            // x86_64 is for the emulator; phones want arm64-v8a.
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Python, yt-dlp and ffmpeg as native libraries. Local only: the server
    // flavor must not carry tens of MB it never runs.
    "localImplementation"(libs.youtubedl.android.library)
    "localImplementation"(libs.youtubedl.android.ffmpeg)

    testImplementation(libs.junit)
    "testLocalImplementation"(libs.okhttp.mockwebserver)
    screenshotTestImplementation(libs.androidx.ui.tooling)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
