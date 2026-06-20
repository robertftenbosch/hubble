package net.tenbo.hubble.app.p2p

import kotlinx.coroutines.channels.Channel
import net.tenbo.hubble.core.transport.Transport
import org.webrtc.DataChannel
import java.nio.ByteBuffer

/**
 * [Transport] over a WebRTC [DataChannel]. Unlike BLE, a data channel preserves
 * message boundaries, so no framing is needed: each [send] is one message and each
 * inbound message is delivered whole. The session feeds inbound bytes via [onMessage].
 */
class WebRtcTransport(private val channel: DataChannel) : Transport {

    private val inbound = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    /** Called by the DataChannel.Observer for each inbound message. */
    fun onMessage(bytes: ByteArray) {
        inbound.trySend(bytes)
    }

    override suspend fun send(bytes: ByteArray) {
        channel.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), /* binary = */ true))
    }

    override suspend fun receive(): ByteArray = inbound.receive()

    fun close() {
        inbound.close()
        runCatching { channel.close() }
    }
}
