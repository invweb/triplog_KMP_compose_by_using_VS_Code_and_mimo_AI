package com.example.triplog

import kotlinx.datetime.LocalDate

data class Trip(
    val id: Int = 0,
    val title: String,
    val city: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String,
    val lat: Double,
    val lng: Double
)