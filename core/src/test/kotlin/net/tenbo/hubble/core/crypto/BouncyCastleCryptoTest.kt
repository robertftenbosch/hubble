package net.tenbo.hubble.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BouncyCastleCryptoTest {
    private val crypto = BouncyCastleCrypto()

    @Test fun `x25519 ecdh yields the same shared secret for both parties`() {
        val a = crypto.generateX25519(ByteArray(32) { 1 })
        val b = crypto.generateX25519(ByteArray(32) { 2 })
        val ab = crypto.x25519SharedSecret(a.privateKey, b.publicKey)
        val ba = crypto.x25519SharedSecret(b.privateKey, a.publicKey)
        assertContentEquals(ab, ba)
    }

    @Test fun `ed25519 sign and verify round-trips`() {
        val kp = crypto.generateEd25519(ByteArray(32) { 7 })
        val msg = "hello hubble".toByteArray()
        val sig = crypto.sign(kp.privateKey, msg)
        assertTrue(crypto.verify(kp.publicKey, msg, sig))
        assertFalse(crypto.verify(kp.publicKey, "tampered".toByteArray(), sig))
    }

    @Test fun `hkdf is deterministic and length-correct`() {
        val ikm = ByteArray(32) { 9 }
        val k1 = crypto.hkdf(ikm, salt = byteArrayOf(), info = "root".toByteArray(), length = 32)
        val k2 = crypto.hkdf(ikm, salt = byteArrayOf(), info = "root".toByteArray(), length = 32)
        assertContentEquals(k1, k2)
        assertEquals(32, k1.size)
    }
}
