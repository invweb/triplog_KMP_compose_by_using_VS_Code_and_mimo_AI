package com.example.triplog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(10.0)

                if (trips.isNotEmpty()) {
                    val avgLat = trips.map { it.lat }.average()
                    val avgLng = trips.map { it.lng }.average()
                    controller.setCenter(GeoPoint(avgLat, avgLng))

                    trips.forEach { trip ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(trip.lat, trip.lng)
                        marker.title = trip.title
                        marker.snippet = trip.city
                        overlays.add(marker)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapView.overlays.clear()
            trips.forEach { trip ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(trip.lat, trip.lng)
                marker.title = trip.title
                marker.snippet = trip.city
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        },
        onRelease = { mapView ->
            mapView.onDetach()
        }
    )
}
