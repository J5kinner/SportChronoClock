package com.sportchronoclock.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MediaControlViewModel(
    private val mediaController: MediaController,
) : ViewModel() {

    init {
        mediaController.start()
    }

    val info: StateFlow<MediaInfo> = mediaController.info
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaInfo(hasSession = false, title = null, artist = null, isPlaying = false),
        )

    val accessState: StateFlow<MediaAccessState> = mediaController.accessState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaAccessState(hasPermission = true, needsOnboarding = false),
        )

    fun togglePlay() = mediaController.togglePlay()
    fun skipNext() = mediaController.skipNext()
    fun skipPrevious() = mediaController.skipPrevious()
    fun requestAccess() = mediaController.requestAccess()

    override fun onCleared() {
        super.onCleared()
        mediaController.stop()
    }
}
