package com.sportchronoclock.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Yaris-style bouncing-motorbike loader, tempo-locked to 135 BPM (~444 ms per beat).
 * One full squish → pop → settle per beat, no idle holds. A small ground shadow widens with the
 * squish to sell the impact.
 *
 * Used wherever the map is waiting for a GPS fix — the bike's bouncing while it scans for satellites.
 */
@Composable
fun BouncingBikeLoader(
    caption: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "bike-bounce")
    // 60000 ms / 135 BPM = 444 ms per beat. One bounce per beat.
    val beatMs = 444

    val scaleX by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = beatMs
                1.00f at 0
                1.35f at 90 using FastOutLinearInEasing         // SQUISH (on-beat impact)
                0.88f at 200 using LinearOutSlowInEasing        // POP overshoot
                1.04f at 310 using FastOutSlowInEasing          // tiny return overshoot
                1.00f at beatMs                                  // settle into next beat
            },
        ),
        label = "scaleX",
    )
    val scaleY by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = beatMs
                1.00f at 0
                0.60f at 90 using FastOutLinearInEasing
                1.18f at 200 using LinearOutSlowInEasing
                0.97f at 310 using FastOutSlowInEasing
                1.00f at beatMs
            },
        ),
        label = "scaleY",
    )
    val shadowScaleX by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = beatMs
                0.70f at 0
                1.25f at 90 using FastOutLinearInEasing          // shadow widens on impact
                0.85f at 200 using LinearOutSlowInEasing
                0.92f at 310
                0.70f at beatMs
            },
        ),
        label = "shadowScaleX",
    )
    val shadowAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = beatMs
                0.45f at 0
                0.75f at 90
                0.30f at 200
                0.40f at 310
                0.45f at beatMs
            },
        ),
        label = "shadowAlpha",
    )

    Box(
        modifier = modifier.background(Color(0xFF111111)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.height(120.dp).width(120.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // Ground shadow — widens and brightens on impact
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(8.dp)
                        .graphicsLayer {
                            this.scaleX = shadowScaleX
                            alpha = shadowAlpha
                        }
                        .background(Color(0xFF000000), CircleShape),
                )
                // Bike emoji — squashes and stretches from its base
                Text(
                    text = "🏍️",
                    fontSize = 72.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            this.scaleX = scaleX
                            this.scaleY = scaleY
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = caption,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
