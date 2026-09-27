package com.example.thasmathjagratha.stt.audio

import android.content.Context
import android.util.Log
import com.example.thasmathjagratha.stt.telemetry.SttTelemetry
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Stage-2 ML-based Voice Activity Detector (Silero VAD v4/v5 ONNX) running on top
 * of the Stage-1 energy-gate detector.
 *
 * Runs on-device via sherpa-onnx's built-in Vad engine.
 * Only called when Stage-1 energy gate flags candidate speech.
 * Enforces a configurable minimum speech duration (default 150 ms).
 */
class SileroVadDetector(private val context: Context) {

    companion object {
        const val MIN_SPEECH_DURATION_MS = 150L
        private const val TAG = "SileroVadDetector"
    }

    private var vad: Vad? = null

    // Telemetry counters
    var stage1CandidateFrames: Long = 0L
        private set
    var stage2ConfirmedFrames: Long = 0L
        private set
    var stage2RejectedFrames: Long = 0L
        private set

    init {
        try {
            ensureModelAsset()
            val modelFile = File(context.filesDir, "silero_vad.onnx")
            val config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = modelFile.absolutePath,
                    threshold = 0.5f,
                    minSilenceDuration = 0.5f,
                    minSpeechDuration = (MIN_SPEECH_DURATION_MS / 1000.0f),
                    windowSize = 512
                ),
                sampleRate = 16000,
                numThreads = 1,
                provider = "cpu",
                debug = false
            )
            vad = Vad(null, config)
            Log.i(TAG, "Native Silero VAD initialized successfully from ${modelFile.absolutePath}")
            SttTelemetry.addLog("[SileroVAD] Initialized Stage-2 ML VAD (minSpeech=${MIN_SPEECH_DURATION_MS}ms)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize native Silero VAD: ${e.message}", e)
            SttTelemetry.addLog("[SileroVAD] Init notice: ${e.message}")
        }
    }

    private fun ensureModelAsset() {
        val dest = File(context.filesDir, "silero_vad.onnx")
        if (!dest.exists() || dest.length() < 100_000L) {
            context.assets.open("silero_vad.onnx").use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Extracted silero_vad.onnx from assets (${dest.length()} bytes)")
        }
    }

    /**
     * Evaluates a candidate audio frame flagged by Stage 1.
     * Returns true if Silero VAD confirms human speech.
     */
    fun evaluateCandidate(samples: FloatArray): Boolean {
        stage1CandidateFrames++
        val instance = vad
        if (instance == null) {
            // If ML VAD couldn't initialize on this device, accept Stage 1 candidate
            stage2ConfirmedFrames++
            return true
        }

        return try {
            instance.acceptWaveform(samples)
            val isSpeech = instance.isSpeechDetected()
            if (isSpeech) {
                stage2ConfirmedFrames++
            } else {
                stage2RejectedFrames++
            }
            isSpeech
        } catch (e: Exception) {
            Log.w(TAG, "Silero VAD evaluation error: ${e.message}")
            stage2ConfirmedFrames++
            true
        }
    }

    fun getStatsString(): String {
        val rejectionRate = if (stage1CandidateFrames > 0) {
            (stage2RejectedFrames.toFloat() / stage1CandidateFrames * 100).toInt()
        } else 0
        return "Stage 1 candidates: $stage1CandidateFrames | Stage 2 confirmed: $stage2ConfirmedFrames | Rejected false-positives: $stage2RejectedFrames ($rejectionRate%)"
    }

    fun reset() {
        try {
            vad?.reset()
        } catch (e: Exception) {
            Log.w(TAG, "Error resetting Vad", e)
        }
    }

    fun release() {
        try {
            vad?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Vad", e)
        } finally {
            vad = null
        }
    }
}
