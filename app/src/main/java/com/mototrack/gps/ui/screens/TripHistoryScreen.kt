package com.mototrack.gps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mototrack.gps.data.local.TripEntity
import com.mototrack.gps.ui.theme.CarbonBlack
import com.mototrack.gps.ui.theme.CardBackground
import com.mototrack.gps.ui.theme.DarkGunmetal
import com.mototrack.gps.ui.theme.NeonAmber
import com.mototrack.gps.ui.theme.NeonCyan
import com.mototrack.gps.ui.theme.NeonRed
import com.mototrack.gps.ui.theme.TextSecondary
import com.mototrack.gps.util.MetricUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saved Trips History Screen.
 * Lists all recorded motorcycle sessions with distance, max speed, and duration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    viewModel: DashboardViewModel,
    onTripClick: (TripEntity) -> Unit,
    onBack: () -> Unit
) {
    val trips by viewModel.pastTrips.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SAVED TRIPS",
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
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved trips yet.\nStart riding and tap 'Finish & Save Trip'.",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripItemCard(
                        trip = trip,
                        onClick = { onTripClick(trip) },
                        onDelete = { viewModel.deleteTrip(trip.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripItemCard(
    trip: TripEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(trip.startTimeMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DISTANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${MetricUtils.formatDistanceKm(trip.totalDistanceKm)} km",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "MAX SPEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${MetricUtils.formatSpeed(trip.maxSpeedKmh)} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = NeonRed
                    )
                }

                Column {
                    Text(
                        text = "AVG SPEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${MetricUtils.formatSpeedOneDecimal(trip.averageSpeedKmh)} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = NeonAmber
                    )
                }
            }
        }
    }
}
