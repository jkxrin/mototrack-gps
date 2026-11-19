package com.mototrack.gps.domain.model

import java.io.Serializable

/**
 * Lightweight immutable representation of a GPS coordinate for route rendering.
 * Latitude and Longitude stored in decimal degrees.
 * speedKmh stores instantaneous speed at this precise coordinate for heatmaps/analysis.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedKmh: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Snapshot of real-time telemetry emitted by the Foreground Service.
 */
data class TelemetryState(
    val currentSpeedKmh: Float = 0.0f,
    val maxSpeedKmh: Float = 0.0f,
    val averageSpeedKmh: Float = 0.0f,
    val totalDistanceKm: Double = 0.0,
    val movingDurationMillis: Long = 0L,
    val isTracking: Boolean = false,
    val currentPoint: TrackPoint? = null,
    val pathPoints: List<TrackPoint> = emptyList()
)
