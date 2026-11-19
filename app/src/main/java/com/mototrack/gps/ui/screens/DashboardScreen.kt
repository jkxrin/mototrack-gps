package com.mototrack.gps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mototrack.gps.ui.components.OsmMapView
import com.mototrack.gps.ui.components.SuperbikeTachometer
import com.mototrack.gps.ui.components.TelemetryCardsRow
import com.mototrack.gps.ui.theme.CarbonBlack
import com.mototrack.gps.ui.theme.DarkGunmetal
import com.mototrack.gps.ui.theme.NeonCyan
import com.mototrack.gps.ui.theme.NeonRed

/**
 * Main Superbike Telemetry Dashboard Screen.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.telemetryState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MOTOTRACK GPS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
                Text(
                    text = if (state.isTracking) "• LIVE GPS RECORDING" else "• STANDBY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isTracking) NeonCyan else Color.Gray
                )
            }

            IconButton(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Trip History",
                    tint = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Superbike Animated Analog + Digital Gauge
        SuperbikeTachometer(
            currentSpeedKmh = state.currentSpeedKmh,
            maxSpeedDisplay = 240f,
            modifier = Modifier.weight(1.3f)
        )

        // Telemetry Row: Distance, Max Speed, Avg Speed, Duration
        TelemetryCardsRow(
            totalDistanceKm = state.totalDistanceKm,
            maxSpeedKmh = state.maxSpeedKmh,
            averageSpeedKmh = state.averageSpeedKmh,
            durationMillis = state.movingDurationMillis,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        // Live OpenStreetMap Route Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGunmetal)
        ) {
            OsmMapView(
                routePoints = state.pathPoints,
                currentPoint = state.currentPoint,
                isInteractive = true,
                autoCenter = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Controls: Save & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetTrip() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
                Text(
                    text = "RESET",
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Button(
                onClick = { viewModel.finishAndSaveTrip() },
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
            ) {
                Text(
                    text = "FINISH & SAVE TRIP",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }
    }
}
