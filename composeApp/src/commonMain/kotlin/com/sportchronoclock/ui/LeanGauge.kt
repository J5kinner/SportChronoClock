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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MaxLean = 60f
private val ScaleBg = Color(0xFF1a1a2e)
private val Track = Color(0xFF223355)
private val Major = Color(0xFF334d66)
private val Cyan = Color(0xFF00B4D8)
private val Red = Color(0xFFC8102E)

@Composable
fun LeanGauge(
    rollDeg: Float,
    peakLeanDeg: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(ScaleBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val centerY = h / 2f
            val barTopY = centerY - h * 0.18f
            val barBottomY = centerY + h * 0.18f
            val barLeft = w * 0.04f
            val barRight = w * 0.96f
            val barWidth = barRight - barLeft

            drawLine(
                color = Track,
                start = Offset(barLeft, centerY),
                end = Offset(barRight, centerY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )

            listOf(-60f, -30f, 0f, 30f, 60f).forEach { deg ->
                val x = barLeft + ((deg + MaxLean) / (MaxLean * 2f)) * barWidth
                drawLine(
                    color = if (deg == 0f) Cyan else Major,
                    start = Offset(x, barTopY),
                    end = Offset(x, barBottomY),
                    strokeWidth = if (deg == 0f) 3f else 2f,
                    cap = StrokeCap.Round,
                )
            }

            val peakAbs = abs(peakLeanDeg).coerceAtMost(MaxLean)
            if (peakAbs > 0f) {
                val rightX = barLeft + ((peakAbs + MaxLean) / (MaxLean * 2f)) * barWidth
                val leftX = barLeft + ((-peakAbs + MaxLean) / (MaxLean * 2f)) * barWidth
                drawLine(
                    color = Red,
                    start = Offset(rightX, barTopY - 6f),
                    end = Offset(rightX, barBottomY + 6f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Red,
                    start = Offset(leftX, barTopY - 6f),
                    end = Offset(leftX, barBottomY + 6f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }

            val current = rollDeg.coerceIn(-MaxLean, MaxLean)
            val markerX = barLeft + ((current + MaxLean) / (MaxLean * 2f)) * barWidth
            drawCircle(
                color = Cyan,
                radius = h * 0.13f,
                center = Offset(markerX, centerY),
            )
            drawCircle(
                color = Color.Black,
                radius = h * 0.05f,
                center = Offset(markerX, centerY),
            )
        }

        Text(
            text = "${rollDeg.roundToInt()}°",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Text(
            text = "PHONE LEAN",
            color = Color(0xFF4488bb),
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        Text(
            text = "PEAK ${peakLeanDeg.roundToInt()}°",
            color = Red,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
