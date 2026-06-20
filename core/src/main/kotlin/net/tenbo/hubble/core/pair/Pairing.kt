package net.tenbo.hubble.core.pair

import net.tenbo.hubble.core.crypto.CryptoProvider
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import java.util.Base64

/**
 * Device pairing — ship the recovery phrase from a phone to a freshly installed desktop
 * via a one-shot ephemeral Diffie-Hellman handshake brokered by an opaque relay mailbox.
 *
 * The desktop displays a QR holding only its ephemeral X25519 public key. The phone scans
 * it, mints its own ephemeral keypair, derives a shared key via ECDH, encrypts the phrase
 * with AES-256-GCM (authenticated against both pubkeys via AAD), and deposits the envelope
 * in a mailbox id derived from the desktop's pubkey. The desktop polls that mailbox, runs
 * the inverse, and gets the phrase. The mailbox id is *not* in the QR; both sides compute
 * it deterministically, which keeps the QR small.
 *
 * The recovery phrase never appears on screen as plaintext — a screen recording or
 * shoulder-camera sees only an ephemeral pubkey that is useless after the pairing. The
 * threat surface is an active MITM swapping the QR; that is the same threat WhatsApp Web
 * accepts, and can later be tightened with an SAS confirmation reusing the BLE handshake's
 * emoji vocabulary. The mailbox should be short-lived (~60s); the relay's existing TTL
 * sweep is sufficient.
 */
class Pairing(private val crypto: CryptoProvider) {

    /** Deterministic mailbox id so the QR only needs to carry the desktop's pubkey. */
    fun mailboxId(desktopEphPub: ByteArray): String {
        val raw = crypto.hkdf(desktopEphPub, byteArrayOf(), MAILBOX_INFO, 16)
        return URLSAFE.encodeToString(raw)
    }

    /** Desktop step 1: mint an ephemeral keypair, return state to keep + the QR payload. */
    fun desktopStart(seed: ByteArray = randomSeed()): DesktopPairingState {
        val eph = crypto.generateX25519(seed)
        val qr = PairingQr.encode(eph.publicKey)
        return DesktopPairingState(
            ephPub = eph.publicKey,
            ephPriv = eph.privateKey,
            mailboxId = mailboxId(eph.publicKey),
            qrPayload = qr,
        )
    }

    /**
     * Phone step: decode the QR, mint our own ephemeral keypair, ECDH+HKDF a key, encrypt
     * the recovery phrase. Returns the envelope to POST and the mailbox id to POST it to.
     */
    fun phoneSeal(
        qrPayload: String,
        recoveryPhrase: String,
        seed: ByteArray = randomSeed(),
    ): PhoneSealed {
        val deskPub = PairingQr.decode(qrPayload)
        val phoneEph = crypto.generateX25519(seed)
        val key = deriveKey(crypto.x25519SharedSecret(phoneEph.privateKey, deskPub))
        val nonce = ByteArray(NONCE_LEN).also { random.nextBytes(it) }
        val aad = deskPub + phoneEph.publicKey
        val ct = aesGcm(true, key, nonce, aad, recoveryPhrase.toByteArray(Charsets.UTF_8))
        // envelope = phoneEphPub (32) || nonce (12) || ciphertext||tag
        return PhoneSealed(
            mailboxId = mailboxId(deskPub),
            envelope = phoneEph.publicKey + nonce + ct,
        )
    }

    /** Desktop step 3: take the envelope, derive the same key, decrypt the phrase. */
    fun desktopOpen(state: DesktopPairingState, envelope: ByteArray): String {
        require(envelope.size >= PUBKEY_LEN + NONCE_LEN + TAG_LEN) { "envelope too short" }
        val phonePub = envelope.copyOfRange(0, PUBKEY_LEN)
        val nonce = envelope.copyOfRange(PUBKEY_LEN, PUBKEY_LEN + NONCE_LEN)
        val ct = envelope.copyOfRange(PUBKEY_LEN + NONCE_LEN, envelope.size)
        val key = deriveKey(crypto.x25519SharedSecret(state.ephPriv, phonePub))
        val aad = state.ephPub + phonePub
        return String(aesGcm(false, key, nonce, aad, ct), Charsets.UTF_8)
    }

    private fun deriveKey(sharedSecret: ByteArray): ByteArray =
        crypto.hkdf(sharedSecret, byteArrayOf(), ENCRYPT_INFO, 32)

    private fun aesGcm(
        encrypt: Boolean,
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        input: ByteArray,
    ): ByteArray {
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(encrypt, AEADParameters(KeyParameter(key), TAG_BITS, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(input.size))
        val n = cipher.processBytes(input, 0, input.size, out, 0)
        val total = n + cipher.doFinal(out, n)
        return out.copyOfRange(0, total)
    }

    private fun randomSeed(): ByteArray = ByteArray(32).also { random.nextBytes(it) }

    private val random = SecureRandom()

    private companion object {
        val MAILBOX_INFO = "hubble-pair-mailbox-v1".toByteArray()
        val ENCRYPT_INFO = "hubble-pair-encrypt-v1".toByteArray()
        const val PUBKEY_LEN = 32
        const val NONCE_LEN = 12
        const val TAG_LEN = 16
        const val TAG_BITS = 128
        val URLSAFE: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    }
}

/** Ephemeral state the desktop has to keep alive between showing the QR and decrypting. */
data class DesktopPairingState(
    val ephPub: ByteArray,
    val ephPriv: ByteArray,
    val mailboxId: String,
    val qrPayload: String,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/** What the phone produces after scanning + sealing. Phone POSTs envelope to mailboxId. */
data class PhoneSealed(val mailboxId: String, val envelope: ByteArray)

/** QR payload: `hubble-pair:1:<base64url(32-byte X25519 pubkey)>`. */
object PairingQr {
    private const val PREFIX = "hubble-pair:1:"
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun encode(ephPub: ByteArray): String {
        require(ephPub.size == 32) { "X25519 pubkey must be 32 bytes" }
        return PREFIX + enc.encodeToString(ephPub)
    }

    fun decode(s: String): ByteArray {
        require(s.startsWith(PREFIX)) { "not a hubble pairing QR (prefix mismatch)" }
        val pub = dec.decode(s.removePrefix(PREFIX))
        require(pub.size == 32) { "expected 32-byte X25519 pubkey, got ${pub.size}" }
        return pub
    }
}
