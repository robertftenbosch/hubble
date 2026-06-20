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

// One version, derived from git. The /VERSION file holds the SemVer base
// ("0.1.0-alpha"); the alpha counter is the total commit count, the build id is
// the short SHA. Bump the base in /VERSION when you cut a real minor/major.
//
//   versionSemver  "0.1.0-alpha.62+77bcd08"   display, log, /health
//   versionShort   "0.1.0-alpha.62"           SemVer-strict (server/Hex, Kotlin libs)
//   versionNumeric "0.1.0.62"                 installer 4-part numeric (MSI/DEB)
//   versionCode    62                         Android versionCode (monotonic int)
//
// MSI auto-upgrade falls out for free: the numeric always strictly increases per
// commit, so Windows Installer treats a new install as an upgrade.
val versionBase = file("VERSION").readText().trim()
val versionCount = runCatching {
    val p = ProcessBuilder("git", "rev-list", "--count", "HEAD").directory(rootDir).start()
    p.inputStream.bufferedReader().readText().trim().toInt().also { p.waitFor() }
}.getOrDefault(0)
val versionHash = runCatching {
    val p = ProcessBuilder("git", "rev-parse", "--short", "HEAD").directory(rootDir).start()
    p.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }.also { p.waitFor() }
}.getOrDefault("unknown")
val versionXYZ = versionBase.substringBefore("-")

// Installer formats (MSI, DEB) require strict 3-part MAJOR.MINOR.PATCH. We collapse
// the SemVer-style "X.Y.Z-alpha.N" down to "X.Y.N" so the strictly increasing N
// gives Windows Installer auto-upgrade for free. When the base in /VERSION switches
// from a pre-release like "0.1.0-alpha" to a final "1.0.0", drop into a per-release
// build where N isn't appended.
private val versionMajorMinor = versionXYZ.split(".").take(2).joinToString(".")
extra["versionSemver"] = "$versionBase.$versionCount+$versionHash"
extra["versionShort"] = "$versionBase.$versionCount"
extra["versionNumeric"] = "$versionMajorMinor.$versionCount"
extra["versionCode"] = versionCount
