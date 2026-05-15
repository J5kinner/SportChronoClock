package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.ride.RideRecord
import com.sportchronoclock.ride.RideStatsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RideHistoryScreen(
    onBack: () -> Unit,
    onRideSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: RideStatsViewModel = koinViewModel()
    val rides by vm.completedRides.collectAsState()
    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("‹ BACK", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                text = "RIDE HISTORY",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        HorizontalDivider(color = Color(0xFF1a2a3a))
        if (rides.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No rides yet. Start riding!",
                    color = Color(0xFF4488bb),
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rides, key = { it.id }) { ride ->
                    RideRow(ride = ride, onClick = { onRideSelected(ride.id) })
                }
            }
        }
    }
}

@Composable
private fun RideRow(ride: RideRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF0071420), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = formatLocalDate(ride.startedAt),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatDistanceKm(ride.distanceMeters)} · ${formatDuration(ride.durationSeconds)}",
                color = Color(0xFF4488bb),
                fontSize = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${ride.maxSpeedKmh.toInt()}",
                color = Color(0xFF00B4D8),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "MAX KM/H",
                color = Color(0xFF334d66),
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

internal fun formatLocalDate(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.year}-${month}-${day} ${hour}:${minute}"
}
