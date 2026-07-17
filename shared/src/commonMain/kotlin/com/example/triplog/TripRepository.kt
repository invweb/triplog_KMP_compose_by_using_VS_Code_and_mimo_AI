package com.example.triplog

import kotlinx.coroutines.flow.Flow

interface TripRepository {
    suspend fun insertTrip(trip: Trip)
    fun getAllTrips(): Flow<List<Trip>>
}