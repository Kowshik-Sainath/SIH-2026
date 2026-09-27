package com.example.thasmathjagratha.repository

import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AppMessage
import com.example.thasmathjagratha.model.EmergencyAlert
import kotlinx.coroutines.delay

enum class SimulatedEventType {
    NORMAL_MESSAGE,
    IMPORTANT_ALERT,
    HIGH_ALERT,
    CRITICAL_GOVT_ALERT,
    UNVERIFIED_ALERT,
    DUPLICATE_ALERT,
    EXPIRED_ALERT
}

class MockIncomingAlertManager(
    private val alertRepository: MockAlertRepository,
    private val messageRepository: MockMessageRepository,
    private val translationService: MockTranslationService
) {

    suspend fun simulateEvent(
        type: SimulatedEventType,
        preferredLanguage: String,
        onNormalMessage: (AppMessage) -> Unit,
        onEmergencyAlert: (EmergencyAlert) -> Unit,
        onDuplicateBlocked: (String) -> Unit
    ) {
        delay(1500) // Simulate background transmission delay

        val now = System.currentTimeMillis()

        when (type) {
            SimulatedEventType.NORMAL_MESSAGE -> {
                val msg = AppMessage(
                    messageId = "MSG-${(1000..9999).random()}",
                    senderId = "RES-01",
                    senderName = "Rescue Team 01",
                    receiverId = "USR-AP-8842",
                    receiverName = "Bhargav Ram",
                    message = "Proceed to Sector 7 evacuation shelter for food packets and medical aid.",
                    language = preferredLanguage,
                    priority = AlertSeverity.NORMAL,
                    timestamp = now,
                    isSentByMe = false,
                    isRead = false
                )
                messageRepository.addMessage(msg)
                onNormalMessage(msg)
            }

            SimulatedEventType.IMPORTANT_ALERT -> {
                val alert = EmergencyAlert(
                    alertId = "AP-ROAD-${(1000..9999).random()}",
                    authority = "Field Traffic Command",
                    alertType = "Road Closure Warning",
                    message = "Tree fall and electrical damage near MG Road junction. Slow traffic expected.",
                    translatedMessage = translationService.translate("Tree fall and electrical damage near MG Road junction.", preferredLanguage),
                    originalLanguage = "English",
                    preferredLanguage = preferredLanguage,
                    severity = AlertSeverity.IMPORTANT,
                    targetArea = "Vijayawada Central",
                    issuedAt = now,
                    expiryTime = now + (2 * 3600 * 1000L),
                    verified = true,
                    acknowledged = false
                )
                alertRepository.addAlert(alert)
                onEmergencyAlert(alert)
            }

            SimulatedEventType.HIGH_ALERT -> {
                val alert = EmergencyAlert(
                    alertId = "AP-RAIN-${(1000..9999).random()}",
                    authority = "State Meteorological Dept",
                    alertType = "Heavy Rainfall Advisory",
                    message = "Heavy rainfall expected over Vijayawada & Guntur over next 6 hours.",
                    translatedMessage = translationService.translate("Heavy rainfall expected over Vijayawada & Guntur over next 6 hours.", preferredLanguage),
                    originalLanguage = "English",
                    preferredLanguage = preferredLanguage,
                    severity = AlertSeverity.HIGH,
                    targetArea = "Krishna District",
                    issuedAt = now,
                    expiryTime = now + (6 * 3600 * 1000L),
                    verified = true,
                    acknowledged = false
                )
                alertRepository.addAlert(alert)
                onEmergencyAlert(alert)
            }

            SimulatedEventType.CRITICAL_GOVT_ALERT -> {
                val newId = "AP-FLOOD-${(1000..9999).random()}"
                val alert = EmergencyAlert(
                    alertId = newId,
                    authority = "District Disaster Management Authority",
                    alertType = "Flood Warning",
                    message = "Move away from low-lying areas immediately. Water level rising fast near Krishna river.",
                    translatedMessage = translationService.translate("Move away from low-lying areas immediately.", preferredLanguage),
                    originalLanguage = "English",
                    preferredLanguage = preferredLanguage,
                    severity = AlertSeverity.CRITICAL,
                    targetArea = "Vijayawada East",
                    issuedAt = now,
                    expiryTime = now + (45 * 60 * 1000L),
                    verified = true,
                    acknowledged = false,
                    hopCount = 4,
                    maxHops = 10
                )
                alertRepository.addAlert(alert)
                onEmergencyAlert(alert)
            }

            SimulatedEventType.UNVERIFIED_ALERT -> {
                val alert = EmergencyAlert(
                    alertId = "UNV-ALERT-${(1000..9999).random()}",
                    authority = "Unknown Sender",
                    alertType = "Unverified Local Report",
                    message = "Unconfirmed report of bridge closure near Prakasam Barrage.",
                    translatedMessage = "ప్రకాశం బ్యారేజీ వద్ద వంతెన మూసివేత సంబంధించి ధృవీకరించని సమాచారం.",
                    originalLanguage = "English",
                    preferredLanguage = preferredLanguage,
                    severity = AlertSeverity.HIGH,
                    targetArea = "Vijayawada",
                    issuedAt = now,
                    expiryTime = now + (30 * 60 * 1000L),
                    verified = false,
                    acknowledged = false
                )
                alertRepository.addAlert(alert)
                onEmergencyAlert(alert)
            }

            SimulatedEventType.DUPLICATE_ALERT -> {
                val existingId = "AP-FLOOD-1029"
                if (alertRepository.isDuplicate(existingId)) {
                    onDuplicateBlocked(existingId)
                } else {
                    val alert = EmergencyAlert(
                        alertId = existingId,
                        authority = "District Disaster Management Authority",
                        alertType = "Flood Warning",
                        message = "Move away from low-lying areas immediately.",
                        translatedMessage = translationService.translate("Move away from low-lying areas immediately.", preferredLanguage),
                        originalLanguage = "English",
                        preferredLanguage = preferredLanguage,
                        severity = AlertSeverity.CRITICAL,
                        targetArea = "Vijayawada East",
                        issuedAt = now,
                        expiryTime = now + (30 * 60 * 1000L),
                        verified = true,
                        acknowledged = false
                    )
                    alertRepository.addAlert(alert)
                    onEmergencyAlert(alert)
                }
            }

            SimulatedEventType.EXPIRED_ALERT -> {
                val alert = EmergencyAlert(
                    alertId = "AP-CYCLONE-OLD",
                    authority = "State Disaster Management",
                    alertType = "Cyclone Advisory",
                    message = "Advisory period expired. Winds have subsided.",
                    translatedMessage = "హెచ్చరిక వ్యవధి ముగిసింది.",
                    originalLanguage = "English",
                    preferredLanguage = preferredLanguage,
                    severity = AlertSeverity.NORMAL,
                    targetArea = "Coastal Area",
                    issuedAt = now - (24 * 3600 * 1000L),
                    expiryTime = now - (2 * 3600 * 1000L),
                    verified = true,
                    acknowledged = true,
                    isExpired = true
                )
                alertRepository.addAlert(alert)
                onEmergencyAlert(alert)
            }
        }
    }
}
