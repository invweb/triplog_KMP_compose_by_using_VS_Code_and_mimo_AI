package com.example.triplog

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val scope = MainScope()

fun main() = application {
    val repository = remember { TripRepositoryImpl() }
    var trips by remember { mutableStateOf(emptyList<Trip>()) }

    LaunchedEffect(Unit) {
        repository.getAllTrips().collectLatest { tripList ->
            trips = tripList
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "TripLog",
        state = rememberWindowState()
    ) {
        TripLogApp(
            trips = trips,
            onAddTrip = { trip ->
                scope.launch {
                    repository.insertTrip(trip)
                }
            },
            onDeleteTrip = { trip ->
                scope.launch {
                    repository.deleteTrip(trip)
                }
            }
        )
    }
}
