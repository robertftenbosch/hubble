package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.identity.IdentityFactory
import net.tenbo.hubble.core.identity.Mnemonic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelfSyncTest {
    private val crypto = BouncyCastleCrypto()
    private val factory = IdentityFactory(crypto)
    private fun identity(seed: Byte) = factory.fromPhrase(Mnemonic.generate(ByteArray(16) { seed }))

    @Test fun `another device with the same phrase recovers the synced matches`() {
        val phrase = Mnemonic.generate(ByteArray(16) { 4 })
        val phone = factory.fromPhrase(phrase)
        val desktop = factory.fromPhrase(phrase) // same recovery phrase, different device

        val matches = listOf(
            MatchSnapshot("h1", "Mara", 27, "Amsterdam", "Café Léon", ByteArray(32) { 1 }, ByteArray(32) { 2 }, ByteArray(32) { 3 }, 10),
        )
        val blob = SelfSync(crypto, phone).seal(MatchSnapshotCodec.encodeList(matches))

        // The desktop computes the same self-mailbox id and decrypts.
        assertEquals(SelfSync(crypto, phone).mailboxId, SelfSync(crypto, desktop).mailboxId)
        val recovered = MatchSnapshotCodec.decodeList(SelfSync(crypto, desktop).open(blob))
        assertEquals(1, recovered.size)
        assertEquals("Mara", recovered[0].name)
        assertEquals("Café Léon", recovered[0].place)
    }

    @Test fun `a different identity cannot open another's self-sync`() {
        val blob = SelfSync(crypto, identity(1)).seal(byteArrayOf(1, 2, 3))
        assertFailsWith<Exception> { SelfSync(crypto, identity(2)).open(blob) }
    }
}
