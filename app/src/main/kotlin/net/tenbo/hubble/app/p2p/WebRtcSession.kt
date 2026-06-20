package net.tenbo.hubble.app.p2p

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/** Lazily-initialized WebRTC factory (initialize once per process). */
object WebRtcFactory {
    private var factory: PeerConnectionFactory? = null

    fun get(context: Context): PeerConnectionFactory = factory ?: synchronized(this) {
        factory ?: run {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .createInitializationOptions(),
            )
            PeerConnectionFactory.builder().createPeerConnectionFactory().also {
                factory = it
                Log.i("HubbleWebRtc", "PeerConnectionFactory initialized")
            }
        }
    }
}

/**
 * One WebRTC session with a single peer. The initiator opens the "hubble" data channel
 * and sends an SDP offer; the responder answers. SDP/ICE are exchanged via
 * [signaling]. When the channel opens, [onReady] receives a [WebRtcTransport] over
 * which the existing E2E envelopes flow. Direct P2P; the relay is the fallback.
 */
class WebRtcSession(
    context: Context,
    private val peerId: String,
    private val isInitiator: Boolean,
    private val signaling: SignalingClient,
    private val onReady: (WebRtcTransport) -> Unit,
) {
    private val tag = "HubbleWebRtc"
    private val factory = WebRtcFactory.get(context)
    private var transport: WebRtcTransport? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    )

    private val pc: PeerConnection? = factory.createPeerConnection(
        PeerConnection.RTCConfiguration(iceServers),
        object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                val payload = JSONObject()
                    .put("sdpMid", c.sdpMid)
                    .put("sdpMLineIndex", c.sdpMLineIndex)
                    .put("candidate", c.sdp)
                signaling.send(peerId, "ice", payload.toString())
            }

            override fun onDataChannel(channel: DataChannel) = bindChannel(channel)

            // Unused callbacks (data-channel-only session).
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                Log.i(tag, "ice state: $s")
            }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver?, s: Array<out MediaStream>?) {}
        },
    )

    fun start() {
        if (isInitiator) {
            val channel = pc?.createDataChannel("hubble", DataChannel.Init())
            channel?.let { bindChannel(it) }
            pc?.createOffer(sdpObserver { offer -> setLocalAndSend(offer, "offer") }, MediaConstraints())
        }
    }

    /** Feed an inbound signaling message addressed to us. */
    fun onSignal(type: String, payload: String) {
        when (type) {
            "offer" -> {
                pc?.setRemoteDescription(setObserver {
                    pc.createAnswer(sdpObserver { ans -> setLocalAndSend(ans, "answer") }, MediaConstraints())
                }, SessionDescription(SessionDescription.Type.OFFER, payload))
            }
            "answer" -> pc?.setRemoteDescription(setObserver {}, SessionDescription(SessionDescription.Type.ANSWER, payload))
            "ice" -> {
                val o = JSONObject(payload)
                pc?.addIceCandidate(IceCandidate(o.optString("sdpMid"), o.optInt("sdpMLineIndex"), o.optString("candidate")))
            }
        }
    }

    fun close() {
        transport?.close()
        runCatching { pc?.close() }
    }

    private fun bindChannel(channel: DataChannel) {
        val t = WebRtcTransport(channel)
        transport = t
        channel.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining()).also { buffer.data.get(it) }
                t.onMessage(bytes)
            }
            override fun onStateChange() {
                if (channel.state() == DataChannel.State.OPEN) {
                    Log.i(tag, "data channel open with $peerId")
                    onReady(t)
                }
            }
            override fun onBufferedAmountChange(p: Long) {}
        })
    }

    private fun setLocalAndSend(sdp: SessionDescription, type: String) {
        pc?.setLocalDescription(setObserver {}, sdp)
        signaling.send(peerId, type, sdp.description)
    }

    private fun sdpObserver(onCreated: (SessionDescription) -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) = onCreated(sdp)
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) { Log.w(tag, "sdp create failed: $error") }
        override fun onSetFailure(error: String?) { Log.w(tag, "sdp set failed: $error") }
    }

    private fun setObserver(onSet: () -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() = onSet()
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) { Log.w(tag, "set remote failed: $error") }
    }
}
