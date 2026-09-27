package com.example.thasmathjagratha

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thasmathjagratha.tts.engine.TtsEngine
import com.example.thasmathjagratha.tts.engine.TtsMode
import com.example.thasmathjagratha.tts.playback.TtsAudioPlayer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TtsDeviceTest {
    @Test fun downloadSynthesizeAndPlayTelugu() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = TtsEngine(context)
        Log.i("TtsDeviceTest", "Downloading shared Rasa pack")
        engine.packManager.download("te")
        assertTrue(listOf("bn", "kn", "ml", "mr", "ta", "te")
            .all { engine.packManager.isPackDownloaded(it) })
        engine.init("te")
        val result = engine.synthesize("సహాయం కావాలి", TtsMode.NORMAL)
        assertTrue(result.samples.isNotEmpty())
        assertTrue(result.sampleRate > 0)
        Log.i("TtsDeviceTest", "PCM samples=${result.samples.size} rate=${result.sampleRate} synth=${result.synthesisTimeMs}ms RTF=${result.realTimeFactor}")
        TtsAudioPlayer(context).play(result)
        Log.i("TtsDeviceTest", "NORMAL playback completed")
        engine.destroy()
    }
}
