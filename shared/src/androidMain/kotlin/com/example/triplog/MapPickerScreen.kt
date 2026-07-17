package com.example.triplog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Enter coordinates manually, or tap the button below to find them on OpenStreetMap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(onClick = {
                val url = "https://www.openstreetmap.org/#map=14/${selectedLat}/${selectedLng}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Open OpenStreetMap in browser")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = String.format("%.6f", selectedLat),
                onValueChange = { selectedLat = it.toDoubleOrNull() ?: selectedLat },
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = String.format("%.6f", selectedLng),
                onValueChange = { selectedLng = it.toDoubleOrNull() ?: selectedLng },
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onConfirm(selectedLat, selectedLng) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm location")
            }
        }
    }
}
