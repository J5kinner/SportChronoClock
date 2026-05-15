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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.sportchronoclock.settings.DisplayMode
import com.sportchronoclock.settings.SettingsViewModel
import com.sportchronoclock.settings.SpeedUnits
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SettingsViewModel = koinViewModel()
    val state by vm.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) {
                Text("‹ DONE", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                text = "SETTINGS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        HorizontalDivider(color = Color(0xFF1a2a3a))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle("Units")
            SegmentedControl(
                options = listOf("KM/H" to SpeedUnits.KMH, "MPH" to SpeedUnits.MPH),
                selected = state.speedUnits,
                onSelect = vm::setUnits,
            )

            SectionTitle("Display Mode")
            SegmentedControl(
                options = listOf(
                    "Day" to DisplayMode.DAY,
                    "Night" to DisplayMode.NIGHT,
                    "Auto" to DisplayMode.AUTO,
                ),
                selected = state.displayMode,
                onSelect = vm::setDisplayMode,
            )

            SectionTitle("Voice")
            ToggleRow(
                label = "Voice turn cues",
                checked = state.voiceCuesEnabled,
                onCheckedChange = vm::setVoiceCues,
            )

            SectionTitle("Sport Mode")
            ToggleRow(
                label = "Launch into Sport HUD",
                checked = state.sportModeOnStart,
                onCheckedChange = vm::setSportOnStart,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF4488bb),
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun <T> SegmentedControl(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0a1320), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Color(0xFF0057B8) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFF8899AA),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0a1320), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0057B8),
                uncheckedThumbColor = Color(0xFF334d66),
                uncheckedTrackColor = Color(0xFF0a1320),
                uncheckedBorderColor = Color(0xFF334d66),
            ),
        )
    }
}
