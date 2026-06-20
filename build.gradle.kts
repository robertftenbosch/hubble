// Plugin versions are declared once here (apply false) so every module shares one
// Kotlin/AGP version on the classpath; modules apply them without a version.
plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
}
