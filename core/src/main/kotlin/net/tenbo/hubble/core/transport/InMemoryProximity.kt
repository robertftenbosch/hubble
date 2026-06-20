package net.tenbo.hubble.core.transport

import kotlinx.coroutines.channels.Channel

/**
 * A bidirectional in-memory [Transport] pair, useful as a local stand-in for a BLE
 * link in demos and tests. `pair()` returns two ends wired together: bytes sent on
 * one arrive on the other.
 */
class InMemoryProximity private constructor(
    private val outgoing: Channel<ByteArray>,
    private val incoming: Channel<ByteArray>,
) : Transport {
    override suspend fun send(bytes: ByteArray) { outgoing.send(bytes) }
    override suspend fun receive(): ByteArray = incoming.receive()

    companion object {
        fun pair(): Pair<Transport, Transport> {
            val a2b = Channel<ByteArray>(capacity = Channel.UNLIMITED)
            val b2a = Channel<ByteArray>(capacity = Channel.UNLIMITED)
            return InMemoryProximity(a2b, b2a) to InMemoryProximity(b2a, a2b)
        }
    }
}
