package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.handshake.Handshake
import net.tenbo.hubble.core.handshake.HandshakeState
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.identity.IdentityFactory
import net.tenbo.hubble.core.identity.Mnemonic
import net.tenbo.hubble.core.transport.InMemoryProximity
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessagingTest {
    private val crypto = BouncyCastleCrypto()
    private val factory = IdentityFactory(crypto)

    private fun identity(seed: Byte) = factory.fromPhrase(Mnemonic.generate(ByteArray(16) { seed }))

    /** Run the real BLE handshake so both sides hold the same friendship root key. */
    private fun befriend(a: Identity, b: Identity): Pair<FriendRecord, FriendRecord> = runBlocking {
        val (ta, tb) = InMemoryProximity.pair()
        val ha = Handshake(crypto, a, "A", ta)
        val hb = Handshake(crypto, b, "B", tb)
        val sa = async { ha.start() }; val sb = async { hb.start() }
        sa.await(); sb.await()
        val da = async { ha.confirmSas() }; val db = async { hb.confirmSas() }
        val aRec = (da.await() as HandshakeState.Completed).friend // A's record of B
        val bRec = (db.await() as HandshakeState.Completed).friend // B's record of A
        aRec to bRec
    }

    @Test fun `friend can open a sealed post but an eavesdropper cannot`() {
        val alice = identity(1); val bob = identity(2)
        val (aliceSeesBob, bobSeesAlice) = befriend(alice, bob)

        val aliceMsg = Messaging(crypto, alice)
        val bobMsg = Messaging(crypto, bob)

        val post = EphemeralPost("p1", alice.hubbleId, "hey from the park 🌳", createdAtMs = 0, ttlMs = 60_000)
        val envelope = aliceMsg.sealPost(post, friend = aliceSeesBob)

        // Bob routes by sender tag, then opens with his record of Alice.
        assertEquals(bobMsg.expectedSenderTag(bobSeesAlice), envelope.senderTag)
        assertEquals(bobMsg.myMailboxId(), envelope.recipientMailbox)

        val opened = bobMsg.open(envelope, friend = bobSeesAlice) as Incoming.Post
        assertEquals("hey from the park 🌳", opened.post.text)
        assertEquals(alice.hubbleId, opened.post.authorHubbleId)

        // A stranger with a different root key cannot decrypt.
        val mallory = identity(9)
        val fakeFriend = bobSeesAlice.copy(rootKey = ByteArray(32) { 0x11 })
        assertFailsWith<Exception> { Messaging(crypto, mallory).open(envelope, fakeFriend) }
    }

    @Test fun `a chat message round-trips between matched peers`() {
        val alice = identity(1); val bob = identity(2)
        val (aliceSeesBob, bobSeesAlice) = befriend(alice, bob)
        val msg = ChatMessage("m1", alice.hubbleId, "the corner table's free ☕", sentAtMs = 5)
        val envelope = Messaging(crypto, alice).sealChat(msg, aliceSeesBob)
        val opened = Messaging(crypto, bob).open(envelope, bobSeesAlice) as Incoming.Chat
        assertEquals("the corner table's free ☕", opened.message.text)
        assertEquals(alice.hubbleId, opened.message.authorHubbleId)
    }

    @Test fun `a voice clip round-trips between matched peers`() {
        val alice = identity(1); val bob = identity(2)
        val (aliceSeesBob, bobSeesAlice) = befriend(alice, bob)
        val clip = ByteArray(2048) { (it * 7).toByte() } // stand-in for an AMR/3GP recording
        val voice = VoiceMessage("v1", alice.hubbleId, "audio/3gpp", durationMs = 4200, sentAtMs = 9, audio = clip)
        val envelope = Messaging(crypto, alice).sealVoice(voice, aliceSeesBob)
        val opened = Messaging(crypto, bob).open(envelope, bobSeesAlice) as Incoming.Voice
        assertEquals("audio/3gpp", opened.message.mime)
        assertEquals(4200, opened.message.durationMs)
        assertEquals(alice.hubbleId, opened.message.authorHubbleId)
        assertTrue(clip.contentEquals(opened.message.audio))
    }

    @Test fun `tampered ciphertext is rejected`() {
        val alice = identity(1); val bob = identity(2)
        val (aliceSeesBob, bobSeesAlice) = befriend(alice, bob)
        val envelope = Messaging(crypto, alice)
            .sealPost(EphemeralPost("p", alice.hubbleId, "hi", 0, 60_000), aliceSeesBob)
        val tampered = envelope.copy(
            ciphertext = envelope.ciphertext.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() },
        )
        assertFailsWith<Exception> { Messaging(crypto, bob).open(tampered, bobSeesAlice) }
    }

    @Test fun `envelope round-trips through its wire codec`() {
        val alice = identity(1); val bob = identity(2)
        val (aliceSeesBob, _) = befriend(alice, bob)
        val envelope = Messaging(crypto, alice)
            .sealPost(EphemeralPost("p", alice.hubbleId, "x", 0, 1), aliceSeesBob)
        val decoded = EnvelopeCodec.decode(EnvelopeCodec.encode(envelope))
        assertEquals(envelope.recipientMailbox, decoded.recipientMailbox)
        assertEquals(envelope.senderTag, decoded.senderTag)
        assertTrue(envelope.ciphertext.contentEquals(decoded.ciphertext))
    }

    @Test fun `posts expire after their ttl`() {
        val post = EphemeralPost("p", "id", "t", createdAtMs = 1_000, ttlMs = 500)
        assertFalse(post.isExpired(1_400))
        assertTrue(post.isExpired(1_500))
        assertTrue(post.isExpired(2_000))
    }
}
