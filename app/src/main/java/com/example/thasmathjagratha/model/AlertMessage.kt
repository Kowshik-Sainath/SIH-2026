package com.example.thasmathjagratha.model

enum class AlertType(val title: String) {
    FLOOD("Flood Warning"),
    FIRE("Fire Emergency"),
    ACCIDENT("Major Accident"),
    MEDICAL("Medical Alert"),
    CYCLONE("Cyclone Alert"),
    OTHER("Public Notice")
}

data class AlertMessage(
    val alertId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val message: String,
    val language: String,
    val priority: AlertSeverity,
    val location: String,
    val timestamp: String,
    val expiryTime: String,
    val verified: Boolean = true,
    val hopCount: Int = 1,
    val alertType: AlertType = AlertType.FLOOD,
    val radiusKm: Int = 10
)
