package com.example.thasmathjagratha.model

data class ReceivedAlert(
    val alertId: String,
    val alertType: String,
    val authority: String,
    val message: String,
    val translatedMessage: String,
    val originalLanguage: String,
    val preferredLanguage: String,
    val severity: AlertSeverity,
    val location: String,
    val issuedAt: Long,
    val expiryTime: Long,
    val verified: Boolean,
    val acknowledged: Boolean = false,
    val expired: Boolean = false,
    val hopCount: Int = 1,
    val maxHops: Int = 10,
    val receivedVia: String = "Mesh Relay P2P",
    val forwarded: Boolean = true,
    val forwardedDeviceCount: Int = 7
)

data class ReceiverProfile(
    val userId: String = "RCV-AP-9901",
    val name: String = "Bhargav Ram",
    val preferredLanguage: String = "Telugu",
    val district: String = "Vijayawada East",
    val state: String = "Andhra Pradesh",
    val verified: Boolean = true,
    val meshRelayEnabled: Boolean = true
)

data class MeshReceiverStatus(
    val connected: Boolean = true,
    val nearbyPeers: Int = 12,
    val alertsReceived: Int = 8,
    val alertsForwarded: Int = 6,
    val duplicatesBlocked: Int = 14,
    val signal: String = "Good (92%)"
)
