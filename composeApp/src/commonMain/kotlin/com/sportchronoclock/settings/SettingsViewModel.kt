package com.sportchronoclock.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<UserSettings> = repository.state

    fun setUnits(value: SpeedUnits) = repository.setUnits(value)
    fun setDisplayMode(value: DisplayMode) = repository.setDisplayMode(value)
    fun setVoiceCues(value: Boolean) = repository.setVoiceCues(value)
    fun setSportOnStart(value: Boolean) = repository.setSportOnStart(value)
}
