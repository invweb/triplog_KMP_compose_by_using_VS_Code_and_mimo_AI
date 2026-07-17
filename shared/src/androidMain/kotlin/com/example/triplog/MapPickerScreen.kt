package com.example.triplog

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.mapview.MapView

private fun saveLastLocation(context: Context, lat: Double, lng: Double) {
    context.getSharedPreferences("location_store", Context.MODE_PRIVATE)
        .edit()
        .putFloat("last_lat", lat.toFloat())
        .putFloat("last_lng", lng.toFloat())
        .apply()
}

private fun getLastLocation(context: Context): Pair<Double, Double> {
    val prefs = context.getSharedPreferences("location_store", Context.MODE_PRIVATE)
    return Pair(
        prefs.getFloat("last_lat", 0f).toDouble(),
        prefs.getFloat("last_lng", 0f).toDouble()
    )
}

private fun hasLastLocation(context: Context): Boolean {
    return context.getSharedPreferences("location_store", Context.MODE_PRIVATE)
        .contains("last_lat")
}

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
    val lastLocation = remember { if (hasLastLocation(context)) getLastLocation(context) else null }
    val startLat = lastLocation?.first ?: initialLat
    val startLng = lastLocation?.second ?: initialLng

    var selectedLat by remember { mutableStateOf(startLat) }
    var selectedLng by remember { mutableStateOf(startLng) }
    var gpsLat by remember { mutableDoubleStateOf(startLat) }
    var gpsLng by remember { mutableDoubleStateOf(startLng) }
    var gpsReady by remember { mutableStateOf(false) }

    val stateHolder = remember { PickerStateHolder() }
    stateHolder.onLocationPicked = { lat, lng ->
        selectedLat = lat
        selectedLng = lng
    }

    val mapView = remember {
        MapView(context).apply {
            mapWindow.map.move(
                CameraPosition(Point(startLat, startLng), 14.0f, 0.0f, 0.0f)
            )
        }
    }

    LaunchedEffect(Unit) {
        if (lastLocation == null) {
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
                        gpsLat = location.latitude
                        gpsLng = location.longitude
                        stateHolder.onLocationPicked(location.latitude, location.longitude)
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
        } else {
            gpsLat = lastLocation.first
            gpsLng = lastLocation.second
            gpsReady = true
        }
    }

    LaunchedEffect(mapView) {
        mapView.mapWindow.map.addCameraListener(object : CameraListener {
            override fun onCameraPositionChanged(
                map: com.yandex.mapkit.map.Map,
                position: CameraPosition,
                reason: CameraUpdateReason,
                finished: Boolean
            ) {
                if (finished) {
                    stateHolder.onLocationPicked(position.target.latitude, position.target.longitude)
                }
            }
        })
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            if (gpsReady) {
                FloatingActionButton(
                    onClick = {
                        stateHolder.onLocationPicked(gpsLat, gpsLng)
                        mapView.mapWindow.map.move(
                            CameraPosition(Point(gpsLat, gpsLng), 14.0f, 0.0f, 0.0f)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "My location")
                }
            }

            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lat: ${String.format("%.6f", selectedLat)}  Lng: ${String.format("%.6f", selectedLng)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Tap map or drag to select. Use  button for GPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            saveLastLocation(context, selectedLat, selectedLng)
                            onConfirm(selectedLat, selectedLng)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm location")
                    }
                }
            }
        }
    }
}

private class PickerStateHolder {
    var onLocationPicked: (Double, Double) -> Unit = { _, _ -> }
}
