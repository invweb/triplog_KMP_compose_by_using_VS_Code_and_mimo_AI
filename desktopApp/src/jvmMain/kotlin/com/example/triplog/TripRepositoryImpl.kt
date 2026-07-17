package com.example.triplog

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class TripRepositoryImpl : TripRepository {
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    private val trips = _trips.asStateFlow()

    init {
        Class.forName("org.sqlite.JDBC")
        val dbDir = File(System.getProperty("user.home"), ".trip_log")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        val dbFile = File(dbDir, "trips.db")
        createTableIfNotExists(dbFile)
        loadTrips(dbFile)
    }

    private fun createTableIfNotExists(dbFile: File) {
        getConnection(dbFile).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        city TEXT,
                        startDate TEXT,
                        endDate TEXT,
                        notes TEXT,
                        lat REAL,
                        lng REAL
                    )
                """.trimIndent())
            }
        }
    }

    private fun loadTrips(dbFile: File) {
        val tripsList = mutableListOf<Trip>()
        getConnection(dbFile).use { connection ->
            connection.prepareStatement("SELECT * FROM trips ORDER BY id DESC").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        tripsList.add(resultSet.toTrip())
                    }
                }
            }
        }
        _trips.value = tripsList
    }

    private fun getConnection(dbFile: File): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
    }

    private fun ResultSet.toTrip() = Trip(
        id = getInt("id"),
        title = getString("title"),
        city = getString("city"),
        startDate = LocalDate.parse(getString("startDate")),
        endDate = LocalDate.parse(getString("endDate")),
        notes = getString("notes"),
        lat = getDouble("lat"),
        lng = getDouble("lng")
    )

    override suspend fun insertTrip(trip: Trip) {
        withContext(Dispatchers.IO) {
            val dbFile = File(System.getProperty("user.home"), ".trip_log/trips.db")
            getConnection(dbFile).use { connection ->
                connection.prepareStatement(
                    "INSERT INTO trips (title, city, startDate, endDate, notes, lat, lng) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
                ).use { statement ->
                    statement.setString(1, trip.title)
                    statement.setString(2, trip.city)
                    statement.setString(3, trip.startDate.toString())
                    statement.setString(4, trip.endDate.toString())
                    statement.setString(5, trip.notes)
                    statement.setDouble(6, trip.lat)
                    statement.setDouble(7, trip.lng)
                    statement.executeUpdate()

                    statement.generatedKeys.use { keys ->
                        if (keys.next()) {
                            val newId = keys.getInt(1)
                            val newTrip = trip.copy(id = newId)
                            _trips.value = listOf(newTrip) + _trips.value
                        }
                    }
                }
            }
        }
    }

    override suspend fun deleteTrip(trip: Trip) {
        withContext(Dispatchers.IO) {
            val dbFile = File(System.getProperty("user.home"), ".trip_log/trips.db")
            getConnection(dbFile).use { connection ->
                connection.prepareStatement("DELETE FROM trips WHERE id = ?").use { statement ->
                    statement.setInt(1, trip.id)
                    statement.executeUpdate()
                }
            }
            _trips.value = _trips.value.filter { it.id != trip.id }
        }
    }

    override fun getAllTrips(): Flow<List<Trip>> {
        return trips
    }
}