import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("org.json:json:20240303")
    // QR rendering for "pair from phone" — desktop shows, phone scans.
    implementation("com.google.zxing:core:3.5.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// Round-trips the OS keychain (store → retrieve → clear) so the integration can be verified
// on a real machine without driving the GUI.  ./gradlew :desktop:keychainCheck
tasks.register<JavaExec>("keychainCheck") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.tenbo.hubble.desktop.KeychainCheckKt")
}

compose.desktop {
    application {
        mainClass = "net.tenbo.hubble.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Hubble"
            packageVersion = "1.0.0"
            // jlink strips everything not explicitly included; this list comes from
            // `./gradlew :desktop:suggestRuntimeModules`. Without java.net.http the app
            // crashes immediately ("Failed to launch JVM" → NoClassDefFoundError in
            // DesktopApi).
            modules("java.instrument", "java.naming", "java.net.http", "java.sql", "jdk.unsupported")
        }
    }
}
