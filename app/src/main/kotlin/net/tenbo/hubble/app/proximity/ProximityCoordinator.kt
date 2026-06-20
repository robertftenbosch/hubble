package net.tenbo.hubble.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.tenbo.hubble.core.transport.Transport
import java.security.SecureRandom

/**
 * Drives the foreground proximity flow: advertise this device, scan for others,
 * and open a GATT server so peers can connect. Discovered devices are surfaced via
 * [nearby]; [incomingTransports] yields a transport when a peer connects to us
 * (responder), and [connect] opens one to a chosen peer (initiator).
 */
class ProximityCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val env: BleEnvironment,
) {
    private val tag = "HubbleProximity"
    private val advertiser = BleAdvertiser(env.advertiser ?: error("no BLE advertiser"))
    private val scanner = BleScanner(env.scanner ?: error("no BLE scanner"))
    private val gattServer = GattServer(context, scope)

    private val _nearby = MutableStateFlow<List<NearbyHubble>>(emptyList())
    val nearby: StateFlow<List<NearbyHubble>> = _nearby

    val incomingTransports: Channel<BleTransport> get() = gattServer.transports

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(tag, "advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.w(tag, "advertising failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        gattServer.open()
        val token = ByteArray(4).also { SecureRandom().nextBytes(it) }
        advertiser.start(token, advertiseCallback)
        scope.launch {
            scanner.scan().collect { hub ->
                val current = _nearby.value.filter { it.address != hub.address }
                _nearby.value = (current + hub).sortedByDescending { it.rssi }
                Log.d(tag, "nearby: ${hub.address} rssi=${hub.rssi}")
            }
        }
    }

    /** Initiator: connect to a chosen peer and return the transport for the handshake. */
    @SuppressLint("MissingPermission")
    suspend fun connect(hubble: NearbyHubble): Transport {
        val device = env.adapter!!.getRemoteDevice(hubble.address)
        return GattClient(context, scope).connect(device)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        advertiser.stop(advertiseCallback)
        gattServer.close()
        _nearby.value = emptyList()
    }
}
