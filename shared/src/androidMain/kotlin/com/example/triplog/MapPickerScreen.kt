package com.example.triplog

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
) {
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }
    val stateHolder = remember { MapPickerStateHolder() }

    LaunchedEffect(Unit) {
        stateHolder.onLocationSelected = { lat, lng ->
            selectedLat = lat
            selectedLng = lng
        }
    }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body { margin: 0; padding: 0; height: 100%; width: 100%; }
                #map { height: 100%; width: 100%; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$initialLat, $initialLng], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors'
                }).addTo(map);
                var marker = L.marker([$initialLat, $initialLng]).addTo(map);
                map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    window.AndroidBridge.onLocationSelected(e.latlng.lat, e.latlng.lng);
                });
            </script>
        </body>
        </html>
    """.trimIndent()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        webViewClient = WebViewClient()
                        addJavascriptInterface(stateHolder, "AndroidBridge")
                        loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Lat: ${String.format("%.6f", selectedLat)}  Lng: ${String.format("%.6f", selectedLng)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tap on the map to select a location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

class MapPickerStateHolder {
    var onLocationSelected: (Double, Double) -> Unit = { _, _ -> }

    @JavascriptInterface
    fun onLocationSelected(lat: Double, lng: Double) {
        onLocationSelected(lat, lng)
    }
}
