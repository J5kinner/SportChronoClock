package com.sportchronoclock.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_UNITS = "speed_units"
private const val KEY_DISPLAY_MODE = "display_mode"
private const val KEY_VOICE = "voice_cues_enabled"
private const val KEY_SPORT_ON_START = "sport_on_start"

class SettingsRepository(private val settings: Settings) {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserSettings> = _state.asStateFlow()

    private fun load(): UserSettings = UserSettings(
        speedUnits = settings.parseEnum(KEY_UNITS, SpeedUnits.KMH) { SpeedUnits.valueOf(it) },
        displayMode = settings.parseEnum(KEY_DISPLAY_MODE, DisplayMode.NIGHT) { DisplayMode.valueOf(it) },
        voiceCuesEnabled = settings.getBoolean(KEY_VOICE, true),
        sportModeOnStart = settings.getBoolean(KEY_SPORT_ON_START, false),
    )

    fun setUnits(value: SpeedUnits) {
        settings.putString(KEY_UNITS, value.name)
        _state.value = _state.value.copy(speedUnits = value)
    }

    fun setDisplayMode(value: DisplayMode) {
        settings.putString(KEY_DISPLAY_MODE, value.name)
        _state.value = _state.value.copy(displayMode = value)
    }

    fun setVoiceCues(value: Boolean) {
        settings.putBoolean(KEY_VOICE, value)
        _state.value = _state.value.copy(voiceCuesEnabled = value)
    }

    fun setSportOnStart(value: Boolean) {
        settings.putBoolean(KEY_SPORT_ON_START, value)
        _state.value = _state.value.copy(sportModeOnStart = value)
    }
}

private inline fun <T> Settings.parseEnum(key: String, default: T, parse: (String) -> T): T {
    val raw = getStringOrNull(key) ?: return default
    return runCatching { parse(raw) }.getOrDefault(default)
}
