package com.sportchronoclock.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

actual class TtsEngine(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var initialized = false
    private val pending = mutableListOf<String>()
    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttrs)
            .setWillPauseWhenDucked(false)
            .build()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(audioAttrs)
                initialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        audioManager.abandonAudioFocusRequest(focusRequest)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        audioManager.abandonAudioFocusRequest(focusRequest)
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        audioManager.abandonAudioFocusRequest(focusRequest)
                    }
                })
                pending.toList().forEach { speak(it) }
                pending.clear()
            }
        }
    }

    actual fun speak(text: String) {
        val engine = tts
        if (engine == null || !initialized) {
            pending += text
            return
        }
        val granted = audioManager.requestAudioFocus(focusRequest)
        if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    actual fun stop() {
        tts?.stop()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    actual fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}
