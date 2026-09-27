package com.example.thasmathjagratha.tts.engine

import android.content.Context
import android.os.SystemClock
import com.example.thasmathjagratha.tts.manager.TtsLanguagePackManager
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class TtsMode { ALERT, NORMAL }

data class TtsResult(
    val samples: ShortArray,
    val sampleRate: Int,
    val synthesisTimeMs: Long,
    val realTimeFactor: Double,
    val languageCode: String,
    val mode: TtsMode
)

interface TtsListener {
    fun onAudioReady(samples: ShortArray, sampleRate: Int) {}
    fun onError(error: Throwable) {}
}

/** One native OfflineTts instance maximum. Six Rasa languages share that instance. */
class TtsEngine(
    context: Context,
    val packManager: TtsLanguagePackManager = TtsLanguagePackManager(context)
) {
    private val mutex = Mutex()
    private var loadedGroup: String? = null
    private var nativeTts: OfflineTts? = null
    private var activeCode: String? = null
    private var listener: TtsListener? = null

    fun setListener(listener: TtsListener) { this.listener = listener }

    suspend fun init(languageCode: String) = withContext(Dispatchers.Default) {
        mutex.withLock { initLocked(languageCode) }
    }

    private fun initLocked(code: String) {
        val pack = packManager.getPack(code)
        require(packManager.isPackDownloaded(code)) { "Download the ${pack.displayName} TTS pack first" }
        if (loadedGroup == pack.modelGroup && nativeTts != null) {
            activeCode = code
            return
        }
        nativeTts?.release()
        nativeTts = null
        loadedGroup = null
        val dir = File(pack.localPath)
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = File(dir, pack.modelFileName).absolutePath,
                    tokens = File(dir, "tokens.txt").absolutePath,
                    dataDir = if (pack.modelGroup == "vits_rasa_13") "" else packManager.espeakDataPath()
                ),
                numThreads = 2,
                provider = "cpu"
            )
        )
        val start = SystemClock.elapsedRealtime()
        val loaded = OfflineTts(config = config)
        nativeTts = loaded
        loadedGroup = pack.modelGroup
        activeCode = code
        TtsTelemetry.logLoad(pack.modelGroup, SystemClock.elapsedRealtime() - start, pack.modelSizeBytes)
    }

    suspend fun synthesize(text: String, mode: TtsMode): TtsResult = withContext(Dispatchers.Default) {
        try {
            val result = mutex.withLock {
                val code = requireNotNull(activeCode) { "Call init(languageCode) first" }
                val pack = packManager.getPack(code)
                val tts = requireNotNull(nativeTts)
                val normalized = TtsTextNormalizer.normalize(text, code)
                val start = SystemClock.elapsedRealtime()
                val config = GenerationConfig(
                    sid = pack.defaultSpeakerId ?: 0,
                    speed = if (mode == TtsMode.ALERT && pack.alertEmotionId == null) 1.08f else 1f,
                    extra = pack.alertEmotionId?.let {
                        mapOf("emotion_id" to (if (mode == TtsMode.ALERT) it else pack.normalEmotionId!!).toString())
                    }
                )
                val generated = tts.generateWithConfig(normalized, config)
                val elapsed = SystemClock.elapsedRealtime() - start
                require(generated.sampleRate > 0 && generated.samples.isNotEmpty()) { "TTS returned no audio" }
                val pcm = ShortArray(generated.samples.size) { i ->
                    (generated.samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                val duration = generated.samples.size.toDouble() * 1000.0 / generated.sampleRate
                val rtf = elapsed / duration
                TtsTelemetry.logSynthesis(code, elapsed, rtf)
                TtsResult(pcm, generated.sampleRate, elapsed, rtf, code, mode)
            }
            listener?.onAudioReady(result.samples, result.sampleRate)
            result
        } catch (error: Throwable) {
            listener?.onError(error)
            throw error
        }
    }

    suspend fun destroy() = withContext(Dispatchers.Default) {
        mutex.withLock {
            nativeTts?.release()
            nativeTts = null
            loadedGroup = null
            activeCode = null
        }
    }
}

/** Rasa uses a character frontend; Piper uses its bundled eSpeak frontend. */
internal object TtsTextNormalizer {
    private val digitWords = mapOf(
        "bn" to "শূন্য এক দুই তিন চার পাঁচ ছয় সাত আট নয়",
        "kn" to "ಸೊನ್ನೆ ಒಂದು ಎರಡು ಮೂರು ನಾಲ್ಕು ಐದು ಆರು ಏಳು ಎಂಟು ಒಂಬತ್ತು",
        "ml" to "പൂജ്യം ഒന്ന് രണ്ട് മൂന്ന് നാല് അഞ്ച് ആറ് ഏഴ് എട്ട് ഒമ്പത്",
        "mr" to "शून्य एक दोन तीन चार पाच सहा सात आठ नऊ",
        "ta" to "பூஜ்ஜியம் ஒன்று இரண்டு மூன்று நான்கு ஐந்து ஆறு ஏழு எட்டு ஒன்பது",
        "te" to "సున్నా ఒకటి రెండు మూడు నాలుగు ఐదు ఆరు ఏడు ఎనిమిది తొమ్మిది"
    ).mapValues { it.value.split(' ') }

    fun normalize(text: String, languageCode: String): String {
        val compact = text.trim().replace(Regex("\\s+"), " ")
        require(compact.isNotEmpty()) { "Enter text to speak" }
        // Rasa's sherpa frontend can synthesize a noisy extra fragment after trailing punctuation.
        val words = digitWords[languageCode] ?: return compact // Piper/eSpeak handles numerals itself.
        val expanded = Regex("\\p{Nd}+").replace(compact) { number ->
            number.value.map { digit -> words[Character.digit(digit, 10)] }.joinToString(" ")
        }
        return expanded.trimEnd('.', '?', '!', '।', '॥')
    }
}
