package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.ride.LiveRideStats
import kotlin.math.round

@Composable
fun TripHudChip(
    stats: LiveRideStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0xF0071420), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF00B4D8), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatBlock(label = "TRIP", value = formatDistanceKm(stats.distanceMeters))
        StatBlock(label = "TIME", value = formatDuration(stats.durationSeconds))
        StatBlock(label = "MAX", value = "${stats.maxSpeedKmh.toInt()}")
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            color = Color(0xFF4488bb),
            fontSize = 9.sp,
            letterSpacing = 2.sp,
        )
    }
}

internal fun formatDistanceKm(meters: Double): String {
    val km = meters / 1000.0
    return when {
        km >= 100.0 -> "${km.toInt()}km"
        km >= 1.0 -> "${round(km * 10) / 10}km"
        else -> "${meters.toInt()}m"
    }
}

internal fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        else -> "${m}:${s.toString().padStart(2, '0')}"
    }
}
