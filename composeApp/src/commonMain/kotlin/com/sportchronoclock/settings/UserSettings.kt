package com.sportchronoclock.settings

enum class SpeedUnits { KMH, MPH }

enum class DisplayMode { DAY, NIGHT, AUTO }

data class UserSettings(
    val speedUnits: SpeedUnits = SpeedUnits.KMH,
    val displayMode: DisplayMode = DisplayMode.NIGHT,
    val voiceCuesEnabled: Boolean = true,
    val sportModeOnStart: Boolean = false,
)
