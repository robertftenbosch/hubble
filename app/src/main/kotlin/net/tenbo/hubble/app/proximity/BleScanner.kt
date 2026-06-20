package net.tenbo.hubble.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** A nearby Hubble device discovered over BLE; [rssi] is the proximity-strength hint. */
data class NearbyHubble(val address: String, val rssi: Int, val token: ByteArray)

/** Scans (foreground) for the Hubble service and emits discovered devices as a Flow. */
class BleScanner(private val scanner: BluetoothLeScanner) {

    @SuppressLint("MissingPermission") // caller must hold BLUETOOTH_SCAN
    fun scan() = callbackFlow {
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val token = result.scanRecord
                    ?.getServiceData(ParcelUuid(HubbleGatt.SERVICE_UUID))
                    ?: ByteArray(0)
                trySend(NearbyHubble(result.device.address, result.rssi, token))
            }
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HubbleGatt.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, cb)
        awaitClose { scanner.stopScan(cb) }
    }
}
