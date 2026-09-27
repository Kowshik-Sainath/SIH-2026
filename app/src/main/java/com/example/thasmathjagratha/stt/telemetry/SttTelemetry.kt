package com.example.thasmathjagratha.stt.telemetry

import java.util.Locale

/**
 * Telemetry data and metrics for offline STT model execution on mobile hardware.
 */
data class TelemetrySnapshot(
    val languageCode: String,
    val modelLoadTimeMs: Long = 0,
    val avgInferenceTimeMs: Float = 0.0f,
    val totalInferenceCount: Int = 0,
    val lastChunkLatencyMs: Long = 0,
    val realTimeFactor: Float = 0.0f, // Processing Time / Audio Duration
    val memoryUsageMb: Float = 0.0f,
    val logs: List<String> = emptyList()
)

object SttTelemetry {

    private var activeLanguageCode: String = "hi"
    private var modelLoadTimeMs: Long = 0
    private var totalInferenceTimeMs: Long = 0
    private var totalAudioDurationMs: Long = 0
    private var totalInferenceCount: Int = 0
    private var lastChunkLatencyMs: Long = 0
    private val logList = ArrayList<String>()

    @Synchronized
    fun logModelLoad(languageCode: String, loadDurationMs: Long) {
        activeLanguageCode = languageCode
        modelLoadTimeMs = loadDurationMs
        addLog("[$languageCode] Model loaded in ${loadDurationMs}ms")
    }

    @Synchronized
    fun logInference(audioDurationMs: Long, inferenceTimeMs: Long) {
        totalInferenceCount++
        totalInferenceTimeMs += inferenceTimeMs
        totalAudioDurationMs += audioDurationMs
        lastChunkLatencyMs = inferenceTimeMs

        val rtf = if (audioDurationMs > 0) inferenceTimeMs.toFloat() / audioDurationMs else 0f
        addLog("[$activeLanguageCode] Chunk infer: ${inferenceTimeMs}ms (audio: ${audioDurationMs}ms, RTF: ${String.format(Locale.US, "%.2f", rtf)})")
    }

    @Synchronized
    fun addLog(msg: String) {
        if (logList.size > 100) {
            logList.removeAt(0)
        }
        logList.add(msg)
    }

    @Synchronized
    fun getSnapshot(): TelemetrySnapshot {
        val runtime = Runtime.getRuntime()
        val usedMemBytes = runtime.totalMemory() - runtime.freeMemory()
        val usedMemMb = usedMemBytes / (1024.0f * 1024.0f)

        val avgInference = if (totalInferenceCount > 0) {
            totalInferenceTimeMs.toFloat() / totalInferenceCount
        } else 0.0f

        val rtf = if (totalAudioDurationMs > 0) {
            totalInferenceTimeMs.toFloat() / totalAudioDurationMs
        } else 0.0f

        return TelemetrySnapshot(
            languageCode = activeLanguageCode,
            modelLoadTimeMs = modelLoadTimeMs,
            avgInferenceTimeMs = avgInference,
            totalInferenceCount = totalInferenceCount,
            lastChunkLatencyMs = lastChunkLatencyMs,
            realTimeFactor = rtf,
            memoryUsageMb = usedMemMb,
            logs = ArrayList(logList)
        )
    }

    @Synchronized
    fun reset() {
        totalInferenceTimeMs = 0
        totalAudioDurationMs = 0
        totalInferenceCount = 0
        lastChunkLatencyMs = 0
        logList.clear()
    }
}
