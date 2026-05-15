package com.sportchronoclock.media

import kotlinx.coroutines.flow.Flow

interface MediaController {
    val info: Flow<MediaInfo>
    val accessState: Flow<MediaAccessState>

    fun start()
    fun stop()

    fun togglePlay()
    fun skipNext()
    fun skipPrevious()

    /** Platform-specific: open the OS settings panel to grant Notification Listener access. */
    fun requestAccess()
}
