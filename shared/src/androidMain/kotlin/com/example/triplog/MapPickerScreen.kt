package com.example.triplog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
    val zoom = 12
    val tileSize = 256
    val n = Math.pow(2.0, zoom.toDouble())

    val centerLat by remember { mutableStateOf(initialLat) }
    val centerLng by remember { mutableStateOf(initialLng) }

    val cols = 3
    val rows = 3
    val clatRad = Math.toRadians(centerLat)
    val cx = ((centerLng + 180) / 360 * n).toInt()
    val cy = ((1 - Math.log(Math.tan(clatRad) + 1 / Math.cos(clatRad)) / Math.PI) / 2 * n).toInt()
    val startX = cx - cols / 2
    val startY = cy - rows / 2

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val combined = Bitmap.createBitmap(tileSize * cols, tileSize * rows, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(combined)
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val url = URL("https://tile.openstreetmap.org/$zoom/${startX + col}/${startY + row}.png")
                        val conn = url.openConnection()
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val tile = BitmapFactory.decodeStream(conn.getInputStream())
                        conn.getInputStream().close()
                        if (tile != null) {
                            canvas.drawBitmap(tile, (col * tileSize).toFloat(), (row * tileSize).toFloat(), null)
                            tile.recycle()
                        }
                    }
                }
                bitmap = combined
            } catch (_: Exception) {
            }
        }
    }

    fun pixelToLatLng(px: Float, py: Float, viewWidth: Int, viewHeight: Int): Pair<Double, Double> {
        val mapWidth = tileSize * cols
        val mapHeight = tileSize * rows
        val mapPx = px / viewWidth * mapWidth
        val mapPy = py / viewHeight * mapHeight
        val lngDelta = (mapPx - mapWidth / 2) / tileSize / n * 360
        val latDelta = -(mapPy - mapHeight / 2) / tileSize / n * 180 / Math.PI
        val latRad = Math.toRadians(centerLat) + latDelta
        val lat = Math.toDegrees(Math.atan(Math.sinh(latRad)))
        val lng = centerLng + lngDelta
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val bmp = bitmap
            if (bmp != null) {
                val density = LocalDensity.current
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
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading map...")
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
