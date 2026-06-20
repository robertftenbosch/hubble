package net.tenbo.hubble.core.pair

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PairingTest {

    private val crypto = BouncyCastleCrypto()
    private val phrase =
        "advance behind cargo damage exhaust feature globe horizon insect jewel knock leader"

    @Test
    fun `round-trip ships the recovery phrase from phone to desktop`() {
        val pairing = Pairing(crypto)
        val desktop = pairing.desktopStart()
        val sealed = pairing.phoneSeal(desktop.qrPayload, phrase)
        assertEquals(desktop.mailboxId, sealed.mailboxId, "both sides derive the same mailbox id")
        val opened = pairing.desktopOpen(desktop, sealed.envelope)
        assertEquals(phrase, opened)
    }

    @Test
    fun `desktop with the wrong ephemeral state cannot decrypt`() {
        val pairing = Pairing(crypto)
        val desktopA = pairing.desktopStart()
        val desktopB = pairing.desktopStart()
        val sealed = pairing.phoneSeal(desktopA.qrPayload, phrase)
        assertFailsWith<Exception> { pairing.desktopOpen(desktopB, sealed.envelope) }
    }

    @Test
    fun `envelope with mutated ciphertext fails authentication`() {
        val pairing = Pairing(crypto)
        val desktop = pairing.desktopStart()
        val sealed = pairing.phoneSeal(desktop.qrPayload, phrase)
        val tampered = sealed.envelope.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFailsWith<Exception> { pairing.desktopOpen(desktop, tampered) }
    }

    @Test
    fun `each desktopStart picks a fresh ephemeral keypair`() {
        val pairing = Pairing(crypto)
        val a = pairing.desktopStart()
        val b = pairing.desktopStart()
        assertNotEquals(a.qrPayload, b.qrPayload)
        assertNotEquals(a.mailboxId, b.mailboxId)
    }

    @Test
    fun `mailbox id is deterministic for a given desktop pubkey`() {
        val pairing = Pairing(crypto)
        val desktop = pairing.desktopStart()
        // The phone derives the mailbox id from the QR alone.
        val phoneSide = pairing.mailboxId(PairingQr.decode(desktop.qrPayload))
        assertEquals(desktop.mailboxId, phoneSide)
    }

    @Test
    fun `QR round-trip of the ephemeral pubkey`() {
        val pub = ByteArray(32) { it.toByte() }
        val encoded = PairingQr.encode(pub)
        assertTrue(encoded.startsWith("hubble-pair:1:"))
        val decoded = PairingQr.decode(encoded)
        assertTrue(pub.contentEquals(decoded))
    }

    @Test
    fun `malformed QR is rejected`() {
        assertFailsWith<IllegalArgumentException> { PairingQr.decode("https://evil.example/qr") }
        assertFailsWith<IllegalArgumentException> { PairingQr.decode("hubble-pair:1:too-short") }
    }
}
