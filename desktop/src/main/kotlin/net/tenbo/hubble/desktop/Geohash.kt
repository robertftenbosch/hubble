package net.tenbo.hubble.desktop

/** Minimal geohash decoder — coarse cell -> centre lat/lon for the activity layout. */
object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

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
}
