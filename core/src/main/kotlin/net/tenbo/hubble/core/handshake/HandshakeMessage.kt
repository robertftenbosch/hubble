package net.tenbo.hubble.core.handshake

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Wire messages exchanged over Transport during the friend handshake. */
sealed interface HandshakeMessage {
    /** Public keys + display name + a per-session ephemeral nonce. */
    data class Hello(
        val signingPublicKey: ByteArray,
        val agreementPublicKey: ByteArray,
        val displayName: String,
        val nonce: ByteArray,
    ) : HandshakeMessage

    /** Signature over the canonical transcript, proving key ownership. */
    data class Confirm(val signature: ByteArray) : HandshakeMessage
}

/** Length-prefixed, deterministic binary codec — no JSON, stable across platforms. */
object HandshakeCodec {
    private const val TYPE_HELLO = 1
    private const val TYPE_CONFIRM = 2

    fun encode(msg: HandshakeMessage): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            when (msg) {
                is HandshakeMessage.Hello -> {
                    out.writeByte(TYPE_HELLO)
                    out.writeField(msg.signingPublicKey)
                    out.writeField(msg.agreementPublicKey)
                    out.writeField(msg.displayName.toByteArray(Charsets.UTF_8))
                    out.writeField(msg.nonce)
                }
                is HandshakeMessage.Confirm -> {
                    out.writeByte(TYPE_CONFIRM)
                    out.writeField(msg.signature)
                }
            }
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): HandshakeMessage {
        DataInputStream(bytes.inputStream()).use { ins ->
            return when (val type = ins.readByte().toInt()) {
                TYPE_HELLO -> HandshakeMessage.Hello(
                    signingPublicKey = ins.readField(),
                    agreementPublicKey = ins.readField(),
                    displayName = String(ins.readField(), Charsets.UTF_8),
                    nonce = ins.readField(),
                )
                TYPE_CONFIRM -> HandshakeMessage.Confirm(signature = ins.readField())
                else -> throw IllegalArgumentException("unknown handshake message type $type")
            }
        }
    }

    private fun DataOutputStream.writeField(b: ByteArray) { writeInt(b.size); write(b) }
    private fun DataInputStream.readField(): ByteArray {
        val n = readInt(); val b = ByteArray(n); readFully(b); return b
    }
}
