package net.tenbo.hubble.core.message

import net.tenbo.hubble.core.crypto.CryptoProvider
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.identity.Identity
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** What an opened envelope contained — a post, a chat message, a voice clip, or a typing ping. */
sealed interface Incoming {
    data class Post(val post: EphemeralPost) : Incoming
    data class Chat(val message: ChatMessage) : Incoming
    data class Voice(val message: VoiceMessage) : Incoming
    data object Typing : Incoming
}

/**
 * Seals and opens [Envelope]s for friends/matches. Sealing signs the payload with our
 * Ed25519 key, then encrypts it under the shared root key; a 1-byte type tag lets the
 * recipient tell a post from a chat message. Opening reverses it and verifies the
 * signature against the sender's known key — so the recipient is sure only the two of
 * them can read it (encryption) and that the sender really authored it (signature).
 */
class Messaging(
    private val crypto: CryptoProvider,
    private val self: Identity,
) {
    private val mailbox = Mailbox(crypto)

    fun myMailboxId(): String = mailbox.id(self.signingPublicKey)

    fun expectedSenderTag(friend: FriendRecord): String =
        mailbox.senderTag(friend.signingPublicKey)

    fun sealPost(post: EphemeralPost, friend: FriendRecord): Envelope =
        seal(TYPE_POST, EphemeralPostCodec.encode(post), friend)

    fun sealChat(message: ChatMessage, friend: FriendRecord): Envelope =
        seal(TYPE_CHAT, ChatMessageCodec.encode(message), friend)

    fun sealVoice(message: VoiceMessage, friend: FriendRecord): Envelope =
        seal(TYPE_VOICE, VoiceMessageCodec.encode(message), friend)

    /** A tiny "I'm typing" ping (no body) so the peer can show "is typing…". */
    fun sealTyping(friend: FriendRecord): Envelope = seal(TYPE_TYPING, ByteArray(0), friend)

    /** Decrypt + verify an envelope known (by sender tag) to be from [friend]. */
    fun open(envelope: Envelope, friend: FriendRecord): Incoming {
        val inner = MessageCrypto(crypto, friend.rootKey).decrypt(envelope.ciphertext)
        val (type, payload, senderKey, signature) = decodeInner(inner)
        require(senderKey.contentEquals(friend.signingPublicKey)) { "sender key mismatch" }
        require(crypto.verify(senderKey, payload, signature)) { "bad signature" }
        return when (type) {
            TYPE_POST -> Incoming.Post(EphemeralPostCodec.decode(payload))
            TYPE_CHAT -> Incoming.Chat(ChatMessageCodec.decode(payload))
            TYPE_VOICE -> Incoming.Voice(VoiceMessageCodec.decode(payload))
            TYPE_TYPING -> Incoming.Typing
            else -> throw IllegalArgumentException("unknown payload type $type")
        }
    }

    private fun seal(type: Byte, payload: ByteArray, friend: FriendRecord): Envelope {
        val signature = crypto.sign(self.signingPrivateKey, payload)
        val inner = encodeInner(type, payload, self.signingPublicKey, signature)
        val ciphertext = MessageCrypto(crypto, friend.rootKey).encrypt(inner)
        return Envelope(
            recipientMailbox = mailbox.id(friend.signingPublicKey),
            senderTag = mailbox.senderTag(self.signingPublicKey),
            ciphertext = ciphertext,
        )
    }

    private data class Inner(val type: Byte, val payload: ByteArray, val senderKey: ByteArray, val signature: ByteArray)

    private fun encodeInner(type: Byte, payload: ByteArray, senderKey: ByteArray, signature: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeByte(type.toInt())
            out.writeInt(payload.size); out.write(payload)
            out.writeInt(senderKey.size); out.write(senderKey)
            out.writeInt(signature.size); out.write(signature)
        }
        return bos.toByteArray()
    }

    private fun decodeInner(bytes: ByteArray): Inner =
        DataInputStream(bytes.inputStream()).use { ins ->
            val type = ins.readByte()
            fun field(): ByteArray { val n = ins.readInt(); return ByteArray(n).also { ins.readFully(it) } }
            Inner(type, field(), field(), field())
        }

    private companion object {
        const val TYPE_POST: Byte = 1
        const val TYPE_CHAT: Byte = 2
        const val TYPE_TYPING: Byte = 3
        const val TYPE_VOICE: Byte = 4
    }
}
