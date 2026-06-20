package net.tenbo.hubble.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SasTest {
    private val crypto = BouncyCastleCrypto()
    private val sas = Sas(crypto)

    @Test fun `same transcript yields same emoji on both sides`() {
        val transcript = "ApubBpubSharedSecret".toByteArray()
        assertEquals(sas.emoji(transcript), sas.emoji(transcript))
    }

    @Test fun `produces five emoji`() {
        assertEquals(5, sas.emoji("anything".toByteArray()).size)
    }

    @Test fun `different transcripts almost always differ`() {
        assertNotEquals(sas.emoji("a".toByteArray()), sas.emoji("b".toByteArray()))
    }
}
