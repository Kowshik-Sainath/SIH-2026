package com.example.thasmathjagratha.stt.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android PCM Audio Recorder capturing 16,000 Hz, 16-bit PCM Mono audio on a background thread.
 * Supports automatic fallback across VOICE_RECOGNITION, MIC, and DEFAULT audio sources.
 */
class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val frameSizeMs: Int = 20
) {
    interface AudioChunkListener {
        fun onAudioChunk(pcmFloatSamples: FloatArray)
        fun onError(error: Throwable)
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val frameSizeSamples = (sampleRate * frameSizeMs) / 1000 // 320 samples for 20ms

    @SuppressLint("MissingPermission")
    fun startRecording(listener: AudioChunkListener) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, frameSizeSamples * 4 * 2)

        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        var initializedRecord: AudioRecord? = null

        for (src in sources) {
            try {
                val rec = AudioRecord(
                    src,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    initializedRecord = rec
                    break
                } else {
                    rec.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (initializedRecord == null) {
            listener.onError(IllegalStateException("Microphone hardware failed to initialize on device."))
            return
        }

        try {
            audioRecord = initializedRecord
            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val shortBuffer = ShortArray(frameSizeSamples)
                val floatBuffer = FloatArray(frameSizeSamples)

                while (isActive && isRecording) {
                    val readSamples = audioRecord?.read(shortBuffer, 0, frameSizeSamples) ?: -1
                    if (readSamples > 0) {
                        for (i in 0 until readSamples) {
                            floatBuffer[i] = shortBuffer[i] / 32768.0f
                        }
                        listener.onAudioChunk(
                            if (readSamples == frameSizeSamples) floatBuffer else floatBuffer.copyOf(readSamples)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            isRecording = false
            listener.onError(e)
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }
    }

    fun isRecording(): Boolean = isRecording
}
