package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.ride.RideStatsViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.round

@Composable
fun RideSummaryScreen(
    rideId: Long,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: RideStatsViewModel = koinViewModel()
    val rideFlow = remember(rideId) { vm.rideById(rideId) }
    val ride by rideFlow.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) {
                Text("‹ BACK", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            TextButton(onClick = {
                vm.deleteRide(rideId)
                onDelete()
            }) {
                Text("DELETE", color = Color(0xFFC8102E), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1a2a3a))
        val r = ride
        if (r == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = Color(0xFF4488bb), fontSize = 14.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = formatLocalDate(r.startedAt),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                HeroNumber(
                    label = "DISTANCE",
                    value = formatDistanceKm(r.distanceMeters),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStat("DURATION", formatDuration(r.durationSeconds), Modifier.weight(1f))
                    SmallStat("MAX KM/H", "${r.maxSpeedKmh.toInt()}", Modifier.weight(1f))
                    SmallStat("AVG KM/H", "${r.avgMovingSpeedKmh.toInt()}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStat(
                        "MAX LEAN",
                        r.maxLeanDeg?.let { "${round(it).toInt()}°" } ?: "—",
                        Modifier.weight(1f),
                    )
                    SmallStat(
                        "MAX G",
                        r.maxLateralG?.let { "${round(it * 10) / 10}" } ?: "—",
                        Modifier.weight(1f),
                    )
                    SmallStat(
                        "ELEVATION",
                        r.elevationGainMeters?.let { "${it.toInt()}m" } ?: "—",
                        Modifier.weight(1f),
                    )
                }
                if (r.crashed) {
                    Text(
                        text = "⚠ Ride ended unexpectedly",
                        color = Color(0xFFC8102E),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroNumber(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            color = Color(0xFF00B4D8),
            fontSize = 52.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = label,
            color = Color(0xFF4488bb),
            fontSize = 10.sp,
            letterSpacing = 3.sp,
        )
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xF0071420), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
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
            letterSpacing = 1.5.sp,
        )
    }
}
