package net.tenbo.hubble.app.p2p

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope

/**
 * Owns this device's WebRTC signaling connection and per-peer sessions. Initializing
 * brings up the PeerConnectionFactory and connects the signaling WebSocket under our
 * own mailbox id, so peers can reach us. Incoming offers spin up a responder session;
 * [connectTo] opens an initiator session to a chosen peer.
 *
 * When a data channel opens (either side), [onTransport] fires with a ready
 * [WebRtcTransport] — the same E2E envelopes that go via the relay flow over it
 * instead, directly peer-to-peer. The relay remains the fallback when a peer is offline.
 */
class WebRtcManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val myMailboxId: String,
    private val wsUrl: String = "ws://127.0.0.1:4000",
    private val onTransport: (peerId: String, WebRtcTransport) -> Unit,
) {
    private val tag = "HubbleWebRtc"
    private val sessions = mutableMapOf<String, WebRtcSession>()

    private val signaling = SignalingClient(wsUrl, myMailboxId) { from, type, payload ->
        session(from, isInitiator = false).onSignal(type, payload)
    }

    fun start() {
        WebRtcFactory.get(context) // initialize once
        signaling.connect()
        Log.i(tag, "p2p ready (mailbox $myMailboxId)")
    }

    /** Initiator: begin a direct connection to [peerId] (their mailbox id). */
    fun connectTo(peerId: String) {
        session(peerId, isInitiator = true).start()
    }

    private fun session(peerId: String, isInitiator: Boolean): WebRtcSession =
        sessions.getOrPut(peerId) {
            WebRtcSession(context, peerId, isInitiator, signaling) { transport ->
                onTransport(peerId, transport)
            }
        }

    fun stop() {
        sessions.values.forEach { it.close() }
        sessions.clear()
        signaling.close()
    }
}
