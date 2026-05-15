package com.sportchronoclock.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController as PlatformMediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidMediaController(private val context: Context) : MediaController {

    private val sessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent = ComponentName(context, NotificationListener::class.java)

    private val _info = MutableStateFlow(
        MediaInfo(hasSession = false, title = null, artist = null, isPlaying = false)
    )
    override val info: Flow<MediaInfo> = _info.asStateFlow()

    private val _accessState = MutableStateFlow(
        MediaAccessState(hasPermission = false, needsOnboarding = true)
    )
    override val accessState: Flow<MediaAccessState> = _accessState.asStateFlow()

    private var activeController: PlatformMediaController? = null

    private val callback = object : PlatformMediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateInfo()
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateInfo()
        override fun onSessionDestroyed() {
            activeController = null
            updateInfo()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToFirst(controllers)
        }

    override fun start() {
        refreshAccessState()
        if (!_accessState.value.hasPermission) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            attachToFirst(sessionManager.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
            _accessState.value = MediaAccessState(hasPermission = false, needsOnboarding = true)
        }
    }

    override fun stop() {
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: SecurityException) {
        }
        activeController?.unregisterCallback(callback)
        activeController = null
    }

    private fun refreshAccessState() {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )
        val granted = enabled?.contains(context.packageName) == true
        _accessState.value = MediaAccessState(
            hasPermission = granted,
            needsOnboarding = !granted,
        )
    }

    private fun attachToFirst(controllers: List<PlatformMediaController>?) {
        activeController?.unregisterCallback(callback)
        activeController = controllers?.firstOrNull()
        activeController?.registerCallback(callback)
        updateInfo()
    }

    private fun updateInfo() {
        val ac = activeController
        if (ac == null) {
            _info.value = MediaInfo(hasSession = false, title = null, artist = null, isPlaying = false)
            return
        }
        val md = ac.metadata
        val ps = ac.playbackState
        _info.value = MediaInfo(
            hasSession = true,
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            isPlaying = ps?.state == PlaybackState.STATE_PLAYING,
        )
    }

    override fun togglePlay() {
        val ac = activeController ?: return
        val playing = ac.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) ac.transportControls.pause() else ac.transportControls.play()
    }

    override fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    override fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    override fun requestAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
