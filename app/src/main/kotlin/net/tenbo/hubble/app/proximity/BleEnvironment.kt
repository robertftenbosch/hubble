package net.tenbo.hubble.app.proximity

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Central access to the device's BLE capabilities and the permissions the proximity
 * flow needs. Keeps the Android-version branching (12+ vs pre-12) in one place.
 */
class BleEnvironment(private val context: Context) {

    val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    val adapter get() = bluetoothManager?.adapter

    val isBluetoothReady: Boolean get() = adapter?.isEnabled == true

    val advertiser get() = adapter?.bluetoothLeAdvertiser
    val scanner get() = adapter?.bluetoothLeScanner

    fun hasAllPermissions(): Boolean =
        requiredPermissions().all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        /** Permissions required to advertise + scan + connect, by SDK level. */
        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            }
    }
}
