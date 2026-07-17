package com.example.triplog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.io.File
import java.net.URL
import java.util.Properties
import javax.imageio.ImageIO
import androidx.compose.ui.awt.SwingPanel
import javax.swing.JLabel

private fun getLastLocationFile(): File {
    val dir = File(System.getProperty("user.home"), ".trip_log")
    dir.mkdirs()
    return File(dir, "last_location.properties")
}

private fun saveLastLocation(lat: Double, lng: Double) {
    Properties().apply {
        setProperty("last_lat", lat.toString())
        setProperty("last_lng", lng.toString())
        getLastLocationFile().outputStream().use { store(it, null) }
    }
}

private fun getLastLocation(): Pair<Double, Double>? {
    return try {
        val props = Properties()
        getLastLocationFile().inputStream().use { props.load(it) }
        val lat = props.getProperty("last_lat")?.toDoubleOrNull()
        val lng = props.getProperty("last_lng")?.toDoubleOrNull()
        if (lat != null && lng != null) Pair(lat, lng) else null
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
) {
    val lastLocation = remember { getLastLocation() }
    val startLat = lastLocation?.first ?: initialLat
    val startLng = lastLocation?.second ?: initialLng

    var lat by remember { mutableStateOf(startLat) }
    var lng by remember { mutableStateOf(startLng) }
    var mapImage by remember { mutableStateOf<java.awt.image.BufferedImage?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(lat, lng) {
        isLoading = true
        try {
            val zoom = 12
            val n = Math.pow(2.0, zoom.toDouble())
            val x = ((lng + 180) / 360 * n).toInt()
            val latRad = Math.toRadians(lat)
            val y = ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n).toInt()
            val url = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
            mapImage = ImageIO.read(url)
        } catch (_: Exception) {
            mapImage = null
        }
        isLoading = false
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lastLocation != null) {
                Text(
                    "Last selected location loaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedButton(
                onClick = {
                    val html = """
                        <!DOCTYPE html>
                        <html><head><meta charset="utf-8">
                        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css"/>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
                        <style>html,body{margin:0;padding:0;height:100%}#map{height:100vh;width:100%}</style>
                        </head><body><div id="map"></div><script>
                        var map=L.map('map').setView([$lat,$lng],12);
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OSM'}).addTo(map);
                        var marker=L.marker([$lat,$lng]).addTo(map);
                        map.on('click',function(e){marker.setLatLng(e.latlng);document.getElementById('coord').innerText=e.latlng.lat.toFixed(6)+', '+e.latlng.lng.toFixed(6)});
                        </script></body></html>
                    """.trimIndent()
                    val f = File.createTempFile("triplog_picker_", ".html")
                    f.deleteOnExit()
                    f.writeText(html)
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(f.toURI())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open map in browser to find coordinates")
            }

            if (mapImage != null) {
                SwingPanel(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    factory = {
                        val label = JLabel()
                        label.icon = javax.swing.ImageIcon(mapImage!!)
                        label.horizontalAlignment = JLabel.CENTER
                        label
                    }
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            OutlinedTextField(
                value = String.format("%.6f", lat),
                onValueChange = { lat = it.toDoubleOrNull() ?: lat },
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = String.format("%.6f", lng),
                onValueChange = { lng = it.toDoubleOrNull() ?: lng },
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    saveLastLocation(lat, lng)
                    onConfirm(lat, lng)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm location")
            }
        }
    }
}
