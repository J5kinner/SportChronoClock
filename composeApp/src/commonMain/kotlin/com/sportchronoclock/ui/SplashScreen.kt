package com.sportchronoclock.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFF000)
private val ArcadeRed = Color(0xFFFF1144)
private val PixelGreen = Color(0xFF00FF66)

private enum class SplashPhase { Boot, Title, Race, Go, Wipe }

/**
 * Retro-arcade boot sequence — scanlines, neon-glitch title, motion-trail bike race, "GO!" flash,
 * diagonal wipe to the dashboard. ~2.6 s. Tap anywhere to skip.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.Boot) }

    LaunchedEffect(Unit) {
        delay(380); phase = SplashPhase.Title
        delay(900); phase = SplashPhase.Race
        delay(900); phase = SplashPhase.Go
        delay(450); phase = SplashPhase.Wipe
        delay(280); onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { onFinish() }
            },
    ) {
        Scanlines()
        TitleBanner(visible = phase >= SplashPhase.Title)
        LoadingBar(phase = phase)
        BikeRacer(visible = phase >= SplashPhase.Race, racing = phase == SplashPhase.Race)
        GoFlash(visible = phase == SplashPhase.Go)
        WipeOverlay(visible = phase >= SplashPhase.Wipe)
    }
}

@Composable
private fun Scanlines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gap = 4f
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1f),
            )
            y += gap
        }
    }
}

@Composable
private fun BoxScope.TitleBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 96.dp),
    ) {
        // Brief CRT-style colour glitch on appearance: cycle channels for a moment then settle.
        var glitch by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            repeat(5) {
                glitch = it
                delay(55)
            }
            glitch = 99
        }
        val sportColor = if (glitch < 99) when (glitch % 3) {
            0 -> NeonMagenta
            1 -> NeonCyan
            else -> NeonYellow
        } else NeonCyan
        val shadowOffset = if (glitch < 99) ((glitch * 5) - 10).dp else 0.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // "SPORT" — biggest line, with magenta drop-shadow underneath for chromatic-aberration vibe
            Box {
                Text(
                    text = "SPORT",
                    color = NeonMagenta.copy(alpha = 0.55f),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 7.sp,
                    modifier = Modifier.offset(x = shadowOffset, y = 2.dp),
                )
                Text(
                    text = "SPORT",
                    color = sportColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 7.sp,
                )
            }
            Text(
                text = "CHRONO",
                color = NeonYellow,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "CLOCK",
                color = PixelGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 5.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.BikeRacer(visible: Boolean, racing: Boolean) {
    if (!visible) return

    // Animatable so we get a real slide-in from -0.2 (off-screen left) → 1.4 (off-screen right).
    val xFraction = remember { Animatable(-0.2f) }
    LaunchedEffect(racing) {
        if (racing) {
            xFraction.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(durationMillis = 880, easing = LinearOutSlowInEasing),
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val w = maxWidth
        val targetX = (w.value * xFraction.value).dp - 48.dp

        // 4-frame fading trail behind the main bike for chunky motion-blur vibe.
        for (i in 0..3) {
            val trailOffset = (i + 1) * 28
            Text(
                text = "🏍️",
                fontSize = 96.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = targetX - trailOffset.dp)
                    .alpha(0.08f * (4 - i)),
            )
        }
        Text(
            text = "🏍️",
            fontSize = 96.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = targetX),
        )
    }
}

@Composable
private fun BoxScope.LoadingBar(phase: SplashPhase) {
    if (phase < SplashPhase.Title || phase >= SplashPhase.Go) return

    val cellCount = 10
    val targetCells = when (phase) {
        SplashPhase.Title -> 3f
        SplashPhase.Race -> 8f
        else -> cellCount.toFloat()
    }
    val progress by animateFloatAsState(
        targetValue = targetCells,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        label = "loading-progress",
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "LOADING",
            color = NeonYellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
        )
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .border(2.dp, NeonYellow, RoundedCornerShape(2.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (i in 0 until cellCount) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(14.dp)
                        .background(
                            if (i < progress) NeonYellow else Color.Transparent,
                            RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.GoFlash(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.3f, animationSpec = tween(120)) + fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
        modifier = Modifier.align(Alignment.Center),
    ) {
        val infinite = rememberInfiniteTransition(label = "go")
        val flashAlpha by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 160
                    1f at 0
                    0.25f at 80
                    1f at 160
                },
            ),
            label = "go-flash",
        )
        Box {
            // Red+yellow stacked shadow for chunky arcade letters
            Text(
                text = "GO!",
                color = NeonYellow.copy(alpha = 0.6f),
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 10.sp,
                modifier = Modifier.offset(x = 4.dp, y = 4.dp),
            )
            Text(
                text = "GO!",
                color = ArcadeRed,
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 10.sp,
                modifier = Modifier.alpha(flashAlpha),
            )
        }
    }
}

@Composable
private fun BoxScope.WipeOverlay(visible: Boolean) {
    val sweep by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = LinearEasing),
        label = "wipe",
    )
    if (sweep > 0f) {
        Canvas(modifier = Modifier.align(Alignment.Center).fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sweepWidth = (w + h * 0.5f) * sweep
            // Diagonal swipe: shape sweeps in from the right, slanted
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w - sweepWidth, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w - sweepWidth + h * 0.4f, h)
                close()
            }
            drawPath(path, color = Color.Black)
        }
    }
}
