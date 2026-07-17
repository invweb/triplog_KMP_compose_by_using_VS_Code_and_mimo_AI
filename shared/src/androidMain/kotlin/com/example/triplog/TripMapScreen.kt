package com.example.triplog

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

@SuppressLint("MissingPermission")
@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current
    var centered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MapKitFactory.initialize(context)
        MapKitFactory.getInstance().onStart()
    }

    val mapView = remember {
        MapView(context).apply {
            val startLat = if (trips.isNotEmpty()) trips.first().lat else 59.9343
            val startLng = if (trips.isNotEmpty()) trips.first().lng else 30.3351
            mapWindow.map.move(CameraPosition(Point(startLat, startLng), 12.0f, 0.0f, 0.0f))

            trips.forEach { trip ->
                mapWindow.map.mapObjects.addPlacemark(Point(trip.lat, trip.lng))
            }
        }
    }

    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = locationManager.getBestProvider(
            android.location.Criteria().apply {
                accuracy = android.location.Criteria.ACCURACY_FINE
                isCostAllowed = false
            },
            true
        ) ?: LocationManager.GPS_PROVIDER

        locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                if (!centered) {
                    mapView.mapWindow.map.move(
                        CameraPosition(Point(location.latitude, location.longitude), 12.0f, 0.0f, 0.0f)
                    )
                    centered = true
                }
            }
            @Deprecated("Deprecated")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, Looper.getMainLooper())
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
