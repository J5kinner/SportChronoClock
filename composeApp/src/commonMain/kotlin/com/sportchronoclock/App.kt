package com.sportchronoclock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sportchronoclock.ride.RideRecorder
import com.sportchronoclock.tts.VoiceNavController
import com.sportchronoclock.ui.DashboardScreen
import com.sportchronoclock.ui.RideHistoryScreen
import com.sportchronoclock.ui.RideSummaryScreen
import com.sportchronoclock.ui.SettingsSheet
import com.sportchronoclock.ui.SplashScreen
import com.sportchronoclock.ui.SportHud
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val rideRecorder: RideRecorder = koinInject()
    val voiceNav: VoiceNavController = koinInject()
    val mainVm: MainViewModel = koinViewModel()

    LaunchedEffect(Unit) { rideRecorder.start() }
    DisposableEffect(Unit) {
        voiceNav.bind(mainVm.currentStep, mainVm.distanceToNextTurnMeters)
        onDispose { voiceNav.unbind() }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var splashDone by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main app renders underneath the splash so ViewModels, location flow, and the map
        // are all warm by the time the wipe completes.
        when (val s = screen) {
            is Screen.Dashboard -> DashboardScreen(
                onOpenHistory = { screen = Screen.History },
                onOpenSport = { screen = Screen.Sport },
                onOpenSettings = { screen = Screen.Settings },
            )
            is Screen.History -> RideHistoryScreen(
                onBack = { screen = Screen.Dashboard },
                onRideSelected = { id -> screen = Screen.Summary(id) },
            )
            is Screen.Summary -> RideSummaryScreen(
                rideId = s.rideId,
                onBack = { screen = Screen.History },
                onDelete = { screen = Screen.History },
            )
            is Screen.Sport -> SportHud(
                onExit = { screen = Screen.Dashboard },
            )
            is Screen.Settings -> SettingsSheet(
                onClose = { screen = Screen.Dashboard },
            )
        }

        if (!splashDone) {
            SplashScreen(onFinish = { splashDone = true })
        }
    }
}

private sealed interface Screen {
    data object Dashboard : Screen
    data object History : Screen
    data object Sport : Screen
    data object Settings : Screen
    data class Summary(val rideId: Long) : Screen
}
