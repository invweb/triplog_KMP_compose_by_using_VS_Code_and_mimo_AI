package com.example.triplog

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }
    var gpsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MapKitFactory.initialize(context)
    }

    val mapView = remember {
        MapView(context).apply {
            mapWindow.map.move(
                CameraPosition(Point(initialLat, initialLng), 12.0f, 0.0f, 0.0f)
            )

            var placemark = mapWindow.map.mapObjects.addPlacemark(Point(initialLat, initialLng))

            mapWindow.map.addInputListener(object : InputListener {
                override fun onMapTap(map: Map, point: Point) {
                    selectedLat = point.latitude
                    selectedLng = point.longitude
                    map.mapObjects.remove(placemark)
                    placemark = map.mapObjects.addPlacemark(point)
                }
                override fun onMapLongTap(map: Map, point: Point) {}
            })
        }
    }

    LaunchedEffect(Unit) {
        try {
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
                    selectedLat = location.latitude
                    selectedLng = location.longitude
                    mapView.mapWindow.map.move(
                        CameraPosition(Point(location.latitude, location.longitude), 14.0f, 0.0f, 0.0f)
                    )
                    gpsReady = true
                }
                @Deprecated("Deprecated")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, Looper.getMainLooper())
        } catch (_: SecurityException) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select location") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lat: ${String.format("%.6f", selectedLat)}  Lng: ${String.format("%.6f", selectedLng)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!gpsReady) {
                        Text(
                            "Determining your location...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Your location detected. Tap map to adjust.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onConfirm(selectedLat, selectedLng) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm location")
                    }
                }
            }
        }
    }
}
