package com.example.thasmathjagratha.model

data class MeshStatus(
    val status: String = "Connected & Relaying",
    val nearbyPeers: Int = 12,
    val messagesRelayed: Int = 38,
    val lastAlertId: String = "AP-FLOOD-1029",
    val signalQuality: String = "Good (92%)"
)
