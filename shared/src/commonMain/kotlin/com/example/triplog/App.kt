package com.example.triplog

import androidx.compose.runtime.*

sealed class Screen {
    data object List : Screen()
    data class Detail(val index: Int) : Screen()
    data object Add : Screen()
    data class MapPicker(val callbackId: Int) : Screen()
    data object Settings : Screen()
}

@Composable
fun TripLogApp(
    trips: List<Trip>,
    onAddTrip: (Trip) -> Unit,
    onDeleteTrip: (Trip) -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.List) }
    var pendingLat by remember { mutableDoubleStateOf(59.9343) }
    var pendingLng by remember { mutableDoubleStateOf(30.3351) }
    var mapPickerCallback by remember { mutableStateOf<((Double, Double) -> Unit)?>(null) }

    when (val screen = currentScreen) {
        is Screen.List -> {
            TripListScreen(
                trips = trips,
                onTripClick = { trip ->
                    val index = trips.indexOf(trip)
                    currentScreen = Screen.Detail(index)
                },
                onAddTrip = { currentScreen = Screen.Add }
            )
        }
        is Screen.Detail -> {
            if (screen.index in trips.indices) {
                TripDetailScreen(
                    trip = trips[screen.index],
                    onBack = { currentScreen = Screen.List },
                    onDelete = { trip ->
                        onDeleteTrip(trip)
                        currentScreen = Screen.List
                    }
                )
            } else {
                currentScreen = Screen.List
            }
        }
        is Screen.Add -> {
            AddTripScreen(
                onSave = { trip ->
                    onAddTrip(trip)
                    currentScreen = Screen.List
                },
                onBack = { currentScreen = Screen.List },
                onOpenMapPicker = { initialLat, initialLng, callback ->
                    pendingLat = initialLat
                    pendingLng = initialLng
                    mapPickerCallback = callback
                    currentScreen = Screen.MapPicker(0)
                }
            )
        }
        is Screen.MapPicker -> {
            MapPickerScreen(
                initialLat = pendingLat,
                initialLng = pendingLng,
                onConfirm = { lat, lng ->
                    mapPickerCallback?.invoke(lat, lng)
                    mapPickerCallback = null
                    currentScreen = Screen.Add
                },
                onCancel = {
                    mapPickerCallback = null
                    currentScreen = Screen.Add
                }
            )
        }
        is Screen.Settings -> {
            SettingsScreen()
        }
    }
}
