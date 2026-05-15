package com.sportchronoclock.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. Cross-app media control isn't possible via public APIs on iOS —
 * MPRemoteCommandCenter is for the playing app, not external observers, and
 * MPNowPlayingInfoCenter only exposes what *this* app sets. Riders use the
 * iOS lock screen / control center to drive Spotify or Apple Music in V1.
 * Reports no session so the [com.sportchronoclock.ui.MediaTile] simply hides.
 */
class IosMediaController : MediaController {

    private val _info = MutableStateFlow(
        MediaInfo(hasSession = false, title = null, artist = null, isPlaying = false)
    )
    override val info: Flow<MediaInfo> = _info.asStateFlow()

    private val _access = MutableStateFlow(
        MediaAccessState(hasPermission = true, needsOnboarding = false)
    )
    override val accessState: Flow<MediaAccessState> = _access.asStateFlow()

    override fun start() = Unit
    override fun stop() = Unit
    override fun togglePlay() = Unit
    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
    override fun requestAccess() = Unit
}
