package com.example.triplog

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun TripMapScreen(trips: List<Trip>) {
    val centerLat = if (trips.isNotEmpty()) trips.map { it.lat }.average() else 55.75
    val centerLng = if (trips.isNotEmpty()) trips.map { it.lng }.average() else 37.62

    val markersParam = if (trips.isNotEmpty()) {
        trips.joinToString("~") { "${it.lng},${it.lat},pm2rdm${it.title}" }
    } else ""

    val url = if (trips.isNotEmpty()) {
        "https://yandex.ru/maps/?ll=${centerLng},${centerLat}&z=5&pt=${markersParam}"
    } else {
        "https://yandex.ru/maps/?ll=${centerLng},${centerLat}&z=5"
    }

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
        modifier = Modifier.fillMaxWidth().height(400.dp),
        update = { it.loadUrl(url) }
    )
}
