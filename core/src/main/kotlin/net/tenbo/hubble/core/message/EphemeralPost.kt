package net.tenbo.hubble.core.message

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * An ephemeral post shared with friends. It expires [ttlMs] after [createdAtMs];
 * expired posts are not rendered. [authorHubbleId] is the sender's public fingerprint.
 */
data class EphemeralPost(
    val id: String,
    val authorHubbleId: String,
    val text: String,
    val createdAtMs: Long,
    val ttlMs: Long,
) {
    fun isExpired(nowMs: Long): Boolean = nowMs >= createdAtMs + ttlMs
}

/** Deterministic length-prefixed codec for [EphemeralPost] (stable across platforms). */
object EphemeralPostCodec {
    fun encode(post: EphemeralPost): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeUTF(post.id)
            out.writeUTF(post.authorHubbleId)
            out.writeUTF(post.text)
            out.writeLong(post.createdAtMs)
            out.writeLong(post.ttlMs)
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): EphemeralPost =
        DataInputStream(bytes.inputStream()).use { ins ->
            EphemeralPost(
                id = ins.readUTF(),
                authorHubbleId = ins.readUTF(),
                text = ins.readUTF(),
                createdAtMs = ins.readLong(),
                ttlMs = ins.readLong(),
            )
        }
}
