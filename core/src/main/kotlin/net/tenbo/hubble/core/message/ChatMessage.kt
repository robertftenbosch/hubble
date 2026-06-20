package net.tenbo.hubble.core.message

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** A direct message to a match. Unlike posts, chat messages do not expire. */
data class ChatMessage(
    val id: String,
    val authorHubbleId: String,
    val text: String,
    val sentAtMs: Long,
)

/** Deterministic length-prefixed codec for [ChatMessage]. */
object ChatMessageCodec {
    fun encode(m: ChatMessage): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(m.id)
            out.writeUTF(m.authorHubbleId)
            out.writeUTF(m.text)
            out.writeLong(m.sentAtMs)
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): ChatMessage =
        DataInputStream(bytes.inputStream()).use { ins ->
            ChatMessage(
                id = ins.readUTF(),
                authorHubbleId = ins.readUTF(),
                text = ins.readUTF(),
                sentAtMs = ins.readLong(),
            )
        }
}
