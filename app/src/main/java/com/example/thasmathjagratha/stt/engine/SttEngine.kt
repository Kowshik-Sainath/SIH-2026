package com.example.thasmathjagratha.stt.engine

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.thasmathjagratha.stt.audio.AudioRecorder
import com.example.thasmathjagratha.stt.audio.SileroVadDetector
import com.example.thasmathjagratha.stt.audio.VoiceActivityDetector
import com.example.thasmathjagratha.stt.manager.LanguagePackManager
import com.example.thasmathjagratha.stt.telemetry.SttTelemetry
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Listener interface for STT callbacks.
 */
interface SttListener {
    fun onPartialText(partialText: String) {}
    fun onFinalText(finalText: String)
    fun onError(error: Throwable)
    fun onAudioLevel(rmsLevel: Float) {}
    fun onProcessingStateChanged(isProcessing: Boolean) {}
}

/**
 * Fully offline, on-device Speech-To-Text (ASR) Engine for Android.
 * Runs 100% on-device using:
 * 1. Hardware PCM Audio capture (AudioRecord, 16 kHz Mono Float32)
 * 2. Stage-1 Energy-based Voice Activity Detection (VoiceActivityDetector)
 * 3. Stage-2 ML-based Voice Activity Detection (Silero VAD v4/v5 ONNX)
 * 4. sherpa-onnx OfflineRecognizer running NeMo CTC Conformer graphs locally
 *
 * Odia is explicitly out of scope.
 */
