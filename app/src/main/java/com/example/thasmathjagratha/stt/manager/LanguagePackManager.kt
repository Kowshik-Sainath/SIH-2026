package com.example.thasmathjagratha.stt.manager

import android.content.Context
import android.util.Log
import com.example.thasmathjagratha.stt.decoder.DomainPhraseBiasing
import com.example.thasmathjagratha.stt.model.LanguagePack
import com.example.thasmathjagratha.stt.telemetry.SttTelemetry
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Manages model downloading, verification, storage, and single-instance recognizer allocation
 * using sherpa-onnx as the single on-device ASR runtime.
 * Odia (or) is explicitly out of scope.
 */
class LanguagePackManager(private val context: Context) {

    private val baseDir = File(context.filesDir, "stt_models")
    private var activeRecognizer: OfflineRecognizer? = null
    private var activeLanguageCode: String? = null

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    /**
     * Supported offline STT language packs:
     * - 9 Indic languages from nmukthap/vertexvoice-indic (Apache-2.0)
     * - English from csukuangfj/sherpa-onnx-nemo-ctc-en-conformer-small (Apache-2.0)
     * - Odia is explicitly excluded.
     */
    val availablePacks: List<LanguagePack>
        get() = listOf(
            createIndicPack("te", "Telugu", "తెలుగు"),
            createIndicPack("hi", "Hindi", "हिन्दी"),
            createIndicPack("ta", "Tamil", "தமிழ்"),
            createIndicPack("kn", "Kannada", "ಕನ್ನಡ"),
            createIndicPack("ml", "Malayalam", "മലയാളം"),
            createIndicPack("mr", "Marathi", "मराठी"),
            createIndicPack("gu", "Gujarati", "ગુજરાતી"),
            createIndicPack("bn", "Bengali", "বাংলা"),
            createIndicPack("pa", "Punjabi", "ਪੰਜਾਬੀ"),
            LanguagePack(
                languageCode = "en",
                displayName = "English",
                nativeName = "English",
                modelFileName = "model.int8.onnx",
                dataFileName = null,
                tokensFileName = "tokens.txt",
                modelSizeBytes = 46_431_465L, // 46.4 MB model + 11.6 KB tokens
                downloadBaseUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-ctc-en-conformer-small/resolve/main",
                isDownloaded = isPackDownloaded("en"),
                localPath = getPackDir("en").absolutePath,
                hotwords = DomainPhraseBiasing.getHotwords("en"),
                license = "Apache-2.0",
                expectedAccuracyWer = "~8-12% WER on NeMo English benchmark"
            )
        )

    private fun createIndicPack(
        code: String,
        displayName: String,
        nativeName: String
    ): LanguagePack {
        return LanguagePack(
            languageCode = code,
            displayName = displayName,
            nativeName = nativeName,
            modelFileName = "model.int8.opt.onnx",
            dataFileName = "model.int8.opt.onnx.data",
            tokensFileName = "vocab.txt",
            modelSizeBytes = 137_356_767L, // ~131 MB total (graph + 136.5 MB external weights + vocab + featurizer)
            downloadBaseUrl = "https://huggingface.co/nmukthap/vertexvoice-indic/resolve/main/$code",
            isDownloaded = isPackDownloaded(code),
            localPath = getPackDir(code).absolutePath,
            hotwords = DomainPhraseBiasing.getHotwords(code),
            license = "Apache-2.0",
            expectedAccuracyWer = "~10-14% WER on Indic benchmark"
        )
    }

