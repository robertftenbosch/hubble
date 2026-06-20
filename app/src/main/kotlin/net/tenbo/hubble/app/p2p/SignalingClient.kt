package net.tenbo.hubble.app.p2p

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * WebSocket client for WebRTC signaling. Connects to `/signal/<myId>` and exchanges
 * JSON `{to, type, payload}` messages with peers (offer / answer / ice). Inbound
 * messages (stamped with `from` by the server) are delivered to [onSignal].
 */
class SignalingClient(
    private val baseWsUrl: String,
    private val myId: String,
    private val onSignal: (from: String, type: String, payload: String) -> Unit,
) {
    private val tag = "HubbleSignaling"
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url("$baseWsUrl/signal/$myId").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "signaling connected as $myId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val o = JSONObject(text)
                    if (o.has("from")) {
                        onSignal(o.getString("from"), o.optString("type"), o.optString("payload"))
                    }
                }.onFailure { Log.w(tag, "bad signal: ${it.message}") }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "signaling failed: ${t.message}")
            }
        })
    }

    fun send(to: String, type: String, payload: String) {
        val msg = JSONObject().put("to", to).put("type", type).put("payload", payload)
        socket?.send(msg.toString())
    }

    fun close() {
        socket?.close(1000, "bye")
        socket = null
    }
}