class SttEngine(
    private val context: Context,
    val packManager: LanguagePackManager = LanguagePackManager(context)
) {
    companion object {
        private const val TAG = "SttEngine"
        private const val SAMPLE_RATE = 16000
    }

    private var activeLanguageCode: String = "hi"
    private var activeRecognizer: OfflineRecognizer? = null
    private var listener: SttListener? = null

    private val audioRecorder = AudioRecorder(sampleRate = SAMPLE_RATE, frameSizeMs = 20)
    private val stage1Vad = VoiceActivityDetector(sampleRate = SAMPLE_RATE, frameSizeMs = 20, energyThresholdDb = 12.0f, silenceHangoverMs = 500)
    private val stage2Vad = SileroVadDetector(context)

    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var isListening = false

    // Audio buffer for the current utterance
    private val pcmBuffer = ArrayList<Float>(SAMPLE_RATE * 10) // pre-allocate ~10s capacity
    private val bufferLock = Any()

    fun setListener(listener: SttListener) {
        this.listener = listener
    }

    /**
     * Pre-initializes the sherpa-onnx recognizer for the specified language.
     * Enforces single-model RAM constraint by unloading any previous instance.
     */
    suspend fun init(languageCode: String) = withContext(Dispatchers.Default) {
        activeLanguageCode = languageCode
        val pack = packManager.availablePacks.find { it.languageCode == languageCode }
        if (pack != null && packManager.isPackDownloaded(languageCode)) {
            try {
                activeRecognizer = packManager.loadLanguage(pack)
                SttTelemetry.addLog("[SttEngine] Preloaded offline recognizer for [$languageCode]")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load recognizer for $languageCode", t)
                SttTelemetry.addLog("[SttEngine] Error loading [$languageCode]: ${t.message}")
                activeRecognizer = null
                throw t
            }
        } else {
            throw IllegalStateException("Language pack [$languageCode] is not downloaded or incomplete.")
        }
    }

    /**
     * Start recording microphone speech and decoding via the on-device pipeline.
     */
    fun startListening() {
        if (isListening) return
        if (activeRecognizer == null) {
            listener?.onError(IllegalStateException("Speech recognizer is not initialized. Please ensure the language pack is downloaded."))
            return
        }
        isListening = true

        synchronized(bufferLock) {
            pcmBuffer.clear()
        }
        stage1Vad.reset()
        stage2Vad.reset()

        listener?.onProcessingStateChanged(false)

        audioRecorder.startRecording(object : AudioRecorder.AudioChunkListener {
            override fun onAudioChunk(pcmFloatSamples: FloatArray) {
                if (!isListening || pcmFloatSamples.isEmpty()) return

                // Stage 1: Fast energy-gate VAD
                val stage1Result = stage1Vad.processFrame(pcmFloatSamples)
                listener?.onAudioLevel(stage1Result.rms.coerceIn(0.05f, 1.0f))

                // Stage 2: ML-based Silero VAD evaluation if Stage 1 flags speech
                val speechConfirmed = if (stage1Result.isSpeech) {
                    stage2Vad.evaluateCandidate(pcmFloatSamples)
                } else {
                    false
                }

                if (speechConfirmed || stage1Result.isSpeech) {
                    synchronized(bufferLock) {
                        for (sample in pcmFloatSamples) {
                            pcmBuffer.add(sample)
                        }
                    }
                }

                // If speech has concluded (hangover expired) and we have audio, process utterance
                if (stage1Result.isSpeechEnded) {
                    processBufferedAudio()
                }
            }

            override fun onError(error: Throwable) {
                isListening = false
                Log.e(TAG, "AudioRecorder error", error)
                listener?.onError(error)
            }
        })
    }

    /**
     * Stop listening (Push-To-Talk release) and finalize on-device speech recognition.
     */
    fun stopListening() {
        if (!isListening) return
        isListening = false
        audioRecorder.stopRecording()
        processBufferedAudio()
    }

    private fun processBufferedAudio() {
        val samples: FloatArray
        synchronized(bufferLock) {
            if (pcmBuffer.isEmpty()) return
            samples = pcmBuffer.toFloatArray()
            pcmBuffer.clear()
        }

        // Require at least 250ms of audio (4000 samples at 16kHz) to run inference
        if (samples.size < 4000) {
            Log.d(TAG, "Buffered audio too short (${samples.size} samples), skipping inference")
            return
        }

        engineScope.launch {
            decodeAudio(samples)
        }
    }

    private suspend fun decodeAudio(samples: FloatArray) = withContext(Dispatchers.Default) {
        val startTime = SystemClock.elapsedRealtime()
        val audioDurationMs = (samples.size * 1000L) / SAMPLE_RATE

        listener?.onProcessingStateChanged(true)

        val pack = packManager.availablePacks.find { it.languageCode == activeLanguageCode }
        if (pack == null || !packManager.isPackDownloaded(activeLanguageCode)) {
            listener?.onProcessingStateChanged(false)
            listener?.onError(IllegalStateException("Language pack for '$activeLanguageCode' is not downloaded. Please download it first."))
            return@withContext
        }

        try {
            val recognizer = activeRecognizer ?: packManager.loadLanguage(pack)
            activeRecognizer = recognizer

            Log.i(TAG, "Starting on-device sherpa-onnx decoding for $audioDurationMs ms audio in [${pack.displayName}]...")

            val stream = recognizer.createStream()
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            val recognizedText = result.text.trim()
            stream.release()

            val latencyMs = SystemClock.elapsedRealtime() - startTime
            Log.i(TAG, "On-device ASR complete in ${latencyMs}ms. Text: \"$recognizedText\"")

            SttTelemetry.logInference(audioDurationMs, latencyMs)
            SttTelemetry.addLog("[ASR ${pack.languageCode}] Latency: ${latencyMs}ms, Audio: ${audioDurationMs}ms -> \"$recognizedText\"")
            SttTelemetry.addLog("[VAD Stats] ${stage2Vad.getStatsString()}")

            withContext(Dispatchers.Main) {
                listener?.onProcessingStateChanged(false)
                if (recognizedText.isNotBlank()) {
                    listener?.onFinalText(recognizedText)
                } else {
                    listener?.onError(RuntimeException("No speech recognized in the recorded audio."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference error during on-device STT", e)
            withContext(Dispatchers.Main) {
                listener?.onProcessingStateChanged(false)
                listener?.onError(e)
            }
        }
    }

    fun isListening(): Boolean = isListening

    fun destroy() {
        stopListening()
        stage2Vad.release()
        packManager.releaseActiveRecognizer()
        activeRecognizer = null
    }
}
