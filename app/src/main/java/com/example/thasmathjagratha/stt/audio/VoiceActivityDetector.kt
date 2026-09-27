package com.example.thasmathjagratha.stt.audio

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Energy-based Voice Activity Detector (VAD) with adaptive noise floor estimation
 * and silence hangover thresholding (~400-600 ms).
 */
class VoiceActivityDetector(
    private val sampleRate: Int = 16000,
    private val frameSizeMs: Int = 20,
    private val energyThresholdDb: Float = 14.0f, // dB above noise floor
    private val silenceHangoverMs: Int = 500
) {
    private val frameSamples = (sampleRate * frameSizeMs) / 1000
    private val hangoverFrames = silenceHangoverMs / frameSizeMs

    private var noiseFloorRms = 0.005f // initial quiet estimate
    private var hangoverCounter = 0
    private var isSpeechActive = false

    /**
     * Process audio frame and determine if speech is present.
     * @param pcmSamples PCM audio buffer [-1.0f, 1.0f]
     * @return VadResult containing active status, RMS level, and whether speech ended
     */
    fun processFrame(pcmSamples: FloatArray): VadResult {
        if (pcmSamples.isEmpty()) {
            return VadResult(isSpeech = false, rms = 0.0f, isSpeechEnded = false)
        }

        var sumSquare = 0.0f
        for (sample in pcmSamples) {
            sumSquare += sample * sample
        }
        val currentRms = sqrt(sumSquare / pcmSamples.size)

        // Update noise floor dynamically during quiet moments
        if (currentRms < noiseFloorRms * 1.5f) {
            noiseFloorRms = 0.95f * noiseFloorRms + 0.05f * currentRms
        }
        noiseFloorRms = max(noiseFloorRms, 0.001f)

        // Ratio in dB above noise floor
        val ratioDb = 20.0f * log10(max(currentRms, 1e-6f) / noiseFloorRms)
        val isFrameSpeech = ratioDb >= energyThresholdDb && currentRms > 0.005f

        var speechEnded = false

        if (isFrameSpeech) {
            isSpeechActive = true
            hangoverCounter = hangoverFrames
        } else {
            if (isSpeechActive) {
                hangoverCounter--
                if (hangoverCounter <= 0) {
                    isSpeechActive = false
                    speechEnded = true
                }
            }
        }

        return VadResult(
            isSpeech = isSpeechActive,
            rms = currentRms,
            isSpeechEnded = speechEnded
        )
    }

    fun reset() {
        hangoverCounter = 0
        isSpeechActive = false
        noiseFloorRms = 0.005f
    }

    data class VadResult(
        val isSpeech: Boolean,
        val rms: Float,
        val isSpeechEnded: Boolean
    )
}
