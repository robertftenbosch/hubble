package net.tenbo.hubble.app.net

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

/**
 * Coarse location via the framework LocationManager (no Play Services dependency).
 * Returns a last-known fix; callers turn it into a coarse geohash for the heatmap, so
 * precise coordinates never leave the device.
 */
object Locator {
    @SuppressLint("MissingPermission") // caller holds ACCESS_COARSE_LOCATION
    fun coarse(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
            if (loc != null) return loc.latitude to loc.longitude
        }
        return null
    }
}
