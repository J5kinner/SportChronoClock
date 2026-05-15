package com.sportchronoclock.tts

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeVoicePrompt
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate

@OptIn(ExperimentalForeignApi::class)
actual class TtsEngine {

    private val synthesizer = AVSpeechSynthesizer()

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayback,
            mode = AVAudioSessionModeVoicePrompt,
            options = AVAudioSessionCategoryOptionDuckOthers or AVAudioSessionCategoryOptionMixWithOthers,
            error = null,
        )
    }

    actual fun speak(text: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            rate = AVSpeechUtteranceDefaultSpeechRate
        }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun release() {
        stop()
    }
}
