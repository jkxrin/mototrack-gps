package com.mototrack.gps.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mototrack.gps.domain.model.TrackPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * OpenStreetMap Compose Wrapper via osmdroid.
 * 100% Free - Zero Google Maps API keys or billing required.
 * Renders live tracking route polyline with glowing neon styling and auto-centering.
 */
@Composable
fun OsmMapView(
    routePoints: List<TrackPoint>,
    currentPoint: TrackPoint?,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    autoCenter: Boolean = true
) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(isInteractive)
            zoomController.setVisibility(
                if (isInteractive) org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                else org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
            )
            controller.setZoom(16.5)
        }
    }

    val polyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = AndroidColor.parseColor("#00E5FF") // Neon Cyan
            outlinePaint.strokeWidth = 10.0f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }
    }

    // Attach polyline once
    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(polyline)) {
            mapView.overlays.add(polyline)
        }
    }

    // Update Polyline points dynamically
    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val geoPoints = routePoints.map { GeoPoint(it.latitude, it.longitude) }
            polyline.setPoints(geoPoints)
            mapView.invalidate()
        }
    }

    // Auto-center smoothly on current GPS coordinate
    LaunchedEffect(currentPoint, autoCenter) {
        if (autoCenter && currentPoint != null) {
            val centerGeo = GeoPoint(currentPoint.latitude, currentPoint.longitude)
            mapView.controller.animateTo(centerGeo)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}
