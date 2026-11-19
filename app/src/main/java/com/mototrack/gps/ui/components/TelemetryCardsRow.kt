package com.mototrack.gps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mototrack.gps.ui.theme.BorderHighlight
import com.mototrack.gps.ui.theme.CardBackground
import com.mototrack.gps.ui.theme.NeonAmber
import com.mototrack.gps.ui.theme.NeonCyan
import com.mototrack.gps.ui.theme.NeonGreen
import com.mototrack.gps.ui.theme.NeonRed
import com.mototrack.gps.ui.theme.TextSecondary
import com.mototrack.gps.util.MetricUtils

/**
 * Superbike Dashboard Telemetry Cards Row:
 * - Distance (km)
 * - Max Speed (km/h)
 * - Average Speed (km/h)
 * - Active Duration (HH:mm:ss)
 */
@Composable
fun TelemetryCardsRow(
    totalDistanceKm: Double,
    maxSpeedKmh: Float,
    averageSpeedKmh: Float,
    durationMillis: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryCard(
                title = "DISTANCE",
                value = MetricUtils.formatDistanceKm(totalDistanceKm),
                unit = "KM",
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "MAX SPEED",
                value = MetricUtils.formatSpeed(maxSpeedKmh),
                unit = "KM/H",
                accentColor = NeonRed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryCard(
                title = "AVG SPEED",
                value = MetricUtils.formatSpeedOneDecimal(averageSpeedKmh),
                unit = "KM/H",
                accentColor = NeonAmber,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "DURATION",
                value = MetricUtils.formatDuration(durationMillis),
                unit = "TIME",
                accentColor = NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, BorderHighlight, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
