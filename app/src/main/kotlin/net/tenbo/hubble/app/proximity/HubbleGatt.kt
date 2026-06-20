package net.tenbo.hubble.app.proximity

import java.util.UUID

/** Shared BLE identifiers for Hubble's proximity protocol. */
object HubbleGatt {
    /** Service advertised + scanned for. */
    val SERVICE_UUID: UUID = UUID.fromString("0000b0bb-0000-1000-8000-00805f9b34fb")

    /** Central writes handshake frames here (peripheral receives). */
    val RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000b0b1-0000-1000-8000-00805f9b34fb")

    /** Peripheral notifies handshake frames here (central receives). */
    val TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000b0b2-0000-1000-8000-00805f9b34fb")

    /** Standard Client Characteristic Configuration Descriptor for enabling notifications. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
