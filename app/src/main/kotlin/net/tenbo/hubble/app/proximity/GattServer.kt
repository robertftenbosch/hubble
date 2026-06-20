package net.tenbo.hubble.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Peripheral-role GATT server. Exposes the Hubble service with an RX characteristic
 * (central writes handshake frames here) and a TX characteristic (we notify frames
 * back). When a central connects and starts the handshake, a [BleTransport] is
 * produced on [transports] for the orchestrator to run the responder handshake.
 */
class GattServer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var server: BluetoothGattServer? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var connected: BluetoothDevice? = null
    private var transport: BleTransport? = null

    /** Emits a transport when a central connects and begins talking. */
    val transports = Channel<BleTransport>(capacity = Channel.UNLIMITED)

    @SuppressLint("MissingPermission") // caller holds BLUETOOTH_CONNECT
    fun open() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        server = manager.openGattServer(context, callback)

        val service = BluetoothGattService(
            HubbleGatt.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        val rx = BluetoothGattCharacteristic(
            HubbleGatt.RX_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        tx = BluetoothGattCharacteristic(
            HubbleGatt.TX_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        service.addCharacteristic(rx)
        service.addCharacteristic(tx)
        server?.addService(service)
    }

    @SuppressLint("MissingPermission")
    fun close() {
        server?.close()
        server = null
        transport = null
        connected = null
    }

    @SuppressLint("MissingPermission")
    private fun ensureTransport(device: BluetoothDevice): BleTransport {
        connected = device
        return transport ?: BleTransport(
            writeRaw = { bytes ->
                val characteristic = tx ?: return@BleTransport
                val dev = connected ?: return@BleTransport
                @Suppress("DEPRECATION")
                run {
                    characteristic.value = bytes
                    server?.notifyCharacteristicChanged(dev, characteristic, false)
                }
            },
        ).also {
            transport = it
            scope.launch { transports.send(it) }
        }
    }

    private val callback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HubbleGatt.RX_CHARACTERISTIC_UUID) {
                val t = ensureTransport(device)
                scope.launch { t.onPacket(value) }
            }
            if (responseNeeded) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }
    }
}
