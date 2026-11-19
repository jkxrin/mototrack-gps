package com.mototrack.gps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mototrack.gps.MainActivity
import com.mototrack.gps.R
import com.mototrack.gps.domain.model.TelemetryState
import com.mototrack.gps.domain.model.TrackPoint
import com.mototrack.gps.util.MetricUtils
import com.mototrack.gps.widget.MotoDistanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Robust Foreground Location Service for MotoTrack GPS.
 * Ensures uninterrupted GPS speed calculation and route recording
 * even when the screen is turned off, device is locked, or other apps are open.
 */
class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager

    private var previousLocation: Location? = null
    private var totalDistanceMeters: Double = 0.0
    private var maxSpeedKmh: Float = 0.0f
    private var startTimeMillis: Long = 0L
    private var movingDurationMillis: Long = 0L
    private var lastMovingTimestamp: Long = 0L
    private val recordedPoints = mutableListOf<TrackPoint>()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_RESET_TRIP -> resetTrip()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        val notification = buildNotification(speedKmh = 0.0f, distanceKm = 0.0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (startTimeMillis == 0L) {
            startTimeMillis = System.currentTimeMillis()
            lastMovingTimestamp = startTimeMillis
        }

        requestLocationUpdates()
        updateTelemetryState(currentSpeed = 0f, isTracking = true)
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL_MS)
            setMinUpdateDistanceMeters(MIN_DISTANCE_DISPLACEMENT_METERS)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Missing location permission
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processNewLocation(location)
                }
            }
        }
    }

    private fun processNewLocation(location: Location) {
        // GPS Drift Filter: Ignore stationary flutter/jitter
        if (MetricUtils.isStationaryNoise(location, previousLocation)) {
            // Stationary: do not add false distance, set instantaneous speed to 0 if almost stopped
            val smoothedZeroSpeed = 0.0f
            updateTelemetryState(currentSpeed = smoothedZeroSpeed, isTracking = true)
            return
        }

        val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f
        var calculatedSpeedKmh = MetricUtils.mpsToKmh(rawSpeedMps)

        // Calculate distance delta
        var deltaMeters = 0.0
        val prev = previousLocation
        val now = System.currentTimeMillis()

        if (prev != null) {
            deltaMeters = prev.distanceTo(location).toDouble()
            val timeDeltaSeconds = ((location.time - prev.time).coerceAtLeast(1L)) / 1000.0

            // If hardware speed is zero or missing, calculate from coordinate delta
            if (calculatedSpeedKmh <= 0.1f && timeDeltaSeconds > 0) {
                val derivedSpeedMps = (deltaMeters / timeDeltaSeconds).toFloat()
                calculatedSpeedKmh = MetricUtils.mpsToKmh(derivedSpeedMps)
            }
        }

        // Motorcycle realistic noise cutoff (< 1.5 km/h is treated as stationary)
        if (calculatedSpeedKmh < 1.5f) {
            calculatedSpeedKmh = 0.0f
        } else {
            // Increment moving time
            if (lastMovingTimestamp > 0L) {
                movingDurationMillis += (now - lastMovingTimestamp).coerceIn(0L, 5000L)
            }
            totalDistanceMeters += deltaMeters
        }
        lastMovingTimestamp = now

        if (calculatedSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = calculatedSpeedKmh
        }

        previousLocation = location

        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speedKmh = calculatedSpeedKmh,
            timestamp = now
        )
        recordedPoints.add(point)

        // Broadcast to Live StateFlow
        updateTelemetryState(currentSpeed = calculatedSpeedKmh, newPoint = point, isTracking = true)

        // Update persistent Notification & Home Screen Widget
        val distanceKm = MetricUtils.metersToKm(totalDistanceMeters)
        updateNotification(calculatedSpeedKmh, distanceKm)
        triggerWidgetUpdate(distanceKm)
    }

    private fun updateTelemetryState(
        currentSpeed: Float,
        newPoint: TrackPoint? = null,
        isTracking: Boolean = true
    ) {
        val totalKm = MetricUtils.metersToKm(totalDistanceMeters)
        val movingHours = movingDurationMillis / 3600000.0
        val avgSpeed = if (movingHours > 0.001) {
            (totalKm / movingHours).toFloat().coerceAtLeast(0f)
        } else {
            0f
        }

        _telemetryFlow.value = TelemetryState(
            currentSpeedKmh = currentSpeed,
            maxSpeedKmh = maxSpeedKmh,
            averageSpeedKmh = avgSpeed,
            totalDistanceKm = totalKm,
            movingDurationMillis = movingDurationMillis,
            isTracking = isTracking,
            currentPoint = newPoint ?: _telemetryFlow.value.currentPoint,
            pathPoints = recordedPoints.toList()
        )
    }

    private fun resetTrip() {
        previousLocation = null
        totalDistanceMeters = 0.0
        maxSpeedKmh = 0.0f
        movingDurationMillis = 0L
        startTimeMillis = System.currentTimeMillis()
        lastMovingTimestamp = startTimeMillis
        recordedPoints.clear()

        updateTelemetryState(currentSpeed = 0f, isTracking = true)
        updateNotification(0f, 0.0)
        triggerWidgetUpdate(0.0)
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
        _telemetryFlow.value = _telemetryFlow.value.copy(
            isTracking = false,
            currentSpeedKmh = 0f
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun triggerWidgetUpdate(distanceKm: Double) {
        currentSharedDistanceKm = distanceKm
        serviceScope.launch {
            try {
                MotoDistanceWidget().updateAll(this@LocationTrackingService)
            } catch (e: Exception) {
                // Glance update fallback
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MotoTrack GPS Trip Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live superbike telemetry (Speed and Distance)"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(speedKmh: Float, distanceKm: Double): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = "${MetricUtils.formatSpeed(speedKmh)} km/h"
        val distText = "${MetricUtils.formatDistanceKm(distanceKm)} km"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MotoTrack GPS • Active Trip")
            .setContentText("Speed: $speedText | Distance: $distText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(speedKmh: Float, distanceKm: Double) {
        val notification = buildNotification(speedKmh, distanceKm)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        const val CHANNEL_ID = "mototrack_gps_service_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_TRACKING = "com.mototrack.gps.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.mototrack.gps.STOP_TRACKING"
        const val ACTION_RESET_TRIP = "com.mototrack.gps.RESET_TRIP"

        private const val LOCATION_INTERVAL_MS = 1000L
        private const val FASTEST_LOCATION_INTERVAL_MS = 800L
        private const val MIN_DISTANCE_DISPLACEMENT_METERS = 1.0f

        @Volatile
        var currentSharedDistanceKm: Double = 0.0

        private val _telemetryFlow = MutableStateFlow(TelemetryState())
        val telemetryFlow: StateFlow<TelemetryState> = _telemetryFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }

        fun reset(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_RESET_TRIP
            }
            context.startService(intent)
        }
    }
}
