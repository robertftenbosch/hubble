package net.tenbo.hubble.core.handshake

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HandshakeCodecTest {
    @Test fun `hello round-trips through encode-decode`() {
        val hello = HandshakeMessage.Hello(
            signingPublicKey = ByteArray(32) { 1 },
            agreementPublicKey = ByteArray(32) { 2 },
            displayName = "Robert 🌍",
            nonce = ByteArray(16) { 3 },
        )
        val decoded = HandshakeCodec.decode(HandshakeCodec.encode(hello)) as HandshakeMessage.Hello
        assertContentEquals(hello.signingPublicKey, decoded.signingPublicKey)
        assertContentEquals(hello.agreementPublicKey, decoded.agreementPublicKey)
        assertEquals(hello.displayName, decoded.displayName)
        assertContentEquals(hello.nonce, decoded.nonce)
    }

    @Test fun `confirm round-trips through encode-decode`() {
        val confirm = HandshakeMessage.Confirm(ByteArray(64) { 9 })
        val decoded = HandshakeCodec.decode(HandshakeCodec.encode(confirm)) as HandshakeMessage.Confirm
        assertContentEquals(confirm.signature, decoded.signature)
    }
}
