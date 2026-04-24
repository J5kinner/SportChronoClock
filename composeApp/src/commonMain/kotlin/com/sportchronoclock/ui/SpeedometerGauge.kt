package com.sportchronoclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonYellow = Color(0xFFFFFF00)
private val TrackGray = Color(0xFF2A2A2A)

@Composable
fun SpeedometerGauge(
    speedKmh: Float,
    maxSpeed: Float = 200f,
    modifier: Modifier = Modifier
) {
    val progress = (speedKmh / maxSpeed).coerceIn(0f, 1f)
    // Arc: starts at 150° (lower-left), sweeps 240° clockwise
    val startAngle = 150f
    val totalSweep = 240f

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.07f
            val inset = strokeWidth / 2f + size.minDimension * 0.05f
            val arcSize = Size(size.minDimension - inset * 2, size.minDimension - inset * 2)
            val arcOffset = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )

            // Background track
            drawArc(
                color = TrackGray,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = NeonYellow,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * progress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedKmh.toInt().toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = NeonYellow
            )
            Text(
                text = "km/h",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.offset(y = (-8).dp)
            )
        }
    }
}
