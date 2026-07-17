package com.example.triplog

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current
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
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css"/>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                html, body { margin: 0; padding: 0; height: 100%; width: 100%; }
                #map { height: 100%; width: 100%; }
                #status { position: absolute; top: 8px; left: 8px; z-index: 9999; background: rgba(255,255,255,0.9); padding: 4px 8px; border-radius: 4px; font-size: 12px; }
            </style>
        </head>
        <body>
            <div id="status">Loading map...</div>
            <div id="map"></div>
            <script>
                try {
                    document.getElementById('status').innerText = 'Initializing...';
                    var map = L.map('map').setView([$avgLat, $avgLng], 5);
                    document.getElementById('status').innerText = 'Loading tiles...';
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap contributors'
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
                    document.getElementById('status').innerText = 'Map ready!';
                    setTimeout(function(){ document.getElementById('status').style.display='none'; }, 2000);
                } catch(e) {
                    document.getElementById('status').innerText = 'Error: ' + e.message;
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(context) {
        val webView = createWebView(context)
        webViewRef.value = webView
        onDispose {
            webView.destroy()
        }
    }

    val wv = webViewRef.value
    if (wv != null) {
        AndroidView(
            factory = { wv },
            modifier = Modifier.fillMaxWidth().height(400.dp),
            update = { webView ->
                val file = File(context.cacheDir, "triplog_map.html")
                file.writeText(html)
                webView.loadUrl("file://${file.absolutePath}")
            }
        )
    }
}

private fun createWebView(context: Context): WebView {
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.allowFileAccess = true
        settings.userAgentString = settings.userAgentString.replace("; wv", "")
        webViewClient = WebViewClient()
    }
}
