package com.example.thasmathjagratha.tts.manager

import android.content.Context
import android.util.Log
import com.example.thasmathjagratha.stt.manager.ModelDownloader
import com.example.thasmathjagratha.tts.model.TtsLanguagePack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages on-device TTS voice packs:
 * - vits_rasa_13 (shared single model.onnx + tokens.txt for Bengali, Kannada, Malayalam, Marathi, Tamil, Telugu)
 * - Piper voices for Hindi (hi_IN-rohan-medium) and English (en_US-lessac-medium)
 * - Gujarati and Odia are explicitly guarded as unsupported.
 */
class TtsLanguagePackManager(context: Context) {
    private val root = File(context.filesDir, "tts-packs")
    private val rasaBase = "https://huggingface.co/MatiasLin/sherpa-onnx-vits-rasa-13/resolve/main"
    private val piperBase = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
    private val espeakUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"

    private data class Spec(val code: String, val name: String, val group: String, val speaker: Int?)
    private val specs = listOf(
        Spec("bn", "Bengali", "vits_rasa_13", 2),
        Spec("kn", "Kannada", "vits_rasa_13", 8),
        Spec("ml", "Malayalam", "vits_rasa_13", 11),
        Spec("mr", "Marathi", "vits_rasa_13", 12),
        Spec("ta", "Tamil", "vits_rasa_13", 18),
        Spec("te", "Telugu", "vits_rasa_13", 19),
        Spec("hi", "Hindi", "hi_IN-rohan-medium", null),
        Spec("en", "English", "en_US-lessac-medium", null)
    )

    val availablePacks: List<TtsLanguagePack>
        get() = specs.map { spec ->
            val rasa = spec.group == "vits_rasa_13"
            val path = File(root, spec.group)
            val model = if (rasa) "model.onnx" else "${spec.group}.onnx"
            val source = when (spec.code) {
                "hi" -> "$piperBase/hi/hi_IN/rohan/medium/$model"
                "en" -> "$piperBase/en/en_US/lessac/medium/$model"
                else -> "$rasaBase/model.onnx"
            }
            TtsLanguagePack(
                languageCode = spec.code,
                displayName = spec.name,
                modelGroup = spec.group,
                modelFileName = model,
                configFileName = if (rasa) null else "$model.json",
                modelSizeBytes = if (rasa) 123_338_635L else if (spec.code == "hi") 62_950_044L else 63_201_294L,
                downloadUrl = source,
                isDownloaded = isPackDownloaded(spec.code),
                localPath = path.absolutePath,
                defaultSpeakerId = spec.speaker,
                alertEmotionId = if (rasa) 10 else null,
                normalEmotionId = if (rasa) 4 else null
            )
        }

    fun getPack(code: String): TtsLanguagePack =
        availablePacks.firstOrNull { it.languageCode.equals(code, ignoreCase = true) }
            ?: throw IllegalArgumentException("TTS is not supported for language '$code'. Gujarati and Odia are currently out of scope.")

    fun isPackDownloaded(code: String): Boolean {
        val spec = specs.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: return false
        val dir = File(root, spec.group)
        val model = File(dir, if (spec.group == "vits_rasa_13") "model.onnx" else "${spec.group}.onnx")
        val tokens = File(dir, "tokens.txt")
        if (!model.isFile || model.length() < 1_000_000L || !tokens.isFile || tokens.length() == 0L) return false
        if (spec.group == "vits_rasa_13") return true
        return File(dir, "${spec.group}.onnx.json").isFile &&
            File(dir, "converted.ok").isFile && File(root, "espeak-ng-data/phontab").isFile
    }

