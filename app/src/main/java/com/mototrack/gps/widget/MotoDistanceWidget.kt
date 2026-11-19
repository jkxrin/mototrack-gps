package com.mototrack.gps.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mototrack.gps.MainActivity
import com.mototrack.gps.service.LocationTrackingService
import com.mototrack.gps.util.MetricUtils

/**
 * Lightweight, battery-efficient Android Home Screen Widget built with Jetpack Glance.
 * Displays only the total kilometers traveled during the current active trip session
 * in large, bold, easy-to-read typography.
 */
class MotoDistanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val distanceKm = LocationTrackingService.currentSharedDistanceKm

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF0D0F12)))
                    .cornerRadius(16.dp)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TRIP DISTANCE",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF90A4AE)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = MetricUtils.formatDistanceKm(distanceKm),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = "KILOMETERS",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF00E5FF)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class MotoDistanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MotoDistanceWidget()
}
