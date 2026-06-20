package net.tenbo.hubble.desktop

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Properties

/**
 * Tiny local store under ~/.hubble. The recovery phrase is kept in the OS secret store
 * ([Keychain]: macOS Keychain / libsecret / Windows DPAPI) whenever one is available;
 * only if none is usable does it fall back to an owner-only file (0600, dir 0700). An
 * existing plaintext file is migrated into the keychain on first read, then deleted.
 */
object Store {
    private val dir = File(System.getProperty("user.home"), ".hubble").apply {
        mkdirs()
        lockDown(this, "rwx------")
    }
    private val phraseFile = File(dir, "phrase")

    private val settingsFile = File(dir, "settings")

    fun loadPhrase(): String? {
        Keychain.retrieve()?.let { return it }
        // No keychain entry. If a legacy plaintext file exists, migrate it into the keychain.
        val fromFile = phraseFile.takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null } ?: return null
        if (Keychain.store(fromFile)) phraseFile.delete() // promoted to the OS secret store
        return fromFile
    }

    // Settings live in a tiny key=value file.
    private fun props(): Properties = Properties().apply {
        if (settingsFile.exists()) settingsFile.inputStream().use { load(it) }
    }
    private fun saveProps(p: Properties) {
        settingsFile.outputStream().use { p.store(it, "hubble") }
        lockDown(settingsFile, "rw-------")
    }

    fun loadSoundEnabled(): Boolean = props().getProperty("sound", "on") != "off"
    fun saveSoundEnabled(on: Boolean) = saveProps(props().apply { setProperty("sound", if (on) "on" else "off") })

    fun loadPresence(): String = props().getProperty("presence", "ONLINE")
    fun savePresence(name: String) = saveProps(props().apply { setProperty("presence", name) })

    fun savePhrase(phrase: String) {
        val p = phrase.trim()
        // Prefer the OS secret store; never leave a plaintext file behind if it succeeds.
        if (Keychain.store(p)) { phraseFile.delete(); return }
        // Fallback: owner-only file (create it locked-down *before* writing the secret).
        if (!phraseFile.exists()) {
            phraseFile.createNewFile()
            lockDown(phraseFile, "rw-------")
        }
        phraseFile.writeText(p)
        lockDown(phraseFile, "rw-------")
    }

    /** Where the phrase currently lives — for the UI to reassure the user. */
    fun phraseLocation(): String = if (Keychain.available()) "your system keychain" else "an owner-only file"

    fun clear() { Keychain.clear(); phraseFile.delete() }

    private fun lockDown(f: File, perms: String) {
        runCatching { Files.setPosixFilePermissions(f.toPath(), PosixFilePermissions.fromString(perms)) }
            .recoverCatching {
                // Non-POSIX (e.g. Windows): fall back to owner-only via java.io.File flags.
                f.setReadable(false, false); f.setReadable(true, true)
                f.setWritable(false, false); f.setWritable(true, true)
            }
    }
}
