package com.example.triplog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private fun latLngToTile(lat: Double, lng: Double, zoom: Int): Triple<Int, Int, Double> {
    val n = Math.pow(2.0, zoom.toDouble())
    val x = ((lng + 180) / 360 * n).toInt()
    val latRad = Math.toRadians(lat)
    val y = ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n).toInt()
    val xFrac = (lng + 180) / 360 * n - x
    val yFrac = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n - y
    return Triple(x, y, 0.0)
}

private fun latLngToPixel(lat: Double, lng: Double, zoom: Int, mapWidth: Int, mapHeight: Int, centerLat: Double, centerLng: Double): Pair<Int, Int> {
    val tileSize = 256
    val n = Math.pow(2.0, zoom.toDouble())
    val cx = (lng + 180) / 360 * n
    val latRad = Math.toRadians(lat)
    val cy = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n
    val clatRad = Math.toRadians(centerLat)
    val ccx = (centerLng + 180) / 360 * n
    val ccy = (1 - Math.log(Math.tan(clatRad) + 1 / Math.cos(clatRad)) / Math.PI) / 2 * n
    val px = (mapWidth / 2 + (cx - ccx) * tileSize).toInt()
    val py = (mapHeight / 2 + (cy - ccy) * tileSize).toInt()
    return Pair(px, py)
}

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val zoom = 5

    val centerLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
    val centerLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

    val tileSize = 256
    val n = Math.pow(2.0, zoom.toDouble())
    val cx = ((centerLng + 180) / 360 * n).toInt()
    val clatRad = Math.toRadians(centerLat)
    val cy = ((1 - Math.log(Math.tan(clatRad) + 1 / Math.cos(clatRad)) / Math.PI) / 2 * n).toInt()

    val cols = 3
    val rows = 3
    val startX = cx - cols / 2
    val startY = cy - rows / 2

    LaunchedEffect(centerLat, centerLng) {
        withContext(Dispatchers.IO) {
            try {
                val combined = Bitmap.createBitmap(tileSize * cols, tileSize * rows, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(combined)

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val tx = startX + col
                        val ty = startY + row
                        val url = URL("https://tile.openstreetmap.org/$zoom/$tx/$ty.png")
                        val conn = url.openConnection()
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val stream = conn.getInputStream()
                        val tile = BitmapFactory.decodeStream(stream)
                        stream.close()
                        if (tile != null) {
                            canvas.drawBitmap(tile, (col * tileSize).toFloat(), (row * tileSize).toFloat(), null)
                            tile.recycle()
                        }
                    }
                }

                val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.RED
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }

                val mapWidth = tileSize * cols
                val mapHeight = tileSize * rows

                trips.forEachIndexed { index, trip ->
                    val px = (mapWidth / 2 + ((trip.lng + 180) / 360 * n - (centerLng + 180) / 360 * n) * tileSize).toInt()
                    val latRad2 = Math.toRadians(trip.lat)
                    val cLatRad2 = Math.toRadians(centerLat)
                    val py = (mapHeight / 2 + ((1 - Math.log(Math.tan(latRad2) + 1 / Math.cos(latRad2)) / Math.PI) / 2 * n - (1 - Math.log(Math.tan(cLatRad2) + 1 / Math.cos(cLatRad2)) / Math.PI) / 2 * n) * tileSize).toInt()

                    canvas.drawCircle(px.toFloat(), py.toFloat(), 18f, borderPaint)
                    canvas.drawCircle(px.toFloat(), py.toFloat(), 15f, markerPaint)
                    canvas.drawText("${index + 1}", px.toFloat(), py.toFloat() + 8f, textPaint)
                }

                bitmap = combined
            } catch (_: Exception) {
            }
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Trip map",
            modifier = Modifier.fillMaxWidth().height(400.dp)
        )
    } else {
            androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text("Loading map...")
        }
    }
}
