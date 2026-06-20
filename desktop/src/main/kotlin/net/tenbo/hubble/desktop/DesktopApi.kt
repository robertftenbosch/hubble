package net.tenbo.hubble.desktop

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

data class HeatCell(val cell: String, val count: Int)

/**
 * Desktop HTTP client for the Hubble relay/heatmap — same endpoints as the phone.
 *
 * The default server URL can be overridden at launch time with the `HUBBLE_SERVER`
 * environment variable. Useful for pointing an installed copy at a LAN test server or a
 * production host without rebuilding (e.g. `setx HUBBLE_SERVER http://192.168.x.y:4000`
 * on Windows, or an `Environment=` line in a Linux .desktop launcher).
 */
class DesktopApi(
    private val baseUrl: String = System.getenv("HUBBLE_SERVER") ?: "http://127.0.0.1:4000",
) {
    private val http = HttpClient.newHttpClient()
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()

    fun health(): Boolean = runCatching { send("GET", "/health", null).statusCode() == 200 }.getOrDefault(false)

    fun deposit(mailboxId: String, envelope: ByteArray) {
        send("POST", "/mailbox/$mailboxId", JSONObject().put("envelope", b64.encodeToString(envelope)).toString())
    }

    fun collect(mailboxId: String): List<ByteArray> {
        val body = send("GET", "/mailbox/$mailboxId", null).body()
        val arr = JSONObject(body).optJSONArray("envelopes") ?: JSONArray()
        return (0 until arr.length()).map { unb64.decode(arr.getString(it)) }
    }

    fun postBeacon(geohash: String) {
        send("POST", "/beacon", JSONObject().put("geohash", geohash).toString())
    }

    fun heatmap(): List<HeatCell> {
        val arr = JSONArray(send("GET", "/heatmap", null).body())
        return (0 until arr.length()).map { val o = arr.getJSONObject(it); HeatCell(o.getString("cell"), o.getInt("count")) }
    }

    private fun send(method: String, path: String, body: String?): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Content-Type", "application/json")
        when (method) {
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
            else -> builder.GET()
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
