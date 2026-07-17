package com.example.triplog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class TripRepositoryImpl(private val dao: TripDao) : TripRepository {
    override suspend fun insertTrip(trip: Trip) {
        dao.insertTrip(trip.toEntity())
    }

    override fun getAllTrips(): Flow<List<Trip>> {
        return dao.getAllTrips().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun Trip.toEntity() = TripEntity(
        id = id,
        title = title,
        city = city,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        notes = notes,
        lat = lat,
        lng = lng
    )

    private fun TripEntity.toDomain() = Trip(
        id = id,
        title = title,
        city = city,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        notes = notes,
        lat = lat,
        lng = lng
    )
}
