package com.mototrack.gps.util

import android.location.Location
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Metric-Only Telemetry Utilities.
 * Strictly calculates speeds in km/h and distances in kilometers.
 * Zero imperial units.
 */
object MetricUtils {

    /**
     * Converts meters per second (m/s) to kilometers per hour (km/h).
     * 1 m/s = 3.6 km/h.
     */
    fun mpsToKmh(mps: Float): Float {
        return (mps * 3.6f).coerceAtLeast(0.0f)
    }

    /**
     * Converts meters to kilometers.
     */
    fun metersToKm(meters: Double): Double {
        return (meters / 1000.0).coerceAtLeast(0.0)
    }

    /**
     * Formats distance in km to 2 decimal places (e.g. 12.45 km).
     */
    fun formatDistanceKm(distanceKm: Double): String {
        return String.format(Locale.US, "%.2f", distanceKm)
    }

    /**
     * Formats speed in km/h to integer display (e.g. 104).
     */
    fun formatSpeed(speedKmh: Float): String {
        return speedKmh.roundToInt().toString()
    }

    /**
     * Formats speed in km/h with 1 decimal place (e.g. 104.2).
     */
    fun formatSpeedOneDecimal(speedKmh: Float): String {
        return String.format(Locale.US, "%.1f", speedKmh)
    }

    /**
     * Formats duration in milliseconds into HH:mm:ss format.
     */
    fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Evaluates whether a new GPS fix represents genuine motorcycle movement or stationary jitter.
     * Stationary GPS noise can add false distance when sitting at red lights or parked.
     */
    fun isStationaryNoise(
        newLocation: Location,
        previousLocation: Location?,
        minMovementMeters: Float = 1.5f,
        speedThresholdKmh: Float = 1.8f
    ): Boolean {
        if (previousLocation == null) return false

        val distance = previousLocation.distanceTo(newLocation)
        val speedKmh = if (newLocation.hasSpeed()) mpsToKmh(newLocation.speed) else 0f

        // If accuracy is poor (> 25m), ignore this jitter point
        if (newLocation.hasAccuracy() && newLocation.accuracy > 25.0f) {
            return true
        }

        // If movement is smaller than GPS noise floor and speed is essentially stationary
        return distance < minMovementMeters && speedKmh < speedThresholdKmh
    }
}
