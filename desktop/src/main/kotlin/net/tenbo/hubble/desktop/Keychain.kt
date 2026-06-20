package net.tenbo.hubble.desktop

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Stores the recovery phrase in the operating system's own secret store, so it is never
 * left as plaintext on disk:
 *
 *  - **macOS**   → Keychain, via the `security` CLI.
 *  - **Linux**   → Secret Service (GNOME Keyring / KWallet), via the `secret-tool` CLI.
 *  - **Windows** → DPAPI (per-user encryption), via PowerShell; ciphertext lives in a file
 *                  that only this Windows account can decrypt.
 *
 * Every backend shells out to a tool already present on a normal desktop, so the app needs
 * no native libraries. When no backend is usable (CLI missing, no keyring daemon, headless),
 * [store]/[retrieve] return false/null and the caller falls back to an owner-only file.
 */
object Keychain {
    private const val SERVICE = "hubble"
    private const val ACCOUNT = "recovery-phrase"
    private const val MAC_LABEL = "Hubble recovery phrase"

    private enum class Os { MAC, LINUX, WINDOWS, OTHER }

    private val os: Os = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("mac") || it.contains("darwin") -> Os.MAC
            it.contains("win") -> Os.WINDOWS
            it.contains("nux") || it.contains("nix") -> Os.LINUX
            else -> Os.OTHER
        }
    }

    private val dpapiFile by lazy { File(File(System.getProperty("user.home"), ".hubble"), "phrase.dpapi") }

    /** Whether an OS secret store looks usable here (best-effort; [store] is the real test). */
    fun available(): Boolean = when (os) {
        Os.MAC -> hasCommand("security")
        Os.LINUX -> hasCommand("secret-tool")
        Os.WINDOWS -> hasCommand("powershell")
        Os.OTHER -> false
    }

    /** Persist [phrase] in the OS secret store. Returns true on success. */
    fun store(phrase: String): Boolean = when (os) {
        Os.MAC -> run(
            listOf("security", "add-generic-password", "-a", ACCOUNT, "-s", SERVICE, "-U", "-w", phrase),
        ).ok
        Os.LINUX -> run(
            listOf("secret-tool", "store", "--label=$MAC_LABEL", "service", SERVICE, "account", ACCOUNT),
            stdin = phrase,
        ).ok
        Os.WINDOWS -> {
            dpapiFile.parentFile?.mkdirs()
            run(powershell(STORE_PS), stdin = phrase).ok
        }
        Os.OTHER -> false
    }

    /** Read the phrase back, or null if absent / no backend. */
    fun retrieve(): String? = when (os) {
        Os.MAC -> run(listOf("security", "find-generic-password", "-s", SERVICE, "-w"))
            .takeIf { it.ok }?.out?.trim()?.ifBlank { null }
        Os.LINUX -> run(listOf("secret-tool", "lookup", "service", SERVICE, "account", ACCOUNT))
            .takeIf { it.ok }?.out?.trim()?.ifBlank { null }
        Os.WINDOWS -> if (!dpapiFile.exists()) null else
            run(powershell(RETRIEVE_PS)).takeIf { it.ok }?.out?.trim()?.ifBlank { null }
        Os.OTHER -> null
    }

    /** Remove the stored phrase. Safe to call when nothing is stored. */
    fun clear() {
        when (os) {
            Os.MAC -> run(listOf("security", "delete-generic-password", "-s", SERVICE))
            Os.LINUX -> run(listOf("secret-tool", "clear", "service", SERVICE, "account", ACCOUNT))
            Os.WINDOWS -> dpapiFile.delete()
            Os.OTHER -> Unit
        }
    }

    // PowerShell snippets (Windows). The phrase arrives on stdin; ciphertext is DPAPI-bound
    // to the current user and base64-encoded into phrase.dpapi.
    private val STORE_PS = """
        Add-Type -AssemblyName System.Security
        ${'$'}s = [Console]::In.ReadToEnd()
        ${'$'}b = [Text.Encoding]::UTF8.GetBytes(${'$'}s)
        ${'$'}e = [Security.Cryptography.ProtectedData]::Protect(${'$'}b, ${'$'}null, 'CurrentUser')
        [IO.File]::WriteAllText('${dpapiWinPath()}', [Convert]::ToBase64String(${'$'}e))
    """.trimIndent()

    private val RETRIEVE_PS = """
        Add-Type -AssemblyName System.Security
        ${'$'}e = [Convert]::FromBase64String([IO.File]::ReadAllText('${dpapiWinPath()}'))
        ${'$'}b = [Security.Cryptography.ProtectedData]::Unprotect(${'$'}e, ${'$'}null, 'CurrentUser')
        [Console]::Out.Write([Text.Encoding]::UTF8.GetString(${'$'}b))
    """.trimIndent()

    private fun dpapiWinPath() = dpapiFile.absolutePath.replace("'", "''")

    private fun powershell(script: String) =
        listOf("powershell", "-NoProfile", "-NonInteractive", "-Command", script)

    private data class Result(val ok: Boolean, val out: String)

    private fun run(command: List<String>, stdin: String? = null): Result = runCatching {
        val p = ProcessBuilder(command).redirectErrorStream(false).start()
        if (stdin != null) p.outputStream.use { it.write(stdin.toByteArray()); it.flush() } else p.outputStream.close()
        val out = p.inputStream.bufferedReader().readText()
        if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); return@runCatching Result(false, "") }
        Result(p.exitValue() == 0, out)
    }.getOrElse { Result(false, "") }

    private fun hasCommand(cmd: String): Boolean = runCatching {
        val which = if (os == Os.WINDOWS) listOf("where", cmd) else listOf("sh", "-c", "command -v $cmd")
        val p = ProcessBuilder(which).start()
        p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)
}
