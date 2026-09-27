package com.example.thasmathjagratha.tts.model

data class TtsLanguagePack(
    val languageCode: String,
    val displayName: String,
    val modelGroup: String,
    val modelFileName: String,
    val configFileName: String?,
    val modelSizeBytes: Long,
    val downloadUrl: String,
    val isDownloaded: Boolean,
    val localPath: String,
    val defaultSpeakerId: Int?,
    val alertEmotionId: Int?,
    val normalEmotionId: Int?
)
