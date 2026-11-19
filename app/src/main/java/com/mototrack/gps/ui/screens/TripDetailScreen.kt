package com.mototrack.gps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mototrack.gps.data.local.TripEntity
import com.mototrack.gps.ui.components.OsmMapView
import com.mototrack.gps.ui.components.TelemetryCardsRow
import com.mototrack.gps.ui.theme.CarbonBlack
import com.mototrack.gps.ui.theme.DarkGunmetal
import com.mototrack.gps.ui.theme.NeonCyan

/**
 * Detailed Trip Inspection Screen.
 * Renders the saved route on OpenStreetMap with full telemetry stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: TripEntity,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TRIP ROUTE REPLAY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGunmetal
                )
            )
        },
        containerColor = CarbonBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                OsmMapView(
                    routePoints = trip.routeCoordinates,
                    currentPoint = trip.routeCoordinates.firstOrNull(),
                    isInteractive = true,
                    autoCenter = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGunmetal)
                    .padding(16.dp)
            ) {
                TelemetryCardsRow(
                    totalDistanceKm = trip.totalDistanceKm,
                    maxSpeedKmh = trip.maxSpeedKmh,
                    averageSpeedKmh = trip.averageSpeedKmh,
                    durationMillis = trip.durationMillis
                )
            }
        }
    }
}
