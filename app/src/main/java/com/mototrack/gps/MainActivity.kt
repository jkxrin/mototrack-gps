package com.mototrack.gps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mototrack.gps.data.local.TripEntity
import com.mototrack.gps.service.LocationTrackingService
import com.mototrack.gps.ui.screens.DashboardScreen
import com.mototrack.gps.ui.screens.DashboardViewModel
import com.mototrack.gps.ui.screens.TripDetailScreen
import com.mototrack.gps.ui.screens.TripHistoryScreen
import com.mototrack.gps.ui.theme.CarbonBlack
import com.mototrack.gps.ui.theme.MotoTrackTheme
import org.osmdroid.config.Configuration

/**
 * MotoTrack GPS - Production Main Activity.
 *
 * Responsibilities:
 * - Initializes osmdroid user agent configuration
 * - Handles fine location, background location, and POST_NOTIFICATIONS runtime permissions
 * - Auto-starts a fresh trip tracking session on launch
 * - Provides lightweight navigation between Dashboard, Saved Trips, and Trip Detail
 */
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            // Auto-start active trip session immediately upon permission grant
            LocationTrackingService.start(this)
            requestBackgroundLocationIfNeeded()
        }
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Background permission handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid cache and user-agent policy
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        // Check and request runtime permissions
        requestLocationPermissions()

        setContent {
            MotoTrackTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CarbonBlack),
                    color = CarbonBlack
                ) {
                    MotoTrackApp(viewModel = dashboardViewModel)
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // Permissions already granted: auto-start a fresh trip session immediately
            LocationTrackingService.start(this)
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }
}

private sealed interface Screen {
    data object Dashboard : Screen
    data object History : Screen
    data class Detail(val trip: TripEntity) : Screen
}

@Composable
private fun MotoTrackApp(viewModel: DashboardViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = {
                        currentScreen = Screen.History
                    }
                )
            }

            is Screen.History -> {
                TripHistoryScreen(
                    viewModel = viewModel,
                    onTripClick = { selectedTrip ->
                        currentScreen = Screen.Detail(selectedTrip)
                    },
                    onBack = {
                        currentScreen = Screen.Dashboard
                    }
                )
            }

            is Screen.Detail -> {
                TripDetailScreen(
                    trip = screen.trip,
                    onBack = {
                        currentScreen = Screen.History
                    }
                )
            }
        }
    }
}
