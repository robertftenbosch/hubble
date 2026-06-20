package net.tenbo.hubble.demo

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.handshake.Handshake
import net.tenbo.hubble.core.handshake.HandshakeState
import net.tenbo.hubble.core.identity.IdentityFactory
import net.tenbo.hubble.core.identity.Mnemonic
import net.tenbo.hubble.core.transport.InMemoryProximity
import java.security.SecureRandom

/**
 * Runnable end-to-end demonstration of Hubble's core mechanic, with no Android or
 * Bluetooth hardware required. Two simulated phones "in proximity" run the real
 * core handshake over an in-memory transport that stands in for the BLE/GATT link.
 *
 * Run:  ./gradlew :demo:run
 */
fun main() = runBlocking {
    val crypto = BouncyCastleCrypto()
    val factory = IdentityFactory(crypto)

    fun newIdentity(label: String): Pair<String, net.tenbo.hubble.core.identity.Identity> {
        val phrase = Mnemonic.generate(ByteArray(16).also { SecureRandom().nextBytes(it) })
        val id = factory.fromPhrase(phrase)
        println("  $label generated identity  hubbleId=${id.hubbleId}")
        println("    recovery phrase: ${phrase.split(" ").take(3).joinToString(" ")} … (12 words; the only backup)")
        return phrase to id
    }

    println("== Hubble proximity-friending demo ==\n")
    println("[1] Two people install Hubble. Each device generates a keypair identity:")
    val (_, alice) = newIdentity("Alice")
    val (_, bob) = newIdentity("Bob")

    println("\n[2] They are physically together. A BLE link opens (simulated here):")
    val (aliceLink, bobLink) = InMemoryProximity.pair()
    println("    proximity link established (stands in for BLE GATT)")

    val aliceHs = Handshake(crypto, alice, "Alice", aliceLink)
    val bobHs = Handshake(crypto, bob, "Bob", bobLink)

    println("\n[3] Both tap 'add'. Keys are exchanged and a shared secret derived in person:")
    val aStart = async { aliceHs.start() }
    val bStart = async { bobHs.start() }
    val aSas = aStart.await() as HandshakeState.AwaitingSasConfirmation
    val bSas = bStart.await() as HandshakeState.AwaitingSasConfirmation
    println("    Alice's screen shows:  ${aSas.sasEmoji.joinToString(" ")}")
    println("    Bob's screen shows:    ${bSas.sasEmoji.joinToString(" ")}")
    check(aSas.sasEmoji == bSas.sasEmoji) { "SAS mismatch — would indicate a relay/MITM attack!" }
    println("    -> codes match, so this is a genuine in-person meeting (no relay attack).")

    println("\n[4] Both confirm the codes match. Signatures are exchanged and verified:")
    val aDone = async { aliceHs.confirmSas() }
    val bDone = async { bobHs.confirmSas() }
    val aliceFriend = (aDone.await() as HandshakeState.Completed).friend
    val bobFriend = (bDone.await() as HandshakeState.Completed).friend

    println("    Alice is now friends with '${aliceFriend.displayName}' (${aliceFriend.hubbleId})")
    println("    Bob is now friends with   '${bobFriend.displayName}' (${bobFriend.hubbleId})")

    val sameKey = aliceFriend.rootKey.contentEquals(bobFriend.rootKey)
    println("\n[5] Both derived the SAME shared root key for future E2E messaging: $sameKey")
    println("    root key (first 8 bytes): ${aliceFriend.rootKey.take(8).joinToString("") { "%02x".format(it) }}…")

    check(sameKey)
    check(aliceFriend.hubbleId == bob.hubbleId)
    check(bobFriend.hubbleId == alice.hubbleId)

    println("\n[6] Later, apart: Alice posts an ephemeral story for her friends.")
    val aliceMsg = net.tenbo.hubble.core.message.Messaging(crypto, alice)
    val bobMsg = net.tenbo.hubble.core.message.Messaging(crypto, bob)
    val post = net.tenbo.hubble.core.message.EphemeralPost(
        id = "post-1",
        authorHubbleId = alice.hubbleId,
        text = "sunset at the harbour 🌅",
        createdAtMs = 0,
        ttlMs = 60_000,
    )
    val envelope = aliceMsg.sealPost(post, aliceFriend)
    println("    Alice's post: \"${post.text}\"")
    println("    -> sealed into an envelope the server CANNOT read:")
    println("       recipientMailbox = ${envelope.recipientMailbox}")
    println("       senderTag        = ${envelope.senderTag}")
    println("       ciphertext       = ${envelope.ciphertext.size} opaque bytes")

    println("\n[7] Bob comes online, collects his mailbox, and decrypts it:")
    check(envelope.recipientMailbox == bobMsg.myMailboxId())
    check(envelope.senderTag == bobMsg.expectedSenderTag(bobFriend))
    val received = bobMsg.open(envelope, bobFriend) as net.tenbo.hubble.core.message.Incoming.Post
    println("    Bob reads: \"${received.post.text}\"  (from ${received.post.authorHubbleId})")
    check(received.post.text == post.text)

    println("\n✅ Friendship from proximity + E2E ephemeral post delivered. Demo complete.")
}
