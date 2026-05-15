package com.sportchronoclock.tts

expect class TtsEngine {
    fun speak(text: String)
    fun stop()
    fun release()
}
