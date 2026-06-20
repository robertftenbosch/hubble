package net.tenbo.hubble.core.transport

/**
 * Moves opaque bytes between two peers. The handshake drives this; BLE/GATT (or
 * the in-memory test double) implement it. The handshake knows nothing else about BLE.
 */
interface Transport {
    /** Send one framed message to the peer. */
    suspend fun send(bytes: ByteArray)

    /** Receive the next framed message from the peer (suspends until available). */
    suspend fun receive(): ByteArray
}
