package com.example.thasmathjagratha.model

data class EmergencyReport(
    val reportId: String,
    val userId: String,
    val userName: String,
    val emergencyType: AlertType,
    val location: String,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: String,
    val status: String = "Submitted to Control Room",
    val hasAudioAttachment: Boolean = false
)
