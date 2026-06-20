package net.tenbo.hubble.app.proximity

import kotlinx.coroutines.channels.Channel
import net.tenbo.hubble.core.transport.Transport
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * [Transport] over a connected GATT link. The GATT client/server callbacks push
 * raw inbound BLE packets into [rawInbound]; this class reassembles length-prefixed
 * frames and exposes them via [receive]. [writeRaw] sends a frame, chunked to the
 * negotiated MTU by the caller's GATT layer.
 *
 * Framing: each logical handshake message is prefixed with a 4-byte big-endian
 * length, then split into MTU-sized BLE packets. The receiver buffers until a full
 * frame is available.
 */
class BleTransport(
    private val writeRaw: suspend (ByteArray) -> Unit,
    private val mtu: Int = 20,
) : Transport {

    private val rawInbound = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private val assembled = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private val buffer = ByteArrayOutputStream()
    private var expected = -1

    /** Called by the GATT layer for every inbound BLE packet. */
    suspend fun onPacket(bytes: ByteArray) {
        buffer.write(bytes)
        drain()
    }

    private suspend fun drain() {
        while (true) {
            val data = buffer.toByteArray()
            if (expected < 0) {
                if (data.size < 4) return
                expected = ByteBuffer.wrap(data, 0, 4).int
                // keep remainder after the length prefix
                buffer.reset()
                buffer.write(data, 4, data.size - 4)
                continue
            }
            val body = buffer.toByteArray()
            if (body.size < expected) return
            assembled.send(body.copyOfRange(0, expected))
            val rest = body.copyOfRange(expected, body.size)
            buffer.reset()
            buffer.write(rest)
            expected = -1
        }
    }

    override suspend fun send(bytes: ByteArray) {
        val framed = ByteBuffer.allocate(4 + bytes.size).putInt(bytes.size).put(bytes).array()
        var offset = 0
        while (offset < framed.size) {
            val end = minOf(offset + mtu, framed.size)
            writeRaw(framed.copyOfRange(offset, end))
            offset = end
        }
    }

    override suspend fun receive(): ByteArray = assembled.receive()
}
