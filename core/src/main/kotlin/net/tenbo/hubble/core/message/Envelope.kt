package net.tenbo.hubble.core.message

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * What crosses the wire / sits on the relay server. Everything the server sees is
 * opaque: [recipientMailbox] and [senderTag] are unlinkable routing tags, and
 * [ciphertext] is AEAD-encrypted under the friendship root key.
 */
data class Envelope(
    val recipientMailbox: String,
    val senderTag: String,
    val ciphertext: ByteArray,
)

/** Deterministic length-prefixed codec for [Envelope]. */
object EnvelopeCodec {
    fun encode(e: Envelope): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(e.recipientMailbox)
            out.writeUTF(e.senderTag)
            out.writeInt(e.ciphertext.size)
            out.write(e.ciphertext)
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): Envelope =
        DataInputStream(bytes.inputStream()).use { ins ->
            val mailbox = ins.readUTF()
            val tag = ins.readUTF()
            val n = ins.readInt()
            val ct = ByteArray(n).also { ins.readFully(it) }
            Envelope(mailbox, tag, ct)
        }
}