    /**
     * Instantiates sherpa-onnx OfflineRecognizer for the given language pack.
     * Enforces single-instance RAM constraint by explicitly releasing any previous instance.
     */
    @Synchronized
    fun loadLanguage(pack: LanguagePack): OfflineRecognizer {
        if (activeLanguageCode == pack.languageCode && activeRecognizer != null) {
            return activeRecognizer!!
        }

        // Release previous recognizer to free RAM
        releaseActiveRecognizer()

        val dir = getPackDir(pack.languageCode)
        val modelFile = File(dir, pack.modelFileName)
        val tokensFile = File(dir, pack.tokensFileName)

        require(modelFile.exists() && modelFile.length() > 0L) {
            "Model file missing for ${pack.displayName}: ${modelFile.absolutePath}"
        }
        require(tokensFile.exists() && tokensFile.length() > 0L) {
            "Tokens file missing for ${pack.displayName}: ${tokensFile.absolutePath}"
        }

        if (pack.dataFileName != null) {
            val dataFile = File(dir, pack.dataFileName)
            require(dataFile.exists() && dataFile.length() > 100_000_000L) {
                "External weights data missing for ${pack.displayName}: ${dataFile.absolutePath}"
            }
        }

        val hotwordsFile = File(dir, "hotwords.txt")
        val hotwordsList = if (pack.hotwords.isNotEmpty()) pack.hotwords else DomainPhraseBiasing.getHotwords(pack.languageCode)
        if (hotwordsList.isNotEmpty()) {
            hotwordsFile.writeText(hotwordsList.joinToString("\n"))
        }

        if (pack.languageCode != "en") {
            ensureIndicMetadata(modelFile)
        }

        // Configure OfflineRecognizer via sherpa-onnx AAR
        val featConfig = FeatureConfig(
            sampleRate = 16000,
            featureDim = 80
        )

        val modelConfig = OfflineModelConfig(
            nemo = OfflineNemoEncDecCtcModelConfig(model = modelFile.absolutePath),
            tokens = tokensFile.absolutePath,
            numThreads = 2,
            provider = "cpu",
            debug = false
        )

        val config = OfflineRecognizerConfig(
            featConfig = featConfig,
            modelConfig = modelConfig,
            hotwordsFile = "",
            decodingMethod = "greedy_search"
        )

        val recognizer = try {
            OfflineRecognizer(config = config)
        } catch (t: Throwable) {
            Log.e("LanguagePackManager", "OfflineRecognizer creation failed for ${pack.displayName}", t)
            SttTelemetry.addLog("[sherpa-onnx] ERROR initializing [${pack.languageCode}]: ${t.message}")
            throw IllegalStateException("Failed to initialize offline recognizer for ${pack.displayName}: ${t.message}", t)
        }
        activeRecognizer = recognizer
        activeLanguageCode = pack.languageCode

        SttTelemetry.addLog("[sherpa-onnx] Successfully loaded STT recognizer for [${pack.languageCode}] (${pack.displayName})")
        return recognizer
    }

    private fun ensureIndicMetadata(modelFile: File) {
        try {
            if (!modelFile.exists() || modelFile.length() == 0L) return
            var bytes = modelFile.readBytes()

            // 1. Check if the corrupt opset 27 header prefix [8, 13, 58, 0, 66, 2, 16, 27] is present
            val badHeader = byteArrayOf(8, 13, 58, 0, 66, 2, 16, 27)
            val badIdx = indexOfSubarray(bytes, badHeader)
            if (badIdx != -1) {
                Log.w("LanguagePackManager", "Found corrupted Opset 27 patch in ${modelFile.name} at offset $badIdx. Stripping...")
                bytes = bytes.copyOfRange(0, badIdx)
                modelFile.writeBytes(bytes)
            }

            // 2. Check if clean vocab_size metadata is already present in the ONNX model
            val tailString = String(bytes.takeLast(2048).toByteArray(), Charsets.ISO_8859_1)
            if (!tailString.contains("vocab_size")) {
                // Clean NeMo CTC conformer metadata: ONLY metadata_props (tag 14), NO opset/header injection
                val cleanPatchBytes = android.util.Base64.decode(
                    "chEKCnZvY2FiX3NpemUSAzI1N3IdCg5ub3JtYWxpemVfdHlwZRILcGVyX2ZlYXR1cmVyFwoSc3Vic2FtcGxpbmdfZmFjdG9yEgE0ch8KCm1vZGVsX3R5cGUSEUVuY0RlY0NUQ01vZGVsQlBFcgwKB3ZlcnNpb24SATFyFAoMbW9kZWxfYXV0aG9yEgRuZW1v",
                    android.util.Base64.DEFAULT
                )
                modelFile.appendBytes(cleanPatchBytes)
                Log.i("LanguagePackManager", "Appended clean NeMo CTC conformer metadata to ${modelFile.name}")
            }
        } catch (e: Exception) {
            Log.w("LanguagePackManager", "Could not verify/patch ONNX metadata: ${e.message}")
        }
    }

