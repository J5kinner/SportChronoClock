package com.sportchronoclock.media

data class MediaInfo(
    val hasSession: Boolean,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
)

data class MediaAccessState(
    val hasPermission: Boolean,
    val needsOnboarding: Boolean,
)
