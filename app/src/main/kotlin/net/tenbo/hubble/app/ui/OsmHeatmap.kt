package net.tenbo.hubble.app.ui

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import net.tenbo.hubble.app.net.Geohash
import net.tenbo.hubble.app.net.HeatCell
import net.tenbo.hubble.app.net.Locator
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * The activity heatmap on a real OpenStreetMap (osmdroid, no API key). Each k-anonymous
 * cell is a translucent signal-blue circle sized by its count — never a precise pin.
 *
 * When location is granted the map opens on the user's own coarse position with a
 * "you are here" marker; otherwise it falls back to fitting the visible cells (once).
 * The user's coordinates only drive the local camera + marker — nothing extra is sent.
 */
@Composable
fun OsmHeatmap(cells: List<HeatCell>, locationGranted: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val map = remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "osmtiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
        }
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }

    // The user's own coarse fix, acquired once permission is available. Drives the
    // initial camera and the "you are here" marker. Stays null without a fix.
    var userPoint by remember { mutableStateOf<GeoPoint?>(null) }
    // True once we've decided whether a fix exists (granted+looked, or not granted), so
    // the data fallback fit doesn't fire before we know if we'll center on the user.
    var fixResolved by remember { mutableStateOf(false) }
    LaunchedEffect(locationGranted) {
        if (locationGranted && userPoint == null) {
            Locator.coarse(context)?.let { userPoint = GeoPoint(it.first, it.second) }
        }
        fixResolved = true
    }
    LaunchedEffect(userPoint) {
        userPoint?.let {
            map.controller.setZoom(14.0)
            map.controller.animateTo(it)
        }
    }
    // Fit-to-data is the no-location fallback and runs only once, so heatmap refreshes
    // never yank the view away from where the user has panned/zoomed to.
    val didInitialFit = remember { booleanArrayOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { map },
        update = { mv ->
            mv.overlays.clear()
            if (cells.isNotEmpty()) {
                val pts = cells.map { it to Geohash.decode(it.cell) }
                val maxCount = cells.maxOf { it.count }.toFloat().coerceAtLeast(1f)
                pts.forEach { (cell, ll) ->
                    val center = GeoPoint(ll.first, ll.second)
                    val radiusM = 250.0 + 150.0 * cell.count
                    val alpha = (55 + 120 * (cell.count / maxCount)).toInt().coerceIn(55, 200)
                    Polygon(mv).apply {
                        setPoints(Polygon.pointsAsCircle(center, radiusM))
                        fillPaint.color = AndroidColor.argb(alpha, 0x24, 0x36, 0xE8)
                        fillPaint.style = android.graphics.Paint.Style.FILL
                        outlinePaint.color = AndroidColor.argb(180, 0x24, 0x36, 0xE8)
                        outlinePaint.strokeWidth = 3f
                    }.also { mv.overlays.add(it) }
                }
                if (userPoint == null && fixResolved && !didInitialFit[0]) {
                    val lats = pts.map { it.second.first }
                    val lons = pts.map { it.second.second }
                    mv.post {
                        mv.zoomToBoundingBox(
                            BoundingBox(lats.max() + 0.03, lons.max() + 0.03, lats.min() - 0.03, lons.min() - 0.03),
                            false, 96,
                        )
                    }
                    didInitialFit[0] = true
                }
            }
            // "You are here" — your own position, on your own device only.
            userPoint?.let { up ->
                Marker(mv).apply {
                    position = up
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                }.also { mv.overlays.add(it) }
            }
            mv.invalidate()
        },
    )
}
