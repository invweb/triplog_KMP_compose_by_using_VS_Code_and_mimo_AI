package com.example.triplog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        MapKitFactory.initialize(context)
        MapKitFactory.getInstance().onStart()
    }

    val mapView = remember {
        MapView(context).apply {
            val centerLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
            val centerLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

            mapWindow.map.move(
                CameraPosition(Point(centerLat, centerLng), 5.0f, 0.0f, 0.0f)
            )

            trips.forEach { trip ->
                mapWindow.map.mapObjects.addPlacemark(Point(trip.lat, trip.lng))
            }
        }
    }

    DisposableEffect(mapView) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}
