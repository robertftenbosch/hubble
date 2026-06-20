package net.tenbo.hubble.app.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.identity.IdentityFactory
import java.security.SecureRandom

/**
 * Stores the recovery phrase encrypted under an Android Keystore master key.
 * The [Identity] is re-derived from the phrase on load, so the phrase is the
 * single source of truth (matches the design: the recovery phrase IS the backup).
 */
class KeystoreVault(context: Context) {
    private val factory = IdentityFactory(BouncyCastleCrypto())

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "hubble-vault",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun hasIdentity(): Boolean = prefs.contains(KEY_PHRASE)

    fun savePhrase(phrase: String) {
        prefs.edit().putString(KEY_PHRASE, phrase).putString(KEY_NAME, prefs.getString(KEY_NAME, "")).apply()
    }

    fun saveDisplayName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun displayName(): String = prefs.getString(KEY_NAME, "") ?: ""

    // ── Dating profile ─────────────────────────────────────────────────────────
    fun hasProfile(): Boolean = displayName().isNotBlank()

    fun saveProfile(p: Profile) {
        prefs.edit()
            .putString(KEY_NAME, p.name)
            .putInt(KEY_AGE, p.age)
            .putString(KEY_CITY, p.city)
            .putString(KEY_BIO, p.bio)
            .putString(KEY_PROMPT_A, p.promptAnswer)
            .putString(KEY_PHOTO, p.photoPath)
            .apply()
    }

    fun loadProfile(): Profile = Profile(
        name = displayName(),
        age = prefs.getInt(KEY_AGE, 0),
        city = prefs.getString(KEY_CITY, "") ?: "",
        bio = prefs.getString(KEY_BIO, "") ?: "",
        promptAnswer = prefs.getString(KEY_PROMPT_A, "") ?: "",
        photoPath = prefs.getString(KEY_PHOTO, null),
    )

    fun loadPhrase(): String? = prefs.getString(KEY_PHRASE, null)

    fun loadIdentity(): Identity? = loadPhrase()?.let { factory.fromPhrase(it) }

    /**
     * 32-byte SQLCipher key for the local database. Generated once with a CSPRNG and
     * persisted inside the Keystore-wrapped encrypted prefs, so it is available from
     * first launch (independent of whether an identity exists yet) and never leaves
     * the Keystore-protected store in plaintext.
     */
    fun databaseKey(): ByteArray {
        prefs.getString(KEY_DB, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DB, Base64.encodeToString(key, Base64.NO_WRAP)).apply()
        return key
    }

    private companion object {
        const val KEY_PHRASE = "phrase"
        const val KEY_NAME = "display_name"
        const val KEY_DB = "db_key"
        const val KEY_AGE = "age"
        const val KEY_CITY = "city"
        const val KEY_BIO = "bio"
        const val KEY_PROMPT_A = "prompt_answer"
        const val KEY_PHOTO = "photo_path"
    }
}

/** A person's dating profile. The prompt question is fixed for the MVP. */
data class Profile(
    val name: String,
    val age: Int,
    val city: String,
    val bio: String,
    val promptAnswer: String,
    val photoPath: String?,
) {
    companion object {
        const val PROMPT_QUESTION = "Where I'm usually found"
    }
}
