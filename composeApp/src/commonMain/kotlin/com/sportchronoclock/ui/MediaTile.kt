package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.media.MediaControlViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MediaTile(modifier: Modifier = Modifier) {
    val vm: MediaControlViewModel = koinViewModel()
    val info by vm.info.collectAsState()
    val access by vm.accessState.collectAsState()

    if (access.needsOnboarding) {
        AccessPromptCard(onGrant = vm::requestAccess, modifier = modifier)
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF0071420), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1a2a3a), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.title ?: if (info.hasSession) "Now playing" else "No media",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = info.artist ?: "—",
                color = Color(0xFF8899AA),
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        MediaButton(symbol = "⏮", onClick = vm::skipPrevious)
        MediaButton(
            symbol = if (info.isPlaying) "⏸" else "▶",
            onClick = vm::togglePlay,
            accent = true,
        )
        MediaButton(symbol = "⏭", onClick = vm::skipNext)
    }
}

@Composable
private fun MediaButton(symbol: String, onClick: () -> Unit, accent: Boolean = false) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                if (accent) Color(0xFF0057B8) else Color(0xFF0a1320),
                CircleShape,
            )
            .border(1.dp, if (accent) Color(0xFF00B4D8) else Color(0xFF1a2a3a), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun AccessPromptCard(onGrant: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF0071420), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFC8102E), RoundedCornerShape(14.dp))
            .clickable(onClick = onGrant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "MEDIA CONTROLS",
            color = Color(0xFFC8102E),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Tap to grant Notification access — required to see and control Spotify, YouTube Music, etc.",
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}
