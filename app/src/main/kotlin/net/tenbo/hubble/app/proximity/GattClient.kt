package net.tenbo.hubble.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Central-role GATT connection to a peripheral Hubble device. Connects, discovers
 * the Hubble service, enables TX notifications, negotiates MTU, and feeds inbound
 * notification bytes into a [BleTransport] while writing outbound frames to RX.
 *
 * The initiator (the user who taps "add") uses this client; the target runs a
 * peripheral GATT server exposing the same characteristics (see GattServer).
 */
class GattClient(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var gatt: BluetoothGatt? = null
    private var rx: BluetoothGattCharacteristic? = null
    private val ready = CompletableDeferred<BleTransport>()

    private lateinit var transport: BleTransport

    @SuppressLint("MissingPermission") // caller must hold BLUETOOTH_CONNECT
    suspend fun connect(device: BluetoothDevice): BleTransport {
        transport = BleTransport(writeRaw = ::writeFrame)
        device.connectGatt(context, false, callback)
        return ready.await()
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeFrame(bytes: ByteArray) {
        val characteristic = rx ?: return
        val g = gatt ?: return
        // API 33+: writeCharacteristic(char, value, writeType). Older: setValue + write.
        @Suppress("DEPRECATION")
        run {
            characteristic.value = bytes
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            g.writeCharacteristic(characteristic)
        }
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = g
                g.requestMtu(247) // request a larger MTU for fewer chunks
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (!ready.isCompleted) ready.completeExceptionally(IllegalStateException("disconnected"))
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(HubbleGatt.SERVICE_UUID) ?: return
            rx = service.getCharacteristic(HubbleGatt.RX_CHARACTERISTIC_UUID)
            val tx = service.getCharacteristic(HubbleGatt.TX_CHARACTERISTIC_UUID) ?: return
            g.setCharacteristicNotification(tx, true)
            val cccd = tx.getDescriptor(HubbleGatt.CCCD_UUID)
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            // Notifications enabled — transport is ready for the handshake.
            if (!ready.isCompleted) ready.complete(transport)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            val value = c.value ?: return
            scope.launch { transport.onPacket(value) }
        }
    }

    @SuppressLint("MissingPermission")
    fun close() { gatt?.close(); gatt = null }
}
