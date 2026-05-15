package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.sport.SportModeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SportHud(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainVm: MainViewModel = koinViewModel()
    val sportVm: SportModeViewModel = koinViewModel()

    val speedKmh by mainVm.speedKmh.collectAsState()
    val motion by sportVm.motion.collectAsState()
    val peakLean by sportVm.peakLeanDeg.collectAsState()
    val peakG by sportVm.peakLateralG.collectAsState()

    LaunchedEffect(Unit) { sportVm.activate() }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpeedPanel(
                        speedKmh = speedKmh,
                        modifier = Modifier.fillMaxHeight().weight(0.5f),
                    )
                    Column(
                        modifier = Modifier.fillMaxHeight().weight(0.5f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LeanGauge(
                            rollDeg = motion?.rollDeg ?: 0f,
                            peakLeanDeg = peakLean,
                            modifier = Modifier.fillMaxWidth().weight(0.55f),
                        )
                        GForceGrid(
                            lateralG = motion?.lateralG ?: 0f,
                            longG = motion?.longG ?: 0f,
                            peakLateralG = peakG,
                            modifier = Modifier.fillMaxWidth().weight(0.45f),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpeedPanel(
                        speedKmh = speedKmh,
                        modifier = Modifier.fillMaxWidth().weight(0.42f),
                    )
                    LeanGauge(
                        rollDeg = motion?.rollDeg ?: 0f,
                        peakLeanDeg = peakLean,
                        modifier = Modifier.fillMaxWidth().weight(0.30f),
                    )
                    GForceGrid(
                        lateralG = motion?.lateralG ?: 0f,
                        longG = motion?.longG ?: 0f,
                        peakLateralG = peakG,
                        modifier = Modifier.fillMaxWidth().weight(0.28f),
                    )
                }
            }
        }

        // Exit button - top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(20.dp))
                .clickable { onExit() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹ EXIT", color = Color(0xFF00B4D8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        // Calibrate button - top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF00B4D8), RoundedCornerShape(20.dp))
                .clickable { sportVm.calibrate() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("SET ZERO", color = Color(0xFF00B4D8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun SpeedPanel(speedKmh: Float, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF1a1a2e), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedKmh.toInt().toString(),
                color = Color(0xFF00B4D8),
                fontSize = 120.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = "KM/H",
                color = Color(0xFF4488bb),
                fontSize = 14.sp,
                letterSpacing = 6.sp,
            )
        }
    }
}
