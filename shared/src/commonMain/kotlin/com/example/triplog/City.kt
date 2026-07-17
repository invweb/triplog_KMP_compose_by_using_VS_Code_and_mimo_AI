package com.example.triplog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class City(
    val city: String,
    val country: String,
    val lat: Double,
    val lng: Double
)

object CityRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadCities(jsonString: String): List<City> {
        return try {
            json.decodeFromString<List<City>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
