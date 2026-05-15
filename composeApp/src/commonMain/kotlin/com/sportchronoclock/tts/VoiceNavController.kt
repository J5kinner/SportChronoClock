package com.sportchronoclock.tts

import com.sportchronoclock.format.UnitFormatter
import com.sportchronoclock.navigation.NavigationStep
import com.sportchronoclock.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val THRESHOLD_FAR_METERS = 200.0
private const val THRESHOLD_NEAR_METERS = 50.0

class VoiceNavController(
    private val ttsEngine: TtsEngine,
    private val settingsRepository: SettingsRepository,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private var lastStepKey: String? = null
    private var spokenFar = false
    private var spokenNear = false

    fun bind(
        stepFlow: StateFlow<NavigationStep?>,
        distanceFlow: StateFlow<Double?>,
    ) {
        job?.cancel()
        job = scope.launch {
            combine(stepFlow, distanceFlow, settingsRepository.state) { step, dist, settings ->
                Triple(step, dist, settings)
            }.collect { (step, dist, settings) ->
                handle(step, dist, settings.voiceCuesEnabled, settings.speedUnits.let { it })
            }
        }
    }

    fun unbind() {
        job?.cancel()
        job = null
        ttsEngine.stop()
    }

    private fun handle(
        step: NavigationStep?,
        distance: Double?,
        enabled: Boolean,
        units: com.sportchronoclock.settings.SpeedUnits,
    ) {
        if (!enabled || step == null || distance == null) {
            return
        }
        val stepKey = "${step.maneuverType}|${step.maneuverModifier}|${step.instruction}"
        if (stepKey != lastStepKey) {
            lastStepKey = stepKey
            spokenFar = false
            spokenNear = false
        }
        when {
            distance <= THRESHOLD_NEAR_METERS && !spokenNear -> {
                spokenNear = true
                ttsEngine.speak("Now, ${step.instruction}")
            }
            distance <= THRESHOLD_FAR_METERS && !spokenFar -> {
                spokenFar = true
                val far = UnitFormatter.shortDistanceLabel(THRESHOLD_FAR_METERS, units)
                ttsEngine.speak("In $far, ${step.instruction}")
            }
        }
    }
}
