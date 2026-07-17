package com.example.triplog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val context = LocalContext.current

    val centerLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
    val centerLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

    val markersParam = if (trips.isNotEmpty()) {
        trips.joinToString("&") { trip ->
            "marker=${trip.lat},${trip.lng}"
        } + "&apiKey=disabled"
    } else ""

    val url = "https://www.openstreetmap.org/#map=5/${centerLat}/${centerLng}"

    Box(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Map available in browser")

            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }) {
                Text("Open map in browser")
            }

            if (trips.isNotEmpty()) {
                trips.forEach { trip ->
                    TextButton(onClick = {
                        val tripUrl = "https://www.openstreetmap.org/?mlat=${trip.lat}&mlon=${trip.lng}#map=14/${trip.lat}/${trip.lng}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tripUrl))
                        context.startActivity(intent)
                    }) {
                        Text("${trip.title} (${trip.city})")
                    }
                }
            }
        }
    }
}
