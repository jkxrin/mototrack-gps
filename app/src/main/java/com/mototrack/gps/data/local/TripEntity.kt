package com.mototrack.gps.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mototrack.gps.domain.model.TrackPoint

/**
 * Room Entity representing a completed motorcycle trip session.
 * All units strictly in kilometers (km) and kilometers per hour (km/h).
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDistanceKm: Double,
    val maxSpeedKmh: Float,
    val averageSpeedKmh: Float,
    val durationMillis: Long,
    val routeCoordinates: List<TrackPoint>
)
