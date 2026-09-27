package com.example.thasmathjagratha.repository

import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.MeshReceiverStatus
import com.example.thasmathjagratha.model.ReceivedAlert
import com.example.thasmathjagratha.model.ReceiverProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockReceiverRepository {

    private val now = System.currentTimeMillis()
    private val oneHour = 3600 * 1000L

    private val _receiverProfile = MutableStateFlow(
        ReceiverProfile(
            userId = "RCV-AP-9901",
            name = "Ramesh Kumar",
            preferredLanguage = "Telugu",
            district = "Vijayawada East",
            state = "Andhra Pradesh",
            verified = true,
            meshRelayEnabled = true
        )
    )
    val receiverProfile: StateFlow<ReceiverProfile> = _receiverProfile.asStateFlow()

    private val _meshStatus = MutableStateFlow(
        MeshReceiverStatus(
            connected = true,
            nearbyPeers = 12,
            alertsReceived = 8,
            alertsForwarded = 6,
            duplicatesBlocked = 14,
            signal = "Good (92%)"
        )
    )
    val meshStatus: StateFlow<MeshReceiverStatus> = _meshStatus.asStateFlow()

    private val _receivedAlerts = MutableStateFlow<List<ReceivedAlert>>(
        listOf(
            ReceivedAlert(
                alertId = "AP-FLOOD-1029",
                alertType = "Flood Warning",
                authority = "District Disaster Management Authority",
                message = "Move away from low-lying areas immediately. Water level rising fast near Krishna river.",
                translatedMessage = "తక్కువ ప్రాంతాల నుండి వెంటనే సురక్షిత ప్రాంతాలకు వెళ్లండి.",
                originalLanguage = "English",
                preferredLanguage = "Telugu",
                severity = AlertSeverity.CRITICAL,
                location = "Vijayawada East",
                issuedAt = now - (5 * 60 * 1000L),
                expiryTime = now + (60 * 60 * 1000L),
                verified = true,
                acknowledged = false,
                expired = false,
                hopCount = 4,
                maxHops = 10,
                receivedVia = "Mesh Relay P2P",
                forwarded = true,
                forwardedDeviceCount = 7
            ),
            ReceivedAlert(
                alertId = "AP-ROAD-2041",
                alertType = "Road Closure",
                authority = "Field Traffic Command",
                message = "Road closed on MG Road due to tree fall and electrical lines damage.",
                translatedMessage = "చెట్లు కూలిపోవడం మరియు విద్యుత్ మార్గాల నష్టం కారణంగా MG రోడ్డు మూసివేయబడింది.",
                originalLanguage = "English",
                preferredLanguage = "Telugu",
                severity = AlertSeverity.HIGH,
                location = "MG Road, Vijayawada",
                issuedAt = now - (20 * 60 * 1000L),
                expiryTime = now + (2 * oneHour),
                verified = true,
                acknowledged = true,
                expired = false,
                hopCount = 2,
                maxHops = 10,
                receivedVia = "Direct Bluetooth Mesh",
                forwarded = true,
                forwardedDeviceCount = 5
            ),
            ReceivedAlert(
                alertId = "AP-RAIN-3005",
                alertType = "Heavy Rain Warning",
                authority = "State Meteorological Dept",
                message = "Heavy rainfall warning expected over Vijayawada & Guntur districts.",
                translatedMessage = "విజయవాడ మరియు గుంటూరు జిల్లాల్లో భారీ వర్ష సూచన ఉంది.",
                originalLanguage = "Hindi",
                preferredLanguage = "Telugu",
                severity = AlertSeverity.IMPORTANT,
                location = "Vijayawada",
                issuedAt = now - oneHour,
                expiryTime = now + (5 * oneHour),
                verified = true,
                acknowledged = false,
                expired = false,
                hopCount = 3,
                maxHops = 10,
                receivedVia = "Mesh Node 14",
                forwarded = true,
                forwardedDeviceCount = 4
            ),
            ReceivedAlert(
                alertId = "AP-CYCLONE-0992",
                alertType = "Cyclone Advisory",
                authority = "State Disaster Management",
                message = "Coastal winds expected to subside. Advisory period expired.",
                translatedMessage = "తీరప్రాంత గాలులు తగ్గుముఖం పట్టే అవకాశం ఉంది. హెచ్చరిక వ్యవధి ముగిసింది.",
                originalLanguage = "English",
                preferredLanguage = "Telugu",
                severity = AlertSeverity.NORMAL,
                location = "Coastal Area",
                issuedAt = now - (24 * oneHour),
                expiryTime = now - (2 * oneHour),
                verified = true,
                acknowledged = true,
                expired = true,
                hopCount = 5,
                maxHops = 10,
                receivedVia = "Relay Hub 01",
                forwarded = false,
                forwardedDeviceCount = 0
            )
        )
    )
    val receivedAlerts: StateFlow<List<ReceivedAlert>> = _receivedAlerts.asStateFlow()

    fun addReceivedAlert(alert: ReceivedAlert) {
        if (_receivedAlerts.value.any { it.alertId == alert.alertId }) return
        _receivedAlerts.value = listOf(alert) + _receivedAlerts.value
        _meshStatus.value = _meshStatus.value.copy(alertsReceived = _meshStatus.value.alertsReceived + 1)
    }

    fun simulateIncomingAlert(): ReceivedAlert {
        val newId = "AP-FLOOD-${(1000..9999).random()}"
        val newAlert = ReceivedAlert(
            alertId = newId,
            alertType = "Flood Warning",
            authority = "District Disaster Management Authority",
            message = "Move away from low-lying areas immediately.",
            translatedMessage = translateText("Move away from low-lying areas immediately.", _receiverProfile.value.preferredLanguage),
            originalLanguage = "English",
            preferredLanguage = _receiverProfile.value.preferredLanguage,
            severity = AlertSeverity.CRITICAL,
            location = "Vijayawada East",
            issuedAt = System.currentTimeMillis(),
            expiryTime = System.currentTimeMillis() + (45 * 60 * 1000L),
            verified = true,
            acknowledged = false,
            expired = false,
            hopCount = 4,
            maxHops = 10,
            receivedVia = "Mesh Relay P2P",
            forwarded = true,
            forwardedDeviceCount = 7
        )

        _receivedAlerts.value = listOf(newAlert) + _receivedAlerts.value
        _meshStatus.value = _meshStatus.value.copy(
            alertsReceived = _meshStatus.value.alertsReceived + 1,
            alertsForwarded = _meshStatus.value.alertsForwarded + 1
        )
        return newAlert
    }

    fun acknowledgeAlert(alertId: String) {
        _receivedAlerts.value = _receivedAlerts.value.map { alert ->
            if (alert.alertId == alertId) alert.copy(acknowledged = true) else alert
        }
    }

    fun updatePreferredLanguage(newLang: String) {
        _receiverProfile.value = _receiverProfile.value.copy(preferredLanguage = newLang)
        _receivedAlerts.value = _receivedAlerts.value.map { alert ->
            if (alert.receivedVia == "Wi-Fi" || alert.receivedVia == "Bluetooth") return@map alert
            alert.copy(
                preferredLanguage = newLang,
                translatedMessage = translateText(alert.message, newLang)
            )
        }
    }

    fun toggleMeshRelay() {
        _receiverProfile.value = _receiverProfile.value.copy(
            meshRelayEnabled = !_receiverProfile.value.meshRelayEnabled
        )
    }

    fun simulateDuplicateBlocked() {
        _meshStatus.value = _meshStatus.value.copy(
            duplicatesBlocked = _meshStatus.value.duplicatesBlocked + 1
        )
    }

    private fun translateText(original: String, lang: String): String {
        return when (lang) {
            "Telugu" -> "తక్కువ ప్రాంతాల నుండి వెంటనే సురక్షిత ప్రాంతాలకు వెళ్లండి."
            "Hindi" -> "तुरंत निचले इलाकों से सुरक्षित स्थानों पर चले जाएं।"
            "Tamil" -> "தாழ்வான பகுதிகளில் இருந்து உடனடியாக பாதுகாப்பான இடத்திற்கு செல்லவும்."
            "Kannada" -> "ತಗ್ಗು ಪ್ರದೇಶಗಳಿಂದ ತಕ್ಷಣ ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತೆರಳಿ."
            "Malayalam" -> "താഴ്ന്ന പ്രദേശങ്ങളിൽ നിന്ന് ഉടൻ സുരക്ഷിതമായ സ്ഥലത്തേക്ക് മാറുക."
            "Marathi" -> "सखल भागातून त्वरित सुरक्षित ठिकाणी जा."
            "Bengali" -> "অবিলম্বে নিচু এলাকা থেকে নিরাপদ স্থানে চলে যান।"
            "Gujarati" -> "તરત જ નીચાણવાળા વિસ્તારોમાંથી સુરક્ષિત સ્થળે ખસી જાઓ."
            "Odia" -> "ତୁରନ୍ତ ତଳିଆ ଅଞ୍ଚଳରୁ ସୁରକ୍ଷିତ ସ୍ଥାନକୁ ଚାଲିଯାଅ।"
            else -> original
        }
    }
}
