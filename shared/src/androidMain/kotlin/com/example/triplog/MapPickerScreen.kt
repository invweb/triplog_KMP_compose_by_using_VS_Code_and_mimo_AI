package com.example.triplog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "TripPicker"

private fun downloadPickerTile(zoom: Int, x: Int, y: Int): Bitmap? {
    val url = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
    val conn = url.openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "TripLog/1.0 (Android)")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.connect()
        if (conn.responseCode == 200) {
            val data = conn.inputStream.readBytes()
            Log.d(TAG, "Tile $zoom/$x/$y: ${data.size} bytes")
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Tile $zoom/$x/$y: ${e.message}")
    } finally {
        conn.disconnect()
    }
    return null
}

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
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var error by remember { mutableStateOf<String?>(null) }
    val zoom = 12
    val tileSize = 256
    val n = Math.pow(2.0, zoom.toDouble())
    val cols = 3
    val rows = 3

    val clatRad = Math.toRadians(initialLat)
    val cx = ((initialLng + 180) / 360 * n).toInt()
    val cy = ((1 - Math.log(Math.tan(clatRad) + 1 / Math.cos(clatRad)) / Math.PI) / 2 * n).toInt()
    val startX = cx - 1
    val startY = cy - 1

    LaunchedEffect(Unit) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                val combined = Bitmap.createBitmap(tileSize * cols, tileSize * rows, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(combined)
                canvas.drawColor(Color.rgb(230, 230, 230))
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val tile = downloadPickerTile(zoom, startX + col, startY + row)
                        if (tile != null) {
                            canvas.drawBitmap(tile, (col * tileSize).toFloat(), (row * tileSize).toFloat(), null)
                            tile.recycle()
                        }
                    }
                }
                combined
            } catch (e: Exception) {
                error = e.message
                null
            }
        }
        bitmap = bmp
    }

    fun pixelToLatLng(px: Float, py: Float, viewWidth: Int, viewHeight: Int): Pair<Double, Double> {
        val mapWidth = tileSize * cols
        val mapHeight = tileSize * rows
        val mapPx = px / viewWidth * mapWidth
        val mapPy = py / viewHeight * mapHeight
        val lngDelta = (mapPx - mapWidth / 2) / tileSize / n * 360
        val latRad2 = Math.toRadians(initialLat) - (mapPy - mapHeight / 2) / tileSize / n * Math.PI / 2
        val lat = Math.toDegrees(Math.atan(Math.sinh(latRad2)))
        val lng = initialLng + lngDelta
        return Pair(lat, lng)
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
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Map - tap to select",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { mapSize = it }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val down = event.changes.firstOrNull()
                                    if (down != null && down.pressed) {
                                        val (lat, lng) = pixelToLatLng(
                                            down.position.x, down.position.y,
                                            mapSize.width, mapSize.height
                                        )
                                        selectedLat = lat
                                        selectedLng = lng
                                    }
                                }
                            }
                        }
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(error?.let { "Error: $it" } ?: "Loading map...")
                }
            }
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lat: ${String.format("%.6f", selectedLat)}  Lng: ${String.format("%.6f", selectedLng)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Tap on the map to select a location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = { onConfirm(selectedLat, selectedLng) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirm location")
                    }
                }
            }
        }
    }
}
