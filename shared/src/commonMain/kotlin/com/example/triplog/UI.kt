package com.example.triplog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    trips: List<Trip>,
    onTripClick: (Trip) -> Unit,
    onAddTrip: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TripLog") },
                actions = {
                    IconButton(onClick = onAddTrip) {
                        Icon(Icons.Default.Add, contentDescription = "Add Trip")
                    }
                }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No trips yet. Tap + to add one!",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(trips) { trip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { onTripClick(trip) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${trip.city} · ${trip.startDate} → ${trip.endDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(trip: Trip, onBack: () -> Unit, onDelete: (Trip) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete trip?") },
            text = { Text("Are you sure you want to delete \"${trip.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(trip)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Text("✕", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = trip.city,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dates: ${trip.startDate} → ${trip.endDate}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coordinates: ${trip.lat}, ${trip.lng}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (trip.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Map",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TripMapScreen(trips = listOf(trip))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onSave: (Trip) -> Unit,
    onBack: () -> Unit,
    onOpenMapPicker: (initialLat: Double, initialLng: Double, callback: (Double, Double) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cityQuery by remember { mutableStateOf("") }
    var showCityDropdown by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var notes by remember { mutableStateOf("") }
    var lat by remember { mutableDoubleStateOf(59.9343) }
    var lng by remember { mutableDoubleStateOf(30.3351) }
    var showError by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val cities = remember {
        val json = loadAsset("cities.json")
        CityRepository.loadCities(json)
    }

    val filteredCities = remember(cityQuery) {
        if (cityQuery.isBlank()) cities
        else cities.filter {
            it.city.contains(cityQuery, ignoreCase = true) ||
            it.country.contains(cityQuery, ignoreCase = true)
        }.take(20)
    }

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (startDate ?: today).toEpochDays() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = LocalDate.fromEpochDays((it / 86400000L).toInt())
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (endDate ?: startDate ?: today).toEpochDays() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        endDate = LocalDate.fromEpochDays((it / 86400000L).toInt())
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cityQuery,
                onValueChange = {
                    cityQuery = it
                    showCityDropdown = true
                    city = it
                },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showCityDropdown && filteredCities.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn {
                        items(filteredCities) { c ->
                            ListItem(
                                headlineContent = { Text("${c.city}, ${c.country}") },
                                modifier = Modifier.clickable {
                                    city = c.city
                                    cityQuery = c.city
                                    lat = c.lat
                                    lng = c.lng
                                    showCityDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = startDate?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Start date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartPicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedTextField(
                value = endDate?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("End date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndPicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedTextField(
                value = String.format("%.6f", lat),
                onValueChange = {},
                readOnly = true,
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedTextField(
                value = String.format("%.6f", lng),
                onValueChange = {},
                readOnly = true,
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedButton(
                onClick = {
                    onOpenMapPicker(lat, lng) { newLat, newLng ->
                        lat = newLat
                        lng = newLng
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select on map")
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (showError) {
                Text(
                    text = "Please fill all fields correctly. Start date must be ≤ end date.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = {
                    val sd = startDate
                    val ed = endDate
                    if (sd != null && ed != null && title.isNotBlank() && city.isNotBlank() && sd <= ed) {
                        onSave(
                            Trip(
                                title = title,
                                city = city,
                                startDate = sd,
                                endDate = ed,
                                notes = notes,
                                lat = lat,
                                lng = lng
                            )
                        )
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MVP TripLog — Room на Android, SQLite JDBC на Desktop",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}