package net.tenbo.hubble.desktop

/** Manual verification: round-trip a secret through the OS keychain. */
fun main() {
    println("os.name      = ${System.getProperty("os.name")}")
    println("available()  = ${Keychain.available()}")
    if (!Keychain.available()) {
        println("No OS secret store usable here — Store would fall back to an owner-only file.")
        return
    }

    val secret = "abandon ability able about above absent absorb abstract absurd abuse access accident"
    check(Keychain.store(secret)) { "store() failed" }
    println("stored ✓")

    val back = Keychain.retrieve()
    check(back == secret) { "retrieve() mismatch: got ${back?.take(12)}…" }
    println("retrieved ✓ (matches)")

    Keychain.clear()
    val afterClear = Keychain.retrieve()
    check(afterClear == null) { "clear() failed: still present" }
    println("cleared ✓")

    println("\nPASS — recovery phrase round-trips through the OS keychain.")
}
