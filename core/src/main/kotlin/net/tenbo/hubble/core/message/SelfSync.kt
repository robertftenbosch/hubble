package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.CryptoProvider
import net.tenbo.hubble.core.identity.Identity

/**
 * Lets a person's own devices share private data (e.g. their match list) through the
 * relay without the server learning anything. Both the encryption key and the mailbox
 * id derive from the identity, so any device that restores the same recovery phrase can
 * pull and decrypt — and nobody else can. Used to bring matches (made on the phone over
 * BLE) onto the desktop companion.
 */
class SelfSync(crypto: CryptoProvider, private val identity: Identity) {
    private val mailbox = Mailbox(crypto)
    private val key = crypto.hkdf(identity.signingPrivateKey, byteArrayOf(), INFO, 32)
    private val cipher = MessageCrypto(crypto, key)

    /** The mailbox this identity's devices read/write for self-sync (distinct from chat). */
    val mailboxId: String get() = mailbox.id(SELF_LABEL + identity.signingPublicKey)

    fun seal(payload: ByteArray): ByteArray = cipher.encrypt(payload)

    fun open(bytes: ByteArray): ByteArray = cipher.decrypt(bytes)

    private companion object {
        val INFO = "hubble-self-sync".toByteArray()
        val SELF_LABEL = "self:".toByteArray()
    }
}
