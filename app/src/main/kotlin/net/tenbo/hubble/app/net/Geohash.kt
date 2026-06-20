package net.tenbo.hubble.app.net

/**
 * Minimal geohash decoder — turns a coarse cell (e.g. "u173zw") into its centre
 * lat/lon so the activity view can lay cells out roughly geographically. We only
 * ever decode the coarse cells the server returns; no precise location is involved.
 */
object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    /** Returns (lat, lon) of the cell centre. */
    fun decode(hash: String): Pair<Double, Double> {
        var latLo = -90.0; var latHi = 90.0
        var lonLo = -180.0; var lonHi = 180.0
        var even = true
        for (c in hash.lowercase()) {
            val idx = BASE32.indexOf(c)
            if (idx < 0) continue
            for (bit in 4 downTo 0) {
                val on = (idx shr bit) and 1 == 1
                if (even) {
                    val mid = (lonLo + lonHi) / 2
                    if (on) lonLo = mid else lonHi = mid
                } else {
                    val mid = (latLo + latHi) / 2
                    if (on) latLo = mid else latHi = mid
                }
                even = !even
            }
        }
        return (latLo + latHi) / 2 to (lonLo + lonHi) / 2
    }

    /** Encode lat/lon to a geohash of [precision] chars (default 6 ≈ 1.2 km cell). */
    fun encode(lat: Double, lon: Double, precision: Int = 6): String {
        var latLo = -90.0; var latHi = 90.0
        var lonLo = -180.0; var lonHi = 180.0
        var even = true
        var bit = 0
        var idx = 0
        val out = StringBuilder()
        while (out.length < precision) {
            if (even) {
                val mid = (lonLo + lonHi) / 2
                if (lon >= mid) { idx = idx * 2 + 1; lonLo = mid } else { idx *= 2; lonHi = mid }
            } else {
                val mid = (latLo + latHi) / 2
                if (lat >= mid) { idx = idx * 2 + 1; latLo = mid } else { idx *= 2; latHi = mid }
            }
            even = !even
            if (bit < 4) bit++ else { out.append(BASE32[idx]); bit = 0; idx = 0 }
        }
        return out.toString()
    }
}
