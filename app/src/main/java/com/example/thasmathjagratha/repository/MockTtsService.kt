package com.example.thasmathjagratha.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TtsPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val language: String = "Telugu",
    val text: String = "",
    val speechSpeed: Float = 1.0f
)

/** Legacy UI state holder. Actual speech is produced by TtsEngine/OfflineTts. */
class MockTtsService(@Suppress("UNUSED_PARAMETER") context: Context? = null) {
    private val _ttsState = MutableStateFlow(TtsPlaybackState())
    val ttsState: StateFlow<TtsPlaybackState> = _ttsState.asStateFlow()

    fun play(text: String, language: String) {
        _ttsState.value = _ttsState.value.copy(isPlaying = true, isPaused = false,
            language = language, text = text)
    }

    fun pause() {
        _ttsState.value = _ttsState.value.copy(isPlaying = false, isPaused = true)
    }

    fun stop() {
        _ttsState.value = _ttsState.value.copy(isPlaying = false, isPaused = false)
    }

    fun setSpeed(speed: Float) {
        _ttsState.value = _ttsState.value.copy(speechSpeed = speed)
    }

    fun shutdown() { stop() }
}
