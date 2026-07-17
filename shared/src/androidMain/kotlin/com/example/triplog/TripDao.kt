package com.example.triplog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insertTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: Int)

    @Query("SELECT * FROM trips ORDER BY id DESC")
    fun getAllTrips(): Flow<List<TripEntity>>
}
