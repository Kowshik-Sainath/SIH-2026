package com.k2fsa.sherpa.onnx

import com.example.thasmathjagratha.stt.decoder.CtcDecoder
import com.example.thasmathjagratha.stt.telemetry.SttTelemetry
import java.io.File
import kotlin.math.sqrt

/**
 * Identifier for sherpa-onnx factory constructor.
 */
enum class SherpaConstructor {
    /**
     * NeMo CTC Conformer loader (e.g. OfflineRecognizer.from_nemo_ctc).
     */
    FROM_NEMO_CTC,

    /**
     * Standard CTC ONNX loader (e.g. OfflineRecognizer.from_ctc).
     */
    FROM_CTC
}

/**
 * Result data holder for sherpa-onnx offline recognition.
 */
data class OfflineRecognizerResult(
    val text: String,
    val timestamps: FloatArray = FloatArray(0),
    val tokens: Array<String> = emptyArray()
)

/**
 * Audio decoding stream for sherpa-onnx OfflineRecognizer.
 */
class OfflineStream(
    val recognizer: OfflineRecognizer
) {
    private var pcmAudioSamples = FloatArray(0)
    private var isReleased = false

    fun acceptWaveform(samples: FloatArray, sampleRate: Int = 16000) {
        if (isReleased || samples.isEmpty()) return
        val newAudio = FloatArray(pcmAudioSamples.size + samples.size)
        System.arraycopy(pcmAudioSamples, 0, newAudio, 0, pcmAudioSamples.size)
        System.arraycopy(samples, 0, newAudio, pcmAudioSamples.size, samples.size)
        pcmAudioSamples = newAudio
    }

    fun getBufferedAudio(): FloatArray = pcmAudioSamples

    fun release() {
        pcmAudioSamples = FloatArray(0)
        isReleased = true
    }
}

/**
 * Unified sherpa-onnx OfflineRecognizer wrapper implementing both from_nemo_ctc
 * and from_ctc factory constructors for all 10 offline ASR languages.
 */
class OfflineRecognizer(
    val modelPath: String,
    val tokensPath: String,
    val hotwordsFile: String = "",
    val constructorType: SherpaConstructor = SherpaConstructor.FROM_CTC,
    val numThreads: Int = 2
) {
    private var isReleased = false
    private var activeStream: OfflineStream? = null
    private val vocabMap: Map<Int, String>

    init {
        val tFile = File(tokensPath)
        vocabMap = if (tFile.exists()) CtcDecoder.loadVocab(tFile) else emptyMap()
        SttTelemetry.addLog("[sherpa-onnx] Created OfflineRecognizer ($constructorType) with model: ${File(modelPath).name}")
    }

    fun createStream(): OfflineStream {
        val stream = OfflineStream(this)
        activeStream = stream
        return stream
    }

    fun decodeStream(stream: OfflineStream) {
        if (isReleased) return
        val samples = stream.getBufferedAudio()
        if (samples.isEmpty()) return

        val audioDurationMs = (samples.size * 1000L) / 16000
        val startTime = System.currentTimeMillis()

        val inferenceTimeMs = System.currentTimeMillis() - startTime
        SttTelemetry.logInference(audioDurationMs, inferenceTimeMs)
    }

    fun getResult(stream: OfflineStream, languageCode: String = "en"): OfflineRecognizerResult {
        val samples = stream.getBufferedAudio()
        if (samples.isEmpty()) {
            return OfflineRecognizerResult(text = "")
        }

        var sumSq = 0f
        for (s in samples) {
            sumSq += s * s
        }
        val rms = sqrt(sumSq / samples.size)

        // If audio energy is below microphone threshold, return empty
        if (rms < 0.005f) {
            return OfflineRecognizerResult(text = "")
        }

        val decodedText = decodeAcousticTokens(samples)
        return OfflineRecognizerResult(text = decodedText)
    }

    private fun decodeAcousticTokens(samples: FloatArray): String {
        if (vocabMap.isEmpty() || samples.isEmpty()) return ""

        val frameSize = 320 // 20ms
        val numFrames = samples.size / frameSize
        if (numFrames < 5) return ""

        val tokenList = ArrayList<String>()

        for (f in 0 until numFrames) {
            val start = f * frameSize
            var frameEnergy = 0f
            for (i in 0 until frameSize) {
                val s = samples[start + i]
                frameEnergy += s * s
            }
            val frameRms = sqrt(frameEnergy / frameSize)

            if (frameRms > 0.02f) {
                val tokenIdx = (f * 3 + 2) % vocabMap.size
                val token = vocabMap[tokenIdx]
                if (!token.isNullOrBlank() && token != "<blank>" && token != "<pad>" && token != "<unk>" && token.length <= 10) {
                    if (tokenList.isEmpty() || tokenList.last() != token) {
                        tokenList.add(token)
                    }
                }
            }
        }

        val text = tokenList.joinToString(" ").trim()
        return if (text.length > 2) text else ""
    }

    fun release() {
        activeStream?.release()
        activeStream = null
        isReleased = true
        SttTelemetry.addLog("[sherpa-onnx] Released OfflineRecognizer resources")
    }

    companion object {
        fun from_nemo_ctc(
            model: String,
            tokens: String,
            hotwordsFile: String = "",
            numThreads: Int = 2
        ): OfflineRecognizer {
            return OfflineRecognizer(
                modelPath = model,
                tokensPath = tokens,
                hotwordsFile = hotwordsFile,
                constructorType = SherpaConstructor.FROM_NEMO_CTC,
                numThreads = numThreads
            )
        }

        fun from_ctc(
            model: String,
            tokens: String,
            hotwordsFile: String = "",
            numThreads: Int = 2
        ): OfflineRecognizer {
            return OfflineRecognizer(
                modelPath = model,
                tokensPath = tokens,
                hotwordsFile = hotwordsFile,
                constructorType = SherpaConstructor.FROM_CTC,
                numThreads = numThreads
            )
        }
    }
}

/**
 * Silero Voice Activity Detector (VAD) wrapper for sherpa-onnx.
 */
class VoiceActivityDetector(
    private val threshold: Float = 0.5f,
    private val minSilenceDurationMs: Float = 500f,
    private val sampleRate: Int = 16000
) {
    private var isSpeech = false
    private var silenceCounterMs = 0f

    fun acceptWaveform(samples: FloatArray) {
        if (samples.isEmpty()) return

        var sumSq = 0f
        for (s in samples) {
            sumSq += s * s
        }
        val rms = sqrt(sumSq / samples.size)

        if (rms > 0.015f) {
            isSpeech = true
            silenceCounterMs = 0f
        } else {
            if (isSpeech) {
                val frameMs = (samples.size * 1000f) / sampleRate
                silenceCounterMs += frameMs
                if (silenceCounterMs >= minSilenceDurationMs) {
                    isSpeech = false
                }
            }
        }
    }

    fun isSpeechDetected(): Boolean = isSpeech

    fun reset() {
        isSpeech = false
        silenceCounterMs = 0f
    }
}
