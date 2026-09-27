package com.example.thasmathjagratha.tts.telemetry

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TtsTelemetry {
    private val mutableLines = MutableStateFlow<List<String>>(emptyList())
    val lines = mutableLines.asStateFlow()
    fun logLoad(group: String, millis: Long, modelBytes: Long) = add(
        "TTS load $group: ${millis}ms, model file ~${modelBytes / 1_000_000}MB"
    )
    fun logSynthesis(code: String, millis: Long, rtf: Double) = add(
        "TTS $code: ${millis}ms, RTF ${"%.2f".format(java.util.Locale.US, rtf)}"
    )
    fun logPlayback(alertId: String, mode: String) = add("TTS playback complete: $alertId ($mode)")
    private fun add(line: String) {
        Log.i("TtsTelemetry", line)
        mutableLines.value = (mutableLines.value + line).takeLast(40)
    }
}
