package net.tenbo.hubble.core.handshake

import net.tenbo.hubble.core.crypto.CryptoProvider
import net.tenbo.hubble.core.crypto.Sas
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.transport.Transport

/**
 * Pure, transport-driven friend-handshake state machine. One instance per peer.
 *
 * Protocol:
 *  1. Both sides send Hello (keys + nonce + name) and receive the peer's Hello.
 *  2. Each derives the shared secret via X25519 ECDH and a root key via HKDF over
 *     a canonical transcript. The SAS (emoji) is derived from the same transcript.
 *  3. UI shows the SAS; both users compare in person and call confirmSas().
 *  4. Each side sends a Confirm = Ed25519 signature over the transcript, verifies
 *     the peer's signature, and emits Completed(FriendRecord) or Aborted.
 *
 * [clockMs] is injectable so tests are deterministic. [forgeSignature] is a test
 * hook that simulates a peer signing with the wrong key (MITM/forgery).
 */
class Handshake(
    private val crypto: CryptoProvider,
    private val self: Identity,
    private val displayName: String,
    private val transport: Transport,
    private val clockMs: () -> Long = { 0L },
    private val forgeSignature: Boolean = false,
) {
    private val sas = Sas(crypto)
    private lateinit var peerHello: HandshakeMessage.Hello
    private lateinit var transcript: ByteArray
    private lateinit var rootKey: ByteArray

    var state: HandshakeState = HandshakeState.Idle
        private set

    /** Send our Hello, receive theirs, derive secret, return AwaitingSasConfirmation. */
    suspend fun start(): HandshakeState {
        val myNonce = crypto.blake2b(self.signingPublicKey + clockMs().toString().toByteArray(), 16)
        val hello = HandshakeMessage.Hello(
            signingPublicKey = self.signingPublicKey,
            agreementPublicKey = self.agreementPublicKey,
            displayName = displayName,
            nonce = myNonce,
        )
        transport.send(HandshakeCodec.encode(hello))
        state = HandshakeState.HelloSent

        peerHello = HandshakeCodec.decode(transport.receive()) as HandshakeMessage.Hello

        val shared = crypto.x25519SharedSecret(self.agreementPrivateKey, peerHello.agreementPublicKey)
        transcript = canonicalTranscript(hello, peerHello, shared)
        rootKey = crypto.hkdf(shared, salt = byteArrayOf(), info = "hubble-root".toByteArray(), length = 32)

        state = HandshakeState.AwaitingSasConfirmation(sas.emoji(transcript))
        return state
    }

    /** Called after the user confirms the SAS matched in person. */
    suspend fun confirmSas(): HandshakeState {
        val sigKey = if (forgeSignature) ByteArray(32) { 0x55 } else self.signingPrivateKey
        val mySig = crypto.sign(sigKey, transcript)
        transport.send(HandshakeCodec.encode(HandshakeMessage.Confirm(mySig)))

        val peerConfirm = HandshakeCodec.decode(transport.receive()) as HandshakeMessage.Confirm
        val ok = crypto.verify(peerHello.signingPublicKey, transcript, peerConfirm.signature)
        state = if (!ok) {
            HandshakeState.Aborted("peer signature verification failed")
        } else {
            HandshakeState.Completed(
                FriendRecord(
                    hubbleId = crypto.blake2b(peerHello.signingPublicKey, 8).toHex(),
                    displayName = peerHello.displayName,
                    signingPublicKey = peerHello.signingPublicKey,
                    agreementPublicKey = peerHello.agreementPublicKey,
                    rootKey = rootKey,
                    establishedAtEpochMs = clockMs(),
                ),
            )
        }
        return state
    }

    /** Order-independent transcript: sort the two Hellos by signing key bytes. */
    private fun canonicalTranscript(
        a: HandshakeMessage.Hello,
        b: HandshakeMessage.Hello,
        shared: ByteArray,
    ): ByteArray {
        val sorted = listOf(a, b).sortedBy { it.signingPublicKey.toHex() }
        return crypto.blake2b(
            HandshakeCodec.encode(sorted[0]) + HandshakeCodec.encode(sorted[1]) + shared,
            32,
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
