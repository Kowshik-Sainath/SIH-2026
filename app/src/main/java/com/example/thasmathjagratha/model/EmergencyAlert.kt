package com.example.thasmathjagratha.model

data class EmergencyAlert(
    val alertId: String,
    val authority: String,
    val alertType: String,
    val message: String,
    val translatedMessage: String,
    val originalLanguage: String,
    val preferredLanguage: String,
    val severity: AlertSeverity,
    val targetArea: String,
    val issuedAt: Long,
    val expiryTime: Long,
    val verified: Boolean,
    val acknowledged: Boolean = false,
    val hopCount: Int = 1,
    val maxHops: Int = 10,
    val isDuplicateBlocked: Boolean = false,
    val isExpired: Boolean = false,
    val receivedVia: String = ""
)
