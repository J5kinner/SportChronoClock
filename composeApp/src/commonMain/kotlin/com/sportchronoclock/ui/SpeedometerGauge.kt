package com.sportchronoclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ArcTrack = Color(0xFF1a1a2e)
private val ProgressStart = Color(0xFF0057B8)
private val ProgressEnd = Color(0xFF00B4D8)
private val SpeedTextColor = Color(0xFF00B4D8)
private val UnitTextColor = Color(0xFF334d66)
private val LabelColor = Color(0xFF4488bb)
private val MinorTickColor = Color(0xFF223355)
private val BadgeBackground = Color(0xFF111122)
private val BadgeBorder = Color(0xFF1a2a3a)
private val MBlue = Color(0xFF0057B8)
private val MViolet = Color(0xFF6B2F8A)
private val MRed = Color(0xFFC8102E)

@Composable
fun SpeedometerGauge(
    speedKmh: Float,
    maxSpeed: Float = 200f,
    modifier: Modifier = Modifier
) {
    val progress = (speedKmh / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 150f
    val totalSweep = 240f
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.07f
            val inset = strokeWidth / 2f + size.minDimension * 0.12f
            val arcSize = Size(size.minDimension - inset * 2, size.minDimension - inset * 2)
            val arcOffset = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )
            val arcRadius = arcSize.width / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background track
            drawArc(
                color = ArcTrack,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (progress > 0f) {
                // Glow layer
                drawArc(
                    color = ProgressStart.copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = totalSweep * progress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(ProgressStart, ProgressEnd),
                        start = Offset(arcOffset.x, arcOffset.y + arcSize.height),
                        end = Offset(arcOffset.x + arcSize.width, arcOffset.y)
                    ),
                    startAngle = startAngle,
                    sweepAngle = totalSweep * progress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Tick marks drawn outside the arc ring — 13 positions every 20°
            val tickStartR = arcRadius + strokeWidth / 2f + size.minDimension * 0.015f
            val minorLen = size.minDimension * 0.04f
            val majorLen = size.minDimension * 0.07f
            val majorIndices = setOf(0, 3, 6, 9, 12)

            for (i in 0..12) {
                val t = i * (totalSweep / 12f)
                val angleRad = ((startAngle + t) * PI / 180.0).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val isMajor = i in majorIndices
                val len = if (isMajor) majorLen else minorLen
                val color = if (isMajor) LabelColor else MinorTickColor
                val sw = if (isMajor) strokeWidth * 0.14f else strokeWidth * 0.09f
                drawLine(
                    color = color,
                    start = Offset(center.x + tickStartR * cosA, center.y + tickStartR * sinA),
                    end = Offset(
                        center.x + (tickStartR + len) * cosA,
                        center.y + (tickStartR + len) * sinA
                    ),
                    strokeWidth = sw
                )
            }

            // Speed labels at major tick positions (0, 50, 100, 150, 200)
            val labelR = tickStartR + majorLen + size.minDimension * 0.045f
            val labelValues = listOf("0", "50", "100", "150", "200")
            majorIndices.sorted().forEachIndexed { idx, majorIdx ->
                val t = majorIdx * (totalSweep / 12f)
                val angleRad = ((startAngle + t) * PI / 180.0).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val layout = textMeasurer.measure(
                    text = labelValues[idx],
                    style = TextStyle(color = LabelColor, fontSize = 9.sp)
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = center.x + labelR * cosA - layout.size.width / 2f,
                        y = center.y + labelR * sinA - layout.size.height / 2f
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = speedKmh.toInt().toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = SpeedTextColor
            )
            Text(
                text = "KM/H",
                fontSize = 10.sp,
                color = UnitTextColor,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            // BMW M Badge
            Box(
                modifier = Modifier
                    .background(BadgeBackground, RoundedCornerShape(4.dp))
                    .border(0.5.dp, BadgeBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "M",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MBlue,
                        letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Box(Modifier.width(7.dp).height(2.dp).background(MBlue, RoundedCornerShape(1.dp)))
                        Box(Modifier.width(7.dp).height(2.dp).background(MViolet, RoundedCornerShape(1.dp)))
                        Box(Modifier.width(7.dp).height(2.dp).background(MRed, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}
