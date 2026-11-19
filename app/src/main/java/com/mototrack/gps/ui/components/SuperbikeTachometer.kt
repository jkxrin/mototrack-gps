package com.mototrack.gps.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mototrack.gps.ui.theme.CarbonBlack
import com.mototrack.gps.ui.theme.NeonAmber
import com.mototrack.gps.ui.theme.NeonCyan
import com.mototrack.gps.ui.theme.NeonGreen
import com.mototrack.gps.ui.theme.NeonRed
import com.mototrack.gps.ui.theme.TextDisabled
import com.mototrack.gps.ui.theme.TextSecondary
import com.mototrack.gps.util.MetricUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Performance Superbike Tachometer Gauge Component (Ducati/Yamaha R1 style).
 * Features:
 * - Sweeping dynamic needle animated with physics-based spring/tween
 * - Multi-color glowing RPM/speed arc dynamically shifting from Cyan -> Green -> Amber -> Red
 * - High-speed redline warning flash animation
 * - Precision radial tick marks and graduated speed intervals (0 to 240+ km/h)
 * - Large digital readout in central instrument cluster
 */
@Composable
fun SuperbikeTachometer(
    currentSpeedKmh: Float,
    maxSpeedDisplay: Float = 240f,
    modifier: Modifier = Modifier
) {
    // Smooth interpolation so needle moves fluidly without jitter
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedKmh.coerceIn(0f, maxSpeedDisplay),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "tachometer_needle"
    )

    // Redline flashing pulse when exceeding 180 km/h
    val isRedlining = animatedSpeed > 180f
    val infiniteTransition = rememberInfiniteTransition(label = "redline_flash")
    val redlineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redline_alpha"
    )

    val startAngle = 140f
    val sweepTotal = 260f
    val speedProgress = (animatedSpeed / maxSpeedDisplay).coerceIn(0f, 1f)
    val currentSweep = sweepTotal * speedProgress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + 10f)
            val radius = (size.minDimension / 2f) - 36f

            // 1. Outer Dark Bezel / Gauge Ring
            drawCircle(
                color = Color(0xFF141820),
                radius = radius + 22f,
                center = center,
                style = Stroke(width = 16f)
            )

            // 2. Track Background Arc (Dimmest track)
            drawArc(
                color = Color(0xFF1A222D),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 22f, cap = StrokeCap.Round)
            )

            // 3. Dynamic Multi-Color Glowing RPM/Speed Arc
            val activeColor = when {
                animatedSpeed < 60f -> NeonCyan
                animatedSpeed < 120f -> NeonGreen
                animatedSpeed < 180f -> NeonAmber
                else -> NeonRed.copy(alpha = if (isRedlining) redlineAlpha else 1f)
            }

            if (currentSweep > 0.5f) {
                val arcBrush = Brush.sweepGradient(
                    0.0f to NeonCyan,
                    0.35f to NeonGreen,
                    0.65f to NeonAmber,
                    0.85f to NeonRed,
                    1.0f to NeonRed,
                    center = center
                )

                // Glow shadow arc
                drawArc(
                    color = activeColor.copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 30f, cap = StrokeCap.Round)
                )

                // Main vibrant arc
                drawArc(
                    brush = arcBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )
            }

            // 4. Tick Marks and Speed Numbers
            val totalTicks = 24
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val tickAngle = startAngle + (sweepTotal * tickFraction)
                val isMajorTick = i % 4 == 0
                val tickLength = if (isMajorTick) 22f else 12f
                val tickWidth = if (isMajorTick) 3.5f else 1.8f
                val tickColor = if (tickFraction <= speedProgress) {
                    activeColor
                } else {
                    if (isMajorTick) Color(0xFF546E7A) else Color(0xFF2C3949)
                }

                val angleRad = Math.toRadians(tickAngle.toDouble())
                val innerR = radius - 18f
                val outerR = innerR - tickLength

                val startX = (center.x + innerR * cos(angleRad)).toFloat()
                val startY = (center.y + innerR * sin(angleRad)).toFloat()
                val endX = (center.x + outerR * cos(angleRad)).toFloat()
                val endY = (center.y + outerR * sin(angleRad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )

                // Draw speed labels for major ticks
                if (isMajorTick) {
                    val labelSpeed = (tickFraction * maxSpeedDisplay).toInt()
                    val labelR = outerR - 20f
                    val labelX = (center.x + labelR * cos(angleRad)).toFloat()
                    val labelY = (center.y + labelR * sin(angleRad)).toFloat() + 5f

                    drawContext.canvas.nativeCanvas.drawText(
                        labelSpeed.toString(),
                        labelX,
                        labelY,
                        Paint().apply {
                            color = android.graphics.Color.argb(200, 144, 164, 174)
                            textSize = 24f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                }
            }

            // 5. Analog Sweeping Needle with Redline Flare
            val needleAngle = startAngle + currentSweep
            rotate(degrees = needleAngle, pivot = center) {
                val needleLength = radius - 26f
                val needlePath = Path().apply {
                    moveTo(center.x - 6f, center.y)
                    lineTo(center.x + needleLength, center.y)
                    lineTo(center.x - 6f, center.y + 4f)
                    close()
                }

                // Needle shadow/glow
                drawPath(
                    path = needlePath,
                    color = activeColor.copy(alpha = 0.4f)
                )

                // Sharp needle line
                drawLine(
                    color = if (isRedlining) NeonRed else Color.White,
                    start = center,
                    end = Offset(center.x + needleLength, center.y),
                    strokeWidth = 4.5f,
                    cap = StrokeCap.Round
                )
            }

            // 6. Center Hub
            drawCircle(
                color = Color(0xFF1E252E),
                radius = 32f,
                center = center
            )
            drawCircle(
                color = activeColor,
                radius = 12f,
                center = center
            )
        }

        // 7. Central Digital Cluster Readout (Exact km/h)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 50.dp)
        ) {
            Text(
                text = MetricUtils.formatSpeed(animatedSpeed),
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isRedlining) NeonRed else Color.White
            )
            Text(
                text = "KM / H",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = NeonCyan
            )
        }
    }
}
