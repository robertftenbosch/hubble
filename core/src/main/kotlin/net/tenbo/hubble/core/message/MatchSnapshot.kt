package net.tenbo.hubble.core.message

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A match as shared between a person's own devices (phone → desktop). Carries enough
 * to chat: the peer's keys + root key, plus display fields. Synced opaquely via the
 * self-mailbox; never seen by the server in the clear.
 */
data class MatchSnapshot(
    val hubbleId: String,
    val name: String,
    val age: Int,
    val city: String,
    val place: String,
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val rootKey: ByteArray,
    val matchedAtMs: Long,
)

/** Deterministic codec for a list of [MatchSnapshot]s (the self-sync payload). */
object MatchSnapshotCodec {
    fun encodeList(matches: List<MatchSnapshot>): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeInt(matches.size)
            for (m in matches) {
                out.writeUTF(m.hubbleId)
                out.writeUTF(m.name)
                out.writeInt(m.age)
                out.writeUTF(m.city)
                out.writeUTF(m.place)
                out.writeField(m.signingPublicKey)
                out.writeField(m.agreementPublicKey)
                out.writeField(m.rootKey)
                out.writeLong(m.matchedAtMs)
            }
        }
        return bos.toByteArray()
    }

    fun decodeList(bytes: ByteArray): List<MatchSnapshot> =
        DataInputStream(bytes.inputStream()).use { ins ->
            val n = ins.readInt()
            (0 until n).map {
                MatchSnapshot(
                    hubbleId = ins.readUTF(),
                    name = ins.readUTF(),
                    age = ins.readInt(),
                    city = ins.readUTF(),
                    place = ins.readUTF(),
                    signingPublicKey = ins.readField(),
                    agreementPublicKey = ins.readField(),
                    rootKey = ins.readField(),
                    matchedAtMs = ins.readLong(),
                )
            }
        }

    private fun DataOutputStream.writeField(b: ByteArray) { writeInt(b.size); write(b) }
    private fun DataInputStream.readField(): ByteArray { val n = readInt(); return ByteArray(n).also { readFully(it) } }
}
