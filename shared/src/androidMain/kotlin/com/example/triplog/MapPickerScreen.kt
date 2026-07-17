package com.example.triplog

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
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
            val url = "https://yandex.ru/maps/?ll=${selectedLng},${selectedLat}&z=14&pt=${selectedLng},${selectedLat},pm2rdm"

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webViewClient = WebViewClient()
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                update = { it.loadUrl(url) }
            )

            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Button(onClick = { onConfirm(selectedLat, selectedLng) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirm location")
                    }
                }
            }
        }
    }
}
