package com.example.triplog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "TripMap"

private fun downloadTile(zoom: Int, x: Int, y: Int): Bitmap? {
    val url = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
    val conn = url.openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "TripLog/1.0 (Android)")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.connect()
        Log.d(TAG, "Downloading tile $zoom/$x/$y... HTTP ${conn.responseCode}")
        if (conn.responseCode == 200) {
            val data = conn.inputStream.readBytes()
            Log.d(TAG, "Tile $zoom/$x/$y: ${data.size} bytes")
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        } else {
            Log.e(TAG, "Tile $zoom/$x/$y: HTTP ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Tile $zoom/$x/$y: ${e.message}")
    } finally {
        conn.disconnect()
    }
    return null
}

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf("Starting...") }

    val centerLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
    val centerLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

    LaunchedEffect(centerLat, centerLng) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                val zoom = 5
                val tileSize = 256
                val n = Math.pow(2.0, zoom.toDouble())
                val cx = ((centerLng + 180) / 360 * n).toInt()
                val clatRad = Math.toRadians(centerLat)
                val cy = ((1 - Math.log(Math.tan(clatRad) + 1 / Math.cos(clatRad)) / Math.PI) / 2 * n).toInt()

                val cols = 3
                val rows = 3
                val startX = cx - 1
                val startY = cy - 1

                Log.d(TAG, "zoom=$zoom center=$cx/$cy tiles=$startX..${startX+2}/$startY..${startY+2}")

                val combined = Bitmap.createBitmap(tileSize * cols, tileSize * rows, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(combined)
                canvas.drawColor(Color.rgb(230, 230, 230))

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val tx = startX + col
                        val ty = startY + row
                        progress = "Downloading tile ${col + row * cols + 1}/9..."
                        val tile = downloadTile(zoom, tx, ty)
                        if (tile != null) {
                            canvas.drawBitmap(tile, (col * tileSize).toFloat(), (row * tileSize).toFloat(), null)
                            tile.recycle()
                        }
                    }
                }

                if (trips.isNotEmpty()) {
                    val mapWidth = tileSize * cols
                    val mapHeight = tileSize * rows
                    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; style = Paint.Style.FILL }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
                    }

                    trips.forEachIndexed { index, trip ->
                        val px = (mapWidth / 2 + ((trip.lng + 180) / 360 * n - (centerLng + 180) / 360 * n) * tileSize).toInt()
                        val latRad2 = Math.toRadians(trip.lat)
                        val cLatRad2 = Math.toRadians(centerLat)
                        val py = (mapHeight / 2 + ((1 - Math.log(Math.tan(latRad2) + 1 / Math.cos(latRad2)) / Math.PI) / 2 * n - (1 - Math.log(Math.tan(cLatRad2) + 1 / Math.cos(cLatRad2)) / Math.PI) / 2 * n) * tileSize).toInt()
                        canvas.drawCircle(px.toFloat(), py.toFloat(), 18f, borderPaint)
                        canvas.drawCircle(px.toFloat(), py.toFloat(), 15f, markerPaint)
                        canvas.drawText("${index + 1}", px.toFloat(), py.toFloat() + 8f, textPaint)
                    }
                }

                Log.d(TAG, "Map complete")
                combined
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                errorMsg = e.message
                null
            }
        }
        result = bmp
    }

    when {
        result != null -> {
            Image(
                bitmap = result!!.asImageBitmap(),
                contentDescription = "Trip map",
                modifier = Modifier.fillMaxWidth().height(400.dp)
            )
        }
        errorMsg != null -> {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                Text("Map error: $errorMsg")
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                Text(progress)
            }
        }
    }
}
