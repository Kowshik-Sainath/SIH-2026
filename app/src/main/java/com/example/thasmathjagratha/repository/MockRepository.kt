package com.example.thasmathjagratha.repository

import com.example.thasmathjagratha.model.AlertMessage
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AlertType
import com.example.thasmathjagratha.model.ChatMessage
import com.example.thasmathjagratha.model.EmergencyReport
import com.example.thasmathjagratha.model.MeshStatus
import com.example.thasmathjagratha.model.NearbyDevice
import com.example.thasmathjagratha.model.User
import com.example.thasmathjagratha.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockRepository {

    private val _currentUser = MutableStateFlow(
        User(
            userId = "USR-AP-8842",
            name = "Bhargav Ram",
            role = UserRole.CITIZEN,
            verified = true,
            preferredLanguage = "Telugu",
            district = "Vijayawada East",
            state = "Andhra Pradesh"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _alerts = MutableStateFlow<List<AlertMessage>>(
        listOf(
            AlertMessage(
                alertId = "AP-FLOOD-1029",
                senderId = "GOV-AP-01",
                senderName = "District Disaster Management Authority",
                senderRole = UserRole.GOVERNMENT,
                message = "Krishna river water level rising rapidly. Move away from low-lying areas of Vijayawada immediately. Evacuation centers open at ZP High School.",
                language = "Telugu",
                priority = AlertSeverity.CRITICAL,
                location = "Vijayawada East",
                timestamp = "5 mins ago",
                expiryTime = "Today, 06:00 PM",
                verified = true,
                hopCount = 1,
                alertType = AlertType.FLOOD,
                radiusKm = 10
            ),
            AlertMessage(
                alertId = "AP-ROAD-2041",
                senderId = "OFF-AP-07",
                senderName = "Field Traffic Command",
                senderRole = UserRole.FIELD_OFFICER,
                message = "Road closed on MG Road due to tree fall and electrical lines damage. Take alternative route via Bandar Road.",
                language = "English",
                priority = AlertSeverity.HIGH,
                location = "MG Road, Vijayawada",
                timestamp = "20 mins ago",
                expiryTime = "Today, 02:00 PM",
                verified = true,
                hopCount = 2,
                alertType = AlertType.ACCIDENT,
                radiusKm = 5
            ),
            AlertMessage(
                alertId = "AP-RAIN-3005",
                senderId = "GOV-AP-01",
                senderName = "State Meteorological Department",
                senderRole = UserRole.GOVERNMENT,
                message = "Heavy rainfall warning (70-110mm) expected over Krishna & Guntur districts over next 6 hours. Avoid unnecessary travel.",
                language = "Hindi",
                priority = AlertSeverity.IMPORTANT,
                location = "Krishna District",
                timestamp = "1 hour ago",
                expiryTime = "Tomorrow, 08:00 AM",
                verified = true,
                hopCount = 3,
                alertType = AlertType.CYCLONE,
                radiusKm = 25
            )
        )
    )
    val alerts: StateFlow<List<AlertMessage>> = _alerts.asStateFlow()

    private val _reports = MutableStateFlow<List<EmergencyReport>>(
        listOf(
            EmergencyReport(
                reportId = "REP-8801",
                userId = "USR-AP-8842",
                userName = "Bhargav Ram",
                emergencyType = AlertType.FLOOD,
                location = "Prakasam Barrage North Bank",
                severity = AlertSeverity.HIGH,
                message = "Water overflow observed near residential colony. 4 families need assistance.",
                timestamp = "15 mins ago",
                status = "Dispatched Rescue Unit 2",
                hasAudioAttachment = true
            ),
            EmergencyReport(
                reportId = "REP-8802",
                userId = "USR-AP-8842",
                userName = "Bhargav Ram",
                emergencyType = AlertType.MEDICAL,
                location = "Sector 4 Community Hall",
                severity = AlertSeverity.CRITICAL,
                message = "First aid required for elderly citizen.",
                timestamp = "1 hour ago",
                status = "Resolved",
                hasAudioAttachment = false
            )
        )
    )
    val reports: StateFlow<List<EmergencyReport>> = _reports.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "MSG-101",
                sender = "Control Room",
                senderRole = UserRole.GOVERNMENT,
                receiver = "Bhargav Ram",
                content = "Proceed to Sector 7 evacuation shelter for food packets and medical aid.",
                timestamp = "4:32 PM",
                isEmergency = false,
                isSentByMe = false,
                isVerified = true
            ),
            ChatMessage(
                id = "MSG-102",
                sender = "Bhargav Ram",
                senderRole = UserRole.CITIZEN,
                receiver = "Rescue Team 1",
                content = "Need immediate medical assistance at Sector 4.",
                timestamp = "4:35 PM",
                isEmergency = true,
                isSentByMe = true,
                isVerified = true
            ),
            ChatMessage(
                id = "MSG-103",
                sender = "Field Officer 07",
                senderRole = UserRole.FIELD_OFFICER,
                receiver = "All Nearby Units",
                content = "Local shelter cleared and equipped with emergency generators.",
                timestamp = "4:40 PM",
                isEmergency = false,
                isSentByMe = false,
                isVerified = true
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _nearbyDevices = MutableStateFlow<List<NearbyDevice>>(
        listOf(
            NearbyDevice("DEV-01", "Rescue Team 01", "Field Emergency Node", "120 m", "95% (Strong)", connected = true),
            NearbyDevice("DEV-02", "Field Officer 07", "Authorized Officer", "280 m", "82% (Good)", connected = true),
            NearbyDevice("DEV-03", "Medical Unit 03", "Mobile Ambulance", "450 m", "68% (Moderate)", connected = false),
            NearbyDevice("DEV-04", "Citizen Peer Node 14", "Mesh Relay", "80 m", "98% (Strong)", connected = true),
            NearbyDevice("DEV-05", "Civil Defense Hub", "Fixed Base Station", "850 m", "45% (Weak)", connected = false)
        )
    )
    val nearbyDevices: StateFlow<List<NearbyDevice>> = _nearbyDevices.asStateFlow()

    private val _meshStatus = MutableStateFlow(
        MeshStatus(
            status = "Connected & Active",
            nearbyPeers = 12,
            messagesRelayed = 38,
            lastAlertId = "AP-FLOOD-1029",
            signalQuality = "Good (92%)"
        )
    )
    val meshStatus: StateFlow<MeshStatus> = _meshStatus.asStateFlow()

    fun loginUser(role: UserRole, mobile: String) {
        val name = when (role) {
            UserRole.CITIZEN -> "Bhargav Ram"
            UserRole.FIELD_OFFICER -> "Inspector S. V. Rao"
            UserRole.GOVERNMENT -> "Dr. K. Srinivas (DDMA)"
        }
        _currentUser.value = _currentUser.value.copy(
            name = name,
            role = role,
            verified = true
        )
    }

    fun registerUser(name: String, mobile: String, govtId: String, state: String, district: String, language: String) {
        _currentUser.value = User(
            userId = "USR-AP-${(1000..9999).random()}",
            name = if (name.isNotBlank()) name else "Bhargav Ram",
            role = UserRole.CITIZEN,
            verified = true,
            preferredLanguage = language,
            district = if (district.isNotBlank()) district else "Vijayawada East",
            state = if (state.isNotBlank()) state else "Andhra Pradesh"
        )
    }

    fun addAlert(alert: AlertMessage) {
        _alerts.value = listOf(alert) + _alerts.value
    }

    fun addEmergencyReport(report: EmergencyReport) {
        _reports.value = listOf(report) + _reports.value
    }

    fun addChatMessage(msg: ChatMessage) {
        _messages.value = listOf(msg) + _messages.value
    }

    fun toggleDeviceConnection(deviceId: String) {
        _nearbyDevices.value = _nearbyDevices.value.map { dev ->
            if (dev.deviceId == deviceId) dev.copy(connected = !dev.connected) else dev
        }
    }

    fun updateLanguage(newLang: String) {
        _currentUser.value = _currentUser.value.copy(preferredLanguage = newLang)
    }
}
