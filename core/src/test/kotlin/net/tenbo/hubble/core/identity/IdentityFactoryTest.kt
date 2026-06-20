package net.tenbo.hubble.core.identity

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdentityFactoryTest {
    private val factory = IdentityFactory(BouncyCastleCrypto())

    @Test fun `same phrase reconstructs identical identity (recovery)`() {
        val phrase = Mnemonic.generate(ByteArray(16) { 5 })
        val a = factory.fromPhrase(phrase)
        val b = factory.fromPhrase(phrase)
        assertContentEquals(a.signingPublicKey, b.signingPublicKey)
        assertContentEquals(a.agreementPublicKey, b.agreementPublicKey)
        assertEquals(a.hubbleId, b.hubbleId)
    }

    @Test fun `signing and agreement keys are independent`() {
        val id = factory.fromPhrase(Mnemonic.generate(ByteArray(16) { 5 }))
        assertTrue(!id.signingPublicKey.contentEquals(id.agreementPublicKey))
    }

    @Test fun `hubbleId is a short hex fingerprint of the signing key`() {
        val id = factory.fromPhrase(Mnemonic.generate(ByteArray(16) { 5 }))
        assertEquals(16, id.hubbleId.length) // 8 bytes hex
    }
}
