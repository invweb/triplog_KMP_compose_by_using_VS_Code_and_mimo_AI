package com.example.triplog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val city: String,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val lat: Double,
    val lng: Double
)
