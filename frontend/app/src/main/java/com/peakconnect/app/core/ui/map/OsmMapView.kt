package com.peakconnect.app.core.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peakconnect.app.BuildConfig
import com.peakconnect.app.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Reusable osmdroid MapView wrapped in Compose with proper lifecycle handling.
 * Configured with OSM Mapnik tiles, 50MB disk tile cache, and OpenStreetMap attribution.
 * Used in Activity Detail and Guide experience (Task 12).
 */
@Composable
fun OsmMapView(
    center: GeoPoint?,
    modifier: Modifier = Modifier,
    zoom: Double = 14.0,
    markerPositions: List<GeoPoint> = center?.let { listOf(it) } ?: emptyList(),
    markerTitles: List<String> = emptyList(),
    interactive: Boolean = true,
    onMapTap: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration once per application
    remember(context) {
        initOsmdroidConfiguration(context)
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            isTilesScaledToDpi = true

            if (!interactive) {
                // Non-interactive display: disable scrolling and zooming touches
                setOnTouchListener { _, _ -> true }
            }
        }
    }

    // Handle map lifecycle
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                view.overlays.clear()

                // Add tap overlay if callback provided
                if (interactive && onMapTap != null) {
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapTap(p)
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    })
                    view.overlays.add(eventsOverlay)
                }

                // Add markers
                markerPositions.forEachIndexed { index, geoPoint ->
                    val marker = Marker(view).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        if (index < markerTitles.size) {
                            title = markerTitles[index]
                        }
                    }
                    view.overlays.add(marker)
                }

                // Set zoom & center
                center?.let {
                    view.controller.setZoom(zoom)
                    view.controller.setCenter(it)
                }
                view.invalidate()
            }
        )

        // Required OpenStreetMap Attribution Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.map_attribution_osm),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White
            )
        }
    }
}

private val osmdroidLock = Any()
private var isOsmdroidConfigured = false

private fun initOsmdroidConfiguration(context: Context) {
    if (!isOsmdroidConfigured) {
        synchronized(osmdroidLock) {
            if (!isOsmdroidConfigured) {
                val appConfig = Configuration.getInstance()
                appConfig.userAgentValue = "${context.packageName}/${BuildConfig.VERSION_NAME}"

                val osmdroidBasePath = File(context.cacheDir, "osmdroid")
                val osmdroidTileCache = File(osmdroidBasePath, "tiles")
                appConfig.osmdroidBasePath = osmdroidBasePath
                appConfig.osmdroidTileCache = osmdroidTileCache
                appConfig.tileFileSystemCacheMaxBytes = 50L * 1024L * 1024L // 50MB
                appConfig.tileFileSystemCacheTrimBytes = 40L * 1024L * 1024L // 40MB
                isOsmdroidConfigured = true
            }
        }
    }
}
