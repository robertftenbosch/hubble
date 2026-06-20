package net.tenbo.hubble.core.handshake

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.identity.IdentityFactory
import net.tenbo.hubble.core.identity.Mnemonic
import net.tenbo.hubble.core.transport.InMemoryTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandshakeTest {
    private val crypto = BouncyCastleCrypto()
    private val factory = IdentityFactory(crypto)

    private fun identity(seed: Byte) = factory.fromPhrase(Mnemonic.generate(ByteArray(16) { seed }))

    @Test fun `two peers derive the same root key and matching SAS`() = runTest {
        val (ta, tb) = InMemoryTransport.pair()
        val alice = Handshake(crypto, identity(1), "Alice", ta)
        val bob = Handshake(crypto, identity(2), "Bob", tb)

        val aState = async { alice.start() }
        val bState = async { bob.start() }
        val a = aState.await() as HandshakeState.AwaitingSasConfirmation
        val b = bState.await() as HandshakeState.AwaitingSasConfirmation
        assertEquals(a.sasEmoji, b.sasEmoji) // users compare these in person

        val aDone = async { alice.confirmSas() }
        val bDone = async { bob.confirmSas() }
        val af = (aDone.await() as HandshakeState.Completed).friend
        val bf = (bDone.await() as HandshakeState.Completed).friend

        assertContentEquals(af.rootKey, bf.rootKey)   // shared secret agreement
        assertEquals("Bob", af.displayName)            // alice sees bob
        assertEquals("Alice", bf.displayName)          // bob sees alice
        assertEquals(identity(2).hubbleId, af.hubbleId)
        assertTrue(af.rootKey.isNotEmpty())
    }

    @Test fun `a tampered confirm signature aborts the handshake`() = runTest {
        val (ta, tb) = InMemoryTransport.pair()
        val alice = Handshake(crypto, identity(1), "Alice", ta)
        val bob = Handshake(crypto, identity(2), "Bob", tb, forgeSignature = true)

        // Both starts must be launched concurrently: each blocks awaiting the other's Hello.
        val aStart = async { alice.start() }
        val bStart = async { bob.start() }
        aStart.await(); bStart.await()

        val aDone = async { alice.confirmSas() }
        val bDone = async { runCatching { bob.confirmSas() } }
        assertTrue(aDone.await() is HandshakeState.Aborted)
        bDone.await()
    }
}
