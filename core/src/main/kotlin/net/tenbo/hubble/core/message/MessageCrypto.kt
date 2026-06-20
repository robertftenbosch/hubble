package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.CryptoProvider
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

/**
 * End-to-end AEAD for friend messages. The key is derived from the friendship
 * [rootKey] (established in person during the BLE handshake) via HKDF, so only the
 * two friends can read each other's content. AES-256-GCM with a fresh random 12-byte
 * nonce, which is prepended to the ciphertext.
 */
class MessageCrypto(
    crypto: CryptoProvider,
    rootKey: ByteArray,
    private val random: SecureRandom = SecureRandom(),
) {
    private val key: ByteArray = crypto.hkdf(rootKey, byteArrayOf(), INFO, 32)

    /** Returns nonce(12) || ciphertext||tag. */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = byteArrayOf()): ByteArray {
        val nonce = ByteArray(NONCE_LEN).also { random.nextBytes(it) }
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(true, AEADParameters(KeyParameter(key), TAG_BITS, nonce, associatedData))
        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        val n = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        cipher.doFinal(out, n)
        return nonce + out
    }

    /** Inverse of [encrypt]; throws if authentication fails. */
    fun decrypt(data: ByteArray, associatedData: ByteArray = byteArrayOf()): ByteArray {
        require(data.size > NONCE_LEN) { "ciphertext too short" }
        val nonce = data.copyOfRange(0, NONCE_LEN)
        val body = data.copyOfRange(NONCE_LEN, data.size)
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(false, AEADParameters(KeyParameter(key), TAG_BITS, nonce, associatedData))
        val out = ByteArray(cipher.getOutputSize(body.size))
        val n = cipher.processBytes(body, 0, body.size, out, 0)
        val total = n + cipher.doFinal(out, n)
        return out.copyOfRange(0, total)
    }

    private companion object {
        val INFO = "hubble-msg".toByteArray()
        const val NONCE_LEN = 12
        const val TAG_BITS = 128
    }
}
