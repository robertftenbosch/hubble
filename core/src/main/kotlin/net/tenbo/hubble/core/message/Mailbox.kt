package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.CryptoProvider

/**
 * Opaque, unlinkable routing tags so the relay server can deliver envelopes without
 * learning identities. A recipient's mailbox id is derived from their signing public
 * key; a sender tag lets the recipient pick which friendship (and thus which root key)
 * an envelope belongs to, by matching against precomputed tags for each friend.
 */
class Mailbox(private val crypto: CryptoProvider) {

    /** Where envelopes for the holder of [signingPublicKey] are stored on the server. */
    fun id(signingPublicKey: ByteArray): String =
        crypto.blake2b(MAILBOX_LABEL + signingPublicKey, 16).toHex()

    /** A tag identifying the sender, matchable by the recipient without revealing identity. */
    fun senderTag(senderSigningPublicKey: ByteArray): String =
        crypto.blake2b(SENDER_LABEL + senderSigningPublicKey, 16).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        val MAILBOX_LABEL = "hubble-mailbox".toByteArray()
        val SENDER_LABEL = "hubble-sender-tag".toByteArray()
    }
}
