// Android application module. Requires the Android SDK (ANDROID_HOME) to build.
// Add `include(":app")` to settings.gradle.kts once the SDK is available.
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "net.tenbo.hubble.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.tenbo.hubble"
        minSdk = 26 // BLE advertise/peripheral reliable from API 26+
        targetSdk = 35
        // Auto-derived from git in root build.gradle.kts.
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionSemver"] as String
        // Where the app posts envelopes / pulls matches. Defaults to the adb-reverse
        // loopback so on-device dev "just works"; override at build time for a LAN
        // test box or production host:
        //   ./gradlew :app:assembleDebug -PhubbleServer=http://192.168.2.8:4000
        //   ./gradlew :app:assembleRelease -PhubbleServer=https://hubble.tenbo.app
        val hubbleServer = (project.findProperty("hubbleServer") as String?)
            ?: "http://127.0.0.1:4000"
        val hubbleServerEscaped = hubbleServer.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "HUBBLE_SERVER", "\"$hubbleServerEscaped\"")
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("net.zetetic:sqlcipher-android:4.6.1") // encrypted Room DB
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Keystore-wrapped prefs

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Direct P2P data channel + signaling client
    implementation("io.getstream:stream-webrtc-android:1.3.9")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Open-source map (OpenStreetMap tiles, no API key)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Background polling for notifications (no FCM / Google push)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // QR scanner — for pairing the desktop client with this phone's identity.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
