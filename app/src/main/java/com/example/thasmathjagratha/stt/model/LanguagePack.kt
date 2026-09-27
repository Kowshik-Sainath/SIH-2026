package com.example.thasmathjagratha.stt.model

import java.util.Locale

/**
 * Data class representing an offline STT language pack loaded via sherpa-onnx.
 */
data class LanguagePack(
    val languageCode: String,
    val displayName: String,
    val nativeName: String,
    val modelFileName: String,
    val dataFileName: String?,
    val tokensFileName: String,
    val modelSizeBytes: Long,
    val downloadBaseUrl: String,
    val isDownloaded: Boolean = false,
    val localPath: String = "",
    val hotwords: List<String> = emptyList(),
    val license: String = "Apache-2.0",
    val expectedAccuracyWer: String = "~8-14% WER on benchmark"
) {
    val modelSizeMb: String
        get() = String.format(Locale.US, "%.1f MB", modelSizeBytes / (1024.0 * 1024.0))
}