    private fun indexOfSubarray(array: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || array.size < target.size) return -1
        outer@ for (i in 0..array.size - target.size) {
            for (j in target.indices) {
                if (array[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    @Synchronized
    fun releaseActiveRecognizer() {
        try {
            activeRecognizer?.release()
        } catch (e: Exception) {
            Log.w("LanguagePackManager", "Error releasing recognizer", e)
        } finally {
            activeRecognizer = null
            activeLanguageCode = null
            SttTelemetry.addLog("[sherpa-onnx] Released STT recognizer")
        }
    }

    fun getPackDir(languageCode: String): File {
        val dir = File(baseDir, languageCode)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isPackDownloaded(languageCode: String): Boolean {
        val dir = getPackDir(languageCode)
        if (languageCode == "en") {
            val model = File(dir, "model.int8.onnx")
            val tokens = File(dir, "tokens.txt")
            return model.exists() && model.length() > 40_000_000L && tokens.exists() && tokens.length() > 1_000L
        } else {
            val graph = File(dir, "model.int8.opt.onnx")
            val data = File(dir, "model.int8.opt.onnx.data")
            val vocab = File(dir, "vocab.txt")
            return graph.exists() && graph.length() > 500_000L &&
                    data.exists() && data.length() > 130_000_000L &&
                    vocab.exists() && vocab.length() > 1_000L
        }
    }

    fun getModelFile(languageCode: String): File {
        val name = if (languageCode == "en") "model.int8.onnx" else "model.int8.opt.onnx"
        return File(getPackDir(languageCode), name)
    }

    fun getTokensFile(languageCode: String): File {
        val name = if (languageCode == "en") "tokens.txt" else "vocab.txt"
        return File(getPackDir(languageCode), name)
    }

    /**
     * Robustly downloads all files required for the specified language pack.
     * Streams in chunks, supports HTTP Range resume, verifies byte sizes, and reports live progress.
     */
    suspend fun downloadLanguagePack(
        pack: LanguagePack,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetDir = getPackDir(pack.languageCode)

        try {
            if (pack.languageCode == "en") {
                // English: model.int8.onnx (46.4 MB) + tokens.txt (11.6 KB)
                val modelFile = File(targetDir, pack.modelFileName)
                val modelUrl = "${pack.downloadBaseUrl}/${pack.modelFileName}"
                val modelResult = ModelDownloader.downloadFile(modelUrl, modelFile, 46_419_854L) { _, _, p ->
                    onProgress(p * 0.95f)
                }
                if (modelResult.isFailure) return@withContext modelResult

                val tokensFile = File(targetDir, pack.tokensFileName)
                val tokensUrl = "${pack.downloadBaseUrl}/${pack.tokensFileName}"
                val tokensResult = ModelDownloader.downloadFile(tokensUrl, tokensFile, 11_611L) { _, _, _ ->
                    onProgress(1.0f)
                }
                if (tokensResult.isFailure) return@withContext tokensResult

            } else {
                // Indic: model.int8.opt.onnx (~739 KB), model.int8.opt.onnx.data (136.5 MB), vocab.txt, mel_filters.json, hanning_window.json
                val graphFile = File(targetDir, pack.modelFileName)
                val graphUrl = "${pack.downloadBaseUrl}/${pack.modelFileName}"
                val graphResult = ModelDownloader.downloadFile(graphUrl, graphFile) { _, _, _ ->
                    onProgress(0.02f)
                }
                if (graphResult.isFailure) return@withContext graphResult

                val dataFile = File(targetDir, pack.dataFileName ?: "model.int8.opt.onnx.data")
                val dataUrl = "${pack.downloadBaseUrl}/${pack.dataFileName ?: "model.int8.opt.onnx.data"}"
                val dataResult = ModelDownloader.downloadFile(dataUrl, dataFile, 136_498_688L) { _, _, p ->
                    onProgress(0.02f + p * 0.95f)
                }
                if (dataResult.isFailure) return@withContext dataResult

                val vocabFile = File(targetDir, pack.tokensFileName)
                val vocabUrl = "${pack.downloadBaseUrl}/${pack.tokensFileName}"
                val vocabResult = ModelDownloader.downloadFile(vocabUrl, vocabFile) { _, _, _ -> }
                if (vocabResult.isFailure) return@withContext vocabResult

                // Download auxiliary featurizer files if not present
                val melFile = File(targetDir, "mel_filters.json")
                val melUrl = "${pack.downloadBaseUrl}/mel_filters.json"
                ModelDownloader.downloadFile(melUrl, melFile) { _, _, _ -> }

                val hanningFile = File(targetDir, "hanning_window.json")
                val hanningUrl = "${pack.downloadBaseUrl}/hanning_window.json"
                ModelDownloader.downloadFile(hanningUrl, hanningFile) { _, _, _ -> }

                onProgress(1.0f)
            }

            SttTelemetry.addLog("[${pack.languageCode}] Downloaded and verified all offline STT model files")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LanguagePackManager", "Failed to download pack for ${pack.languageCode}", e)
            SttTelemetry.addLog("[${pack.languageCode}] STT Download Failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun deleteLanguagePack(languageCode: String): Boolean {
        if (activeLanguageCode == languageCode) {
            releaseActiveRecognizer()
        }
        val dir = getPackDir(languageCode)
        return if (dir.exists()) {
            dir.deleteRecursively()
            SttTelemetry.addLog("[$languageCode] STT Language pack deleted")
            true
        } else false
    }
}
