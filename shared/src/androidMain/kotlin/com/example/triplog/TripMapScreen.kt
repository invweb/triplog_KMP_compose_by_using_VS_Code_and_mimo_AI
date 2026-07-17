package com.example.triplog

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(ctx: android.content.Context, html: String): WebView {
    return WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webViewClient = WebViewClient()
        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val markersJs = trips.joinToString(",") { trip ->
        "[${trip.lat},${trip.lng},\"${trip.title.replace("\"", "\\\"")}\",\"${trip.city.replace("\"", "\\\"")}\"]"
    }

    val avgLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
    val avgLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

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
                var map = L.map('map').setView([$avgLat, $avgLng], 5);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors'
                }).addTo(map);
                var markers = [$markersJs];
                markers.forEach(function(m) {
                    L.marker([m[0], m[1]]).addTo(map)
                        .bindPopup('<b>' + m[2] + '</b><br>' + m[3]);
                });
                if (markers.length > 0) {
                    var bounds = L.latLngBounds(markers.map(function(m) { return [m[0], m[1]]; }));
                    map.fitBounds(bounds, { padding: [50, 50] });
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx -> createWebView(ctx, html) },
        modifier = Modifier.fillMaxSize(),
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}
