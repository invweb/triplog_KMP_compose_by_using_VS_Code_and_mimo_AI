package com.example.triplog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = TripDatabase.getDatabase(this)
        val repository = TripRepositoryImpl(database.tripDao())

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var trips by remember { mutableStateOf(emptyList<Trip>()) }

                    LaunchedEffect(Unit) {
                        repository.getAllTrips().collectLatest { tripList ->
                            trips = tripList
                        }
                    }

                    TripLogApp(
                        trips = trips,
                        onAddTrip = { trip ->
                            scope.launch {
                                repository.insertTrip(trip)
                            }
                        }
                    )
                }
            }
        }
    }
}
