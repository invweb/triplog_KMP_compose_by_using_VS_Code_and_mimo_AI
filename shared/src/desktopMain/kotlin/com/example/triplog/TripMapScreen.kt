package com.example.triplog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.io.File

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
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                html, body { margin: 0; padding: 0; height: 100%; }
                #map { height: 100vh; width: 100%; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$avgLat, $avgLng], 5);
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
            </script>
        </body>
        </html>
    """.trimIndent()

    var openMap by remember { mutableStateOf(false) }

    LaunchedEffect(openMap) {
        if (openMap) {
            val tmpFile = File.createTempFile("triplog_map_", ".html")
            tmpFile.deleteOnExit()
            tmpFile.writeText(html)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(tmpFile.toURI())
            }
            openMap = false
        }
    }

    if (trips.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No trips to display on map")
        }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.OutlinedButton(onClick = { openMap = true }) {
                Text("Open Map (${trips.size} trip${if (trips.size != 1) "s" else ""})")
            }
        }
    }
}
