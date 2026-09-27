package com.example.thasmathjagratha.tts.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.example.thasmathjagratha.tts.engine.TtsMode
import com.example.thasmathjagratha.tts.engine.TtsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** UI may stop NORMAL playback. ALERT playback has only the explicit acknowledge path. */
class TtsAudioPlayer(context: Context) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Volatile private var acknowledged = false
    @Volatile private var stopNormal = false
    @Volatile private var playingAlert = false

    fun stopNormal() { if (!playingAlert) stopNormal = true }
    fun acknowledgeAlert() { acknowledged = true }
    fun stop() { stopNormal(); acknowledgeAlert() }

    suspend fun play(result: TtsResult) = withContext(Dispatchers.IO) {
        val alert = result.mode == TtsMode.ALERT
        acknowledged = false
        stopNormal = false
        val stream = if (alert) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
        val previousVolume = if (alert) manager.getStreamVolume(stream) else 0
        val attributes = AudioAttributes.Builder()
            .setUsage(if (alert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val focus = if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(
            if (alert) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        ).setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (!alert && change <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stopNormal = true
        }.build() else null
        val granted = if (focus != null) manager.requestAudioFocus(focus) else
            @Suppress("DEPRECATION") manager.requestAudioFocus(null, stream,
                if (alert) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        check(granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "Audio focus unavailable" }
        playingAlert = alert
        var track: AudioTrack? = null
        try {
            if (alert) manager.setStreamVolume(stream, manager.getStreamMaxVolume(stream), 0)
            val format = AudioFormat.Builder().setSampleRate(result.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            val minimum = AudioTrack.getMinBufferSize(result.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "Unsupported TTS sample rate" }
            track = AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minimum, 4096)).build()
            track.play()
            var offset = 0
            while (offset < result.samples.size && !(if (alert) acknowledged else stopNormal)) {
                val written = track.write(result.samples, offset,
                    minOf(2048, result.samples.size - offset), AudioTrack.WRITE_BLOCKING)
                check(written > 0) { "AudioTrack write failed: $written" }
                offset += written
            }
            while (!acknowledged && !stopNormal &&
                track.playbackHeadPosition.toLong() < result.samples.size.toLong()) {
                kotlinx.coroutines.delay(40)
            }
        } finally {
            track?.stop()
            track?.release()
            if (alert) manager.setStreamVolume(stream, previousVolume, 0)
            if (focus != null) manager.abandonAudioFocusRequest(focus) else
                @Suppress("DEPRECATION") manager.abandonAudioFocus(null)
            playingAlert = false
        }
    }
}
