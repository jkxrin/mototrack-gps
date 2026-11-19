package com.mototrack.gps.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mototrack.gps.data.local.AppDatabase
import com.mototrack.gps.data.local.TripEntity
import com.mototrack.gps.data.repository.TripRepository
import com.mototrack.gps.domain.model.TelemetryState
import com.mototrack.gps.service.LocationTrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main Live Telemetry Dashboard ViewModel.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao())
    }

    val telemetryState: StateFlow<TelemetryState> = LocationTrackingService.telemetryFlow

    val pastTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startTrip() {
        LocationTrackingService.start(getApplication())
    }

    fun resetTrip() {
        LocationTrackingService.reset(getApplication())
    }

    fun finishAndSaveTrip(onSaved: (Long) -> Unit = {}) {
        val current = telemetryState.value
        // Only save if there's actual distance or points
        if (current.totalDistanceKm > 0.05 || current.pathPoints.size >= 2) {
            viewModelScope.launch {
                val trip = TripEntity(
                    startTimeMillis = System.currentTimeMillis() - current.movingDurationMillis,
                    endTimeMillis = System.currentTimeMillis(),
                    totalDistanceKm = current.totalDistanceKm,
                    maxSpeedKmh = current.maxSpeedKmh,
                    averageSpeedKmh = current.averageSpeedKmh,
                    durationMillis = current.movingDurationMillis,
                    routeCoordinates = current.pathPoints
                )
                val id = repository.saveTrip(trip)
                LocationTrackingService.reset(getApplication())
                onSaved(id)
            }
        } else {
            LocationTrackingService.reset(getApplication())
        }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch {
            repository.deleteTrip(id)
        }
    }
}
