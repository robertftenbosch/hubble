package net.tenbo.hubble.app.net

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin HTTP client for the Hubble server (heatmap + relay). Uses HttpURLConnection
 * (no extra deps). All calls are blocking and must run off the main thread.
 *
 * [baseUrl] defaults to the adb-reverse loopback used during on-device dev; point it
 * at the real server in production. The relay only ever sees opaque base64 envelopes.
 */
class HubbleApi(private val baseUrl: String = "http://127.0.0.1:4000") {

    /** Deposit an opaque encrypted envelope into a recipient's mailbox. */
    fun deposit(mailboxId: String, envelope: ByteArray) {
        val body = JSONObject().put("envelope", Base64.encodeToString(envelope, Base64.NO_WRAP))
        request("POST", "/mailbox/$mailboxId", body.toString())
    }

    /** Collect (and drain) all pending envelopes for our mailbox. */
    fun collect(mailboxId: String): List<ByteArray> {
        val (_, text) = request("GET", "/mailbox/$mailboxId", null)
        val arr = JSONObject(text).optJSONArray("envelopes") ?: JSONArray()
        return (0 until arr.length()).map { Base64.decode(arr.getString(it), Base64.NO_WRAP) }
    }

    /** Post an anonymous coarse presence beacon. */
    fun postBeacon(geohash: String) {
        request("POST", "/beacon", JSONObject().put("geohash", geohash).toString())
    }

    /** Fetch the coarse activity heatmap cells. */
    fun heatmap(): List<HeatCell> {
        val (_, text) = request("GET", "/heatmap", null)
        val arr = JSONArray(text)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it); HeatCell(o.getString("cell"), o.getInt("count"))
        }
    }

    private fun request(method: String, path: String, body: String?): Pair<Int, String> {
        val conn = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 5000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) conn.outputStream.use { os: OutputStream -> os.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }
}

data class HeatCell(val cell: String, val count: Int)
