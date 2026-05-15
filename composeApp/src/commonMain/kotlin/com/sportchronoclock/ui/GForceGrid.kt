package com.sportchronoclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

private const val MaxG = 2f
private val GridBg = Color(0xFF1a1a2e)
private val Grid = Color(0xFF223355)
private val Major = Color(0xFF334d66)
private val Cyan = Color(0xFF00B4D8)

@Composable
fun GForceGrid(
    lateralG: Float,
    longG: Float,
    peakLateralG: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(GridBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val radius = minOf(w, h) / 2f * 0.92f

            for (g in 1..2) {
                drawCircle(
                    color = Grid,
                    radius = (g / MaxG.toDouble()).toFloat() * radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f),
                )
            }
            drawLine(
                color = Major,
                start = Offset(cx - radius, cy),
                end = Offset(cx + radius, cy),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Major,
                start = Offset(cx, cy - radius),
                end = Offset(cx, cy + radius),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )

            val clampedLat = lateralG.coerceIn(-MaxG, MaxG)
            val clampedLong = longG.coerceIn(-MaxG, MaxG)
            val dotX = cx + (clampedLat / MaxG) * radius
            val dotY = cy - (clampedLong / MaxG) * radius
            drawCircle(
                color = Cyan.copy(alpha = 0.35f),
                radius = 14f,
                center = Offset(dotX, dotY),
            )
            drawCircle(
                color = Cyan,
                radius = 8f,
                center = Offset(dotX, dotY),
            )
        }

        Text(
            text = "G",
            color = Color(0xFF4488bb),
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = "PEAK ${round(peakLateralG * 10) / 10}",
            color = Color(0xFFC8102E),
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = "${round(lateralG * 100) / 100}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

