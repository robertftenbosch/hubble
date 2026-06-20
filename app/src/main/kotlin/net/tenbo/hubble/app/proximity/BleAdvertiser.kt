package net.tenbo.hubble.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid

/**
 * Advertises the Hubble service with a short *rotating ephemeral token* (never the
 * identity key) so passive observers cannot track a user across time. Generate a
 * fresh token each session/rotation interval and restart advertising.
 */
class BleAdvertiser(private val advertiser: BluetoothLeAdvertiser) {

    @SuppressLint("MissingPermission") // caller must hold BLUETOOTH_ADVERTISE
    fun start(ephemeralToken: ByteArray, callback: AdvertiseCallback) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(HubbleGatt.SERVICE_UUID))
            .addServiceData(ParcelUuid(HubbleGatt.SERVICE_UUID), ephemeralToken)
            .build()
        advertiser.startAdvertising(settings, data, callback)
    }

    @SuppressLint("MissingPermission")
    fun stop(callback: AdvertiseCallback) = advertiser.stopAdvertising(callback)
}
