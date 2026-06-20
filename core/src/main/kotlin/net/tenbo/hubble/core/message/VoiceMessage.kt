package net.tenbo.hubble.core.message

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A recorded voice clip sent to a match — an "async" voice note. Like chat it does not
 * expire. [audio] holds the encoded clip bytes (e.g. AMR/3GP) and [mime] names the codec
 * so the receiver knows how to play it; [durationMs] lets the UI show length without
 * decoding. The audio rides the same E2E envelope as chat — the relay only sees ciphertext.
 */
data class VoiceMessage(
    val id: String,
    val authorHubbleId: String,
    val mime: String,
    val durationMs: Long,
    val sentAtMs: Long,
    val audio: ByteArray,
)

/** Deterministic length-prefixed codec for [VoiceMessage] (stable across platforms). */
object VoiceMessageCodec {
    fun encode(m: VoiceMessage): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(m.id)
            out.writeUTF(m.authorHubbleId)
            out.writeUTF(m.mime)
            out.writeLong(m.durationMs)
            out.writeLong(m.sentAtMs)
            out.writeInt(m.audio.size)
            out.write(m.audio)
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): VoiceMessage =
        DataInputStream(bytes.inputStream()).use { ins ->
            val id = ins.readUTF()
            val author = ins.readUTF()
            val mime = ins.readUTF()
            val duration = ins.readLong()
            val sentAt = ins.readLong()
            val audio = ByteArray(ins.readInt()).also { ins.readFully(it) }
            VoiceMessage(id, author, mime, duration, sentAt, audio)
        }
}
