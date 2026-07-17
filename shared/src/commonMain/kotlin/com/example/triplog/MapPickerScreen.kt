package com.example.triplog

import androidx.compose.runtime.Composable

@Composable
expect fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
)