    /** A download for any Rasa language installs the same shared pack for all six Indic languages. */
    suspend fun download(code: String, onProgress: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        val pack = getPack(code)
        if (isPackDownloaded(code)) {
            onProgress(100)
            return@withContext
        }

        val dir = File(pack.localPath).apply { mkdirs() }
        val modelFile = File(dir, pack.modelFileName)

        Log.i("TtsPackManager", "Starting download for ${pack.displayName} (${pack.modelGroup})...")
        val modelResult = ModelDownloader.downloadFile(pack.downloadUrl, modelFile, pack.modelSizeBytes) { _, _, progress ->
            onProgress((progress * 80).toInt())
        }
        if (modelResult.isFailure) {
            throw modelResult.exceptionOrNull() ?: IOException("Failed to download ${pack.modelFileName}")
        }

        if (pack.modelGroup == "vits_rasa_13") {
            val tokensFile = File(dir, "tokens.txt")
            val tokensResult = ModelDownloader.downloadFile("$rasaBase/tokens.txt", tokensFile, 9_556L) { _, _, _ ->
                onProgress(100)
            }
            if (tokensResult.isFailure) {
                throw tokensResult.exceptionOrNull() ?: IOException("Failed to download Rasa tokens.txt")
            }
        } else {
            val jsonFile = File(dir, requireNotNull(pack.configFileName))
            val jsonResult = ModelDownloader.downloadFile("${pack.downloadUrl}.json", jsonFile) { _, _, _ ->
                onProgress(85)
            }
            if (jsonResult.isFailure) {
                throw jsonResult.exceptionOrNull() ?: IOException("Failed to download Piper config JSON")
            }

            val converted = File(dir, "converted.ok")
            if (!converted.isFile) {
                PiperPackConverter.convert(File(dir, pack.modelFileName), jsonFile, File(dir, "tokens.txt"))
                converted.writeText("sherpa-onnx Piper metadata installed")
            }
            ensureEspeakData { p -> onProgress(85 + (p * 0.15f).toInt()) }
            onProgress(100)
        }

        require(isPackDownloaded(code)) { "TTS pack is incomplete after download: $code" }
        Log.i("TtsPackManager", "TTS pack ${pack.displayName} installed and verified.")
    }

    /** Explicitly opt in to removing the shared pack; it serves all six Indic languages. */
    fun delete(code: String, allowSharedRemoval: Boolean = false) {
        val pack = getPack(code)
        if (pack.modelGroup == "vits_rasa_13" && !allowSharedRemoval) {
            throw IllegalStateException("This removes TTS for bn, kn, ml, mr, ta, and te; confirm shared removal")
        }
        File(pack.localPath).deleteRecursively()
    }

    fun espeakDataPath(): String = File(root, "espeak-ng-data").absolutePath

    private suspend fun ensureEspeakData(onProgress: (Float) -> Unit = {}) {
        if (File(root, "espeak-ng-data/phontab").isFile) {
            onProgress(1.0f)
            return
        }
        val archive = File(root, "espeak-ng-data.tar.bz2")
        val downloadResult = ModelDownloader.downloadFile(espeakUrl, archive) { _, _, p -> onProgress(p * 0.8f) }
        if (downloadResult.isFailure) {
            throw downloadResult.exceptionOrNull() ?: IOException("Failed to download espeak-ng-data archive")
        }

        withContext(Dispatchers.IO) {
            TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    val target = File(root, entry.name).canonicalFile
                    require(target.path.startsWith(root.canonicalPath + File.separator)) { "Unsafe archive path" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { tar.copyTo(it) }
                    }
                }
            }
        }
        archive.delete()
        check(File(root, "espeak-ng-data/phontab").isFile) { "Invalid espeak-ng-data archive: phontab not found" }
        onProgress(1.0f)
    }
}

/** Minimal protobuf append for ModelProto.metadata_props (field 14), equivalent to sherpa's piper.py. */
internal object PiperPackConverter {
    fun convert(model: File, jsonFile: File, tokens: File) {
        val config = JSONObject(jsonFile.readText())
        val ids = config.getJSONObject("phoneme_id_map")
        tokens.bufferedWriter().use { writer ->
            ids.keys().forEach { phoneme ->
                writer.append(phoneme).append(' ').append(ids.getJSONArray(phoneme).getInt(0).toString()).append('\n')
            }
        }
        val metadata = mapOf(
            "model_type" to "vits", "comment" to "piper",
            "language" to config.getJSONObject("language").getString("name_english"),
            "voice" to config.getJSONObject("espeak").getString("voice"),
            "has_espeak" to "1", "n_speakers" to config.getInt("num_speakers").toString(),
            "sample_rate" to config.getJSONObject("audio").getInt("sample_rate").toString()
        )
        FileOutputStream(model, true).use { output ->
            metadata.forEach { (key, value) ->
                val pair = field(1, key.toByteArray(Charsets.UTF_8)) + field(2, value.toByteArray(Charsets.UTF_8))
                output.write(field(14, pair))
            }
            output.fd.sync()
        }
    }

    private fun field(number: Int, bytes: ByteArray): ByteArray =
        varint((number * 8 + 2).toLong()) + varint(bytes.size.toLong()) + bytes

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val out = ArrayList<Byte>()
        while (remaining >= 128) {
            out.add(((remaining and 127) or 128).toByte())
            remaining = remaining ushr 7
        }
        out.add(remaining.toByte())
        return out.toByteArray()
    }
}
