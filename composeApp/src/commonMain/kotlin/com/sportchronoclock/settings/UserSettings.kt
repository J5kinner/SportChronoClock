package com.sportchronoclock.settings

enum class SpeedUnits { KMH, MPH }

enum class SpeedoSkin { BMW_M, TRACK_BLACK }

enum class DisplayMode { DAY, NIGHT, AUTO }

data class UserSettings(
    val speedUnits: SpeedUnits = SpeedUnits.KMH,
    val speedoSkin: SpeedoSkin = SpeedoSkin.BMW_M,
    val displayMode: DisplayMode = DisplayMode.NIGHT,
    val voiceCuesEnabled: Boolean = true,
    val sportModeOnStart: Boolean = false,
)
