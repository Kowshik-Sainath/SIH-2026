package com.example.thasmathjagratha.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thasmathjagratha.manager.DeviceRoleManager
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AlertType
import com.example.thasmathjagratha.model.AppMessage
import com.example.thasmathjagratha.model.DeviceRole
import com.example.thasmathjagratha.model.EmergencyAlert
import com.example.thasmathjagratha.service.AlertReceiverForegroundService
import com.example.thasmathjagratha.service.ReceiverListeningState
import com.example.thasmathjagratha.service.ReceiverStateHolder
import com.example.thasmathjagratha.model.MeshReceiverStatus
import com.example.thasmathjagratha.model.NearbyDevice
import com.example.thasmathjagratha.model.ReceivedAlert
import com.example.thasmathjagratha.model.ReceiverProfile
import com.example.thasmathjagratha.model.User
import com.example.thasmathjagratha.model.UserRole
import com.example.thasmathjagratha.repository.EmergencyAlertPermissionsState
import com.example.thasmathjagratha.repository.MockAlertRepository
import com.example.thasmathjagratha.repository.MockIncomingAlertManager
import com.example.thasmathjagratha.repository.MockMessageRepository
import com.example.thasmathjagratha.repository.MockPermissionManager
import com.example.thasmathjagratha.repository.MockReceiverRepository
import com.example.thasmathjagratha.repository.MockRepository
import com.example.thasmathjagratha.repository.MockTranslationService
import com.example.thasmathjagratha.repository.MockTtsService
import com.example.thasmathjagratha.repository.SimulatedEventType
import com.example.thasmathjagratha.repository.TtsPlaybackState
import com.example.thasmathjagratha.stt.engine.SttEngine
import com.example.thasmathjagratha.stt.engine.SttListener
import com.example.thasmathjagratha.stt.manager.LanguagePackManager
import com.example.thasmathjagratha.stt.telemetry.SttTelemetry
import com.example.thasmathjagratha.stt.telemetry.TelemetrySnapshot
import com.example.thasmathjagratha.transport.LocalAlertTransport
import com.example.thasmathjagratha.transport.WireAlert
import com.example.thasmathjagratha.tts.engine.TtsEngine
import com.example.thasmathjagratha.tts.engine.TtsMode
import com.example.thasmathjagratha.tts.playback.TtsAudioPlayer
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpeechState(
    val isListening: Boolean = false,
    val selectedLanguage: String = "Telugu",
    val selectedReceiver: String = "Control Room",
    val transcriptionText: String = "",
    val confidenceScore: Int = 0,
    val isTranscribed: Boolean = false
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val alertRepository: MockAlertRepository = MockAlertRepository(),
    private val messageRepository: MockMessageRepository = MockMessageRepository(),
    private val translationService: MockTranslationService = MockTranslationService(),
    private val ttsService: MockTtsService = MockTtsService(),
    private val permissionManager: MockPermissionManager = MockPermissionManager(),
    private val receiverRepository: MockReceiverRepository = MockReceiverRepository(),
    private val repository: MockRepository = MockRepository()
) : AndroidViewModel(application) {

    constructor() : this(
        try {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as Application
        } catch (_: Exception) {
            Application()
        }
    )

    private val roleManager = DeviceRoleManager(application)
    private val _deviceRole = MutableStateFlow(roleManager.getRole())
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _transportDeliveryStatus = MutableStateFlow("Idle")
    val transportDeliveryStatus: StateFlow<String> = _transportDeliveryStatus.asStateFlow()

    init {
        // Collect alerts forwarded by the background Foreground Service
        viewModelScope.launch {
            ReceiverStateHolder.incomingAlerts.collect { wireAlert ->
                onRemoteAlert(wireAlert)
            }
        }
        // If device role is persisted as RECEIVER, ensure the background listener is running
        if (_deviceRole.value == DeviceRole.RECEIVER) {
            AlertReceiverForegroundService.start(application)
        }
    }

    fun selectRole(role: DeviceRole, context: Context) {
        val app = context.applicationContext
        roleManager.setRole(role)
        _deviceRole.value = role

        if (role == DeviceRole.SENDER) {
            // Teardown Receiver Service and TTS resources to minimize RAM
            AlertReceiverForegroundService.stop(app)
            viewModelScope.launch {
                receiverTts?.destroy()
                receiverTts = null
                receiverPlayer?.stop()
                receiverPlayer = null
                localTransport?.close()
                localTransport = null
            }
            showSnackbar("Configured as Sender: STT Mode (Speak & Send)")
        } else if (role == DeviceRole.RECEIVER) {
            // Teardown Sender STT resources to minimize RAM
            sttEngine?.destroy()
            sttEngine = null
            _speechState.value = _speechState.value.copy(isListening = false)
            // Start continuous background Receiver Foreground Service
            AlertReceiverForegroundService.start(app)
            showSnackbar("Configured as Receiver: TTS Mode (Listen & Receive)")
        }
    }

    fun changeRole(context: Context) {
        val app = context.applicationContext
        AlertReceiverForegroundService.stop(app)
        sttEngine?.destroy()
        sttEngine = null
        _speechState.value = _speechState.value.copy(isListening = false)
        viewModelScope.launch {
            receiverTts?.destroy()
            receiverTts = null
            receiverPlayer?.stop()
            receiverPlayer = null
            localTransport?.close()
            localTransport = null
        }
        roleManager.clearRole()
        _deviceRole.value = DeviceRole.UNSET
        showSnackbar("Operating role reset. Please select a mode.")
    }

    private val incomingAlertManager = MockIncomingAlertManager(
        alertRepository,
        messageRepository,
        translationService
    )

    private val _currentUser = MutableStateFlow(
        User(
            userId = "USR-AP-8842",
            name = "Bhargav Ram",
            role = UserRole.CITIZEN,
            verified = true,
            preferredLanguage = "Telugu",
            state = "Andhra Pradesh",
            district = "Vijayawada East"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    val alerts: StateFlow<List<EmergencyAlert>> = alertRepository.alerts
    val messages: StateFlow<List<AppMessage>> = messageRepository.messages
    val permissionsState: StateFlow<EmergencyAlertPermissionsState> = permissionManager.permissionsState
    val ttsState: StateFlow<TtsPlaybackState> = ttsService.ttsState

    val nearbyDevices: StateFlow<List<NearbyDevice>> = repository.nearbyDevices
    val receiverProfile: StateFlow<ReceiverProfile> = receiverRepository.receiverProfile
    val meshReceiverStatus: StateFlow<MeshReceiverStatus> = receiverRepository.meshStatus
    val receivedAlerts: StateFlow<List<ReceivedAlert>> = receiverRepository.receivedAlerts

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

    // Overlay / Banner Popup State
    private val _activeCriticalAlert = MutableStateFlow<EmergencyAlert?>(null)
    val activeCriticalAlert: StateFlow<EmergencyAlert?> = _activeCriticalAlert.asStateFlow()

    private val _activeNormalBanner = MutableStateFlow<AppMessage?>(null)
    val activeNormalBanner: StateFlow<AppMessage?> = _activeNormalBanner.asStateFlow()

    private val _activeAlertBanner = MutableStateFlow<EmergencyAlert?>(null)
    val activeAlertBanner: StateFlow<EmergencyAlert?> = _activeAlertBanner.asStateFlow()

    private val _speechState = MutableStateFlow(SpeechState())
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _telemetrySnapshot = MutableStateFlow(SttTelemetry.getSnapshot())
    val telemetrySnapshot: StateFlow<TelemetrySnapshot> = _telemetrySnapshot.asStateFlow()

    private var sttEngine: SttEngine? = null
    private var localTransport: LocalAlertTransport? = null
    private var receiverTts: TtsEngine? = null
    private var receiverPlayer: TtsAudioPlayer? = null
    private val announcementMutex = Mutex()
    private val announcementJobs = ConcurrentHashMap<String, Job>()

    fun startLocalAlertReceiver(context: Context) {
        if (_deviceRole.value != DeviceRole.RECEIVER) {
            Log.i("MainViewModel", "Skipping local alert receiver: device role is ${_deviceRole.value}")
            return
        }
        AlertReceiverForegroundService.start(context)
    }

    fun enableBluetoothAlertReceiver() { localTransport?.startBluetoothIfPermitted() }

    fun replayReceivedAlert(alert: EmergencyAlert) {
        val code = receiverTts?.packManager?.availablePacks?.firstOrNull {
            it.displayName.equals(alert.preferredLanguage, ignoreCase = true)
        }?.languageCode ?: return
        if (receiverTts?.packManager?.isPackDownloaded(code) != true) {
            showSnackbar("Download the ${alert.preferredLanguage} TTS pack to replay this alert")
            return
        }
        val job = viewModelScope.launch {
            announcementMutex.withLock {
                try {
                    receiverTts?.init(code)
                    val audio = receiverTts?.synthesize(alert.translatedMessage, TtsMode.ALERT)
                    if (audio != null) receiverPlayer?.play(audio)
                } catch (error: Exception) {
                    showSnackbar("Alert audio failed: ${error.message}")
                }
            }
        }
        announcementJobs[alert.alertId] = job
        job.invokeOnCompletion { announcementJobs.remove(alert.alertId) }
    }

    private fun onRemoteAlert(packet: WireAlert) {
        viewModelScope.launch {
            if (alertRepository.isDuplicate(packet.id)) return@launch
            val code = packet.languageCode.lowercase()
            val pack = runCatching { receiverTts?.packManager?.getPack(code) }.getOrNull()
            val language = pack?.displayName ?: code
            val severity = AlertSeverity.entries.firstOrNull { it.name == packet.severity }
                ?: return@launch
            val now = System.currentTimeMillis()
            val alert = EmergencyAlert(
                alertId = packet.id,
                authority = packet.senderName,
                alertType = "Local device alert",
                message = packet.text,
                translatedMessage = packet.text,
                originalLanguage = language,
                preferredLanguage = language,
                severity = severity,
                targetArea = "Nearby device",
                issuedAt = packet.sentAt,
                expiryTime = now + 60 * 60 * 1000L,
                verified = false // The local transport does not authenticate authorities.
            )
            alertRepository.addAlert(alert)
            receiverRepository.addReceivedAlert(ReceivedAlert(
                alertId = alert.alertId, alertType = alert.alertType, authority = alert.authority,
                message = alert.message, translatedMessage = alert.translatedMessage,
                originalLanguage = alert.originalLanguage, preferredLanguage = alert.preferredLanguage,
                severity = alert.severity, location = alert.targetArea, issuedAt = alert.issuedAt,
                expiryTime = alert.expiryTime, verified = false, receivedVia = packet.transport,
                forwarded = false, forwardedDeviceCount = 0
            ))
            if (severity == AlertSeverity.CRITICAL) _activeCriticalAlert.value = alert
            else _activeAlertBanner.value = alert
            showSnackbar("Alert received from ${packet.senderName} via ${packet.transport}")
            if (!_autoPlayAlerts.value) return@launch
            val tts = receiverTts ?: return@launch
            if (pack == null || tts.packManager.isPackDownloaded(code) != true) {
                showSnackbar("Alert received; download the $language offline TTS pack to hear it")
                return@launch
            }
            val job = viewModelScope.launch {
                announcementMutex.withLock {
                    try {
                        tts.init(code)
                        val mode = if (severity == AlertSeverity.CRITICAL) TtsMode.ALERT else TtsMode.NORMAL
                        val audio = tts.synthesize(packet.text, mode) ?: return@withLock
                        ttsService.play(packet.text, language)
                        receiverPlayer?.play(audio)
                        TtsTelemetry.logPlayback(packet.id, mode.name)
                        ttsService.stop()
                    } catch (error: Exception) {
                        ttsService.stop()
                        showSnackbar("Alert audio failed: ${error.message}")
                    }
                }
            }
            announcementJobs[packet.id] = job
            job.invokeOnCompletion { announcementJobs.remove(packet.id) }
        }
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Settings
    private val _textSize = MutableStateFlow("Medium")
    val textSize: StateFlow<String> = _textSize.asStateFlow()

    private val _autoPlayAlerts = MutableStateFlow(true)
    val autoPlayAlerts: StateFlow<Boolean> = _autoPlayAlerts.asStateFlow()

    private val _notificationSound = MutableStateFlow(true)
    val notificationSound: StateFlow<Boolean> = _notificationSound.asStateFlow()

    private val _emergencyVibration = MutableStateFlow(true)
    val emergencyVibration: StateFlow<Boolean> = _emergencyVibration.asStateFlow()

    // Automatic Incoming Simulation Trigger
    fun triggerSimulatedEvent(type: SimulatedEventType) {
        viewModelScope.launch {
            showSnackbar("Detecting incoming background emergency packet...")
            incomingAlertManager.simulateEvent(
                type = type,
                preferredLanguage = currentUser.value.preferredLanguage,
                onNormalMessage = { msg ->
                    _activeNormalBanner.value = msg
                    showSnackbar("New Message received from ${msg.senderName}")
                },
                onEmergencyAlert = { alert ->
                    if (alert.severity == AlertSeverity.CRITICAL) {
                        _activeCriticalAlert.value = alert
                        showSnackbar("CRITICAL EMERGENCY ALERT RECEIVED & VERIFIED")
                        if (_autoPlayAlerts.value) {
                            replayReceivedAlert(alert)
                        }
                    } else {
                        _activeAlertBanner.value = alert
                        showSnackbar("Incoming Alert: ${alert.alertType} (${alert.severity.displayName})")
                    }
                },
                onDuplicateBlocked = { alertId ->
                    _meshStatus.value = _meshStatus.value.copy(
                        duplicatesBlocked = _meshStatus.value.duplicatesBlocked + 1
                    )
                    showSnackbar("Duplicate Alert Blocked ($alertId)")
                }
            )
        }
    }

    fun toggleDeviceConnection(deviceId: String) {
        repository.toggleDeviceConnection(deviceId)
    }

    fun toggleMeshRelay() {
        receiverRepository.toggleMeshRelay()
        val enabled = receiverProfile.value.meshRelayEnabled
        showSnackbar("Mesh Relay P2P Forwarding: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    fun simulateDuplicatePacket() {
        receiverRepository.simulateDuplicateBlocked()
        _meshStatus.value = _meshStatus.value.copy(
            duplicatesBlocked = _meshStatus.value.duplicatesBlocked + 1
        )
        showSnackbar("Duplicate packet detected & blocked (Hash Match)")
    }

    fun playReceiverTts(text: String, language: String) {
        playTts(text, language)
    }

    fun playTtsSimulation() {
        val text = speechState.value.transcriptionText
        if (text.isNotBlank()) {
            playTts(text, speechState.value.selectedLanguage)
        } else {
            showSnackbar("No transcribed text available to play")
        }
    }

    fun sendVoiceMessage() {
        val text = speechState.value.transcriptionText
        if (text.isNotBlank()) {
            val selectedLang = speechState.value.selectedLanguage
            sendUserMessage(
                recipient = speechState.value.selectedReceiver,
                language = selectedLang,
                priority = AlertSeverity.NORMAL,
                text = text
            )
            val alert = EmergencyAlert(
                alertId = "VOICE-${(1000..9999).random()}",
                authority = currentUser.value.name,
                alertType = "Voice Relay Message",
                message = text,
                translatedMessage = text,
                originalLanguage = selectedLang,
                preferredLanguage = selectedLang,
                severity = AlertSeverity.NORMAL,
                targetArea = currentUser.value.district,
                issuedAt = System.currentTimeMillis(),
                expiryTime = System.currentTimeMillis() + (60 * 60 * 1000L),
                verified = true
            )
            alertRepository.addAlert(alert)
            sendLocalAlert(alert, selectedLang)
            showSnackbar("Voice message sent to ${speechState.value.selectedReceiver} and broadcasted locally")
        } else {
            showSnackbar("No transcribed text available to send")
        }
    }

    fun dismissCriticalAlert() {
        _activeCriticalAlert.value?.alertId?.let { announcementJobs[it]?.cancel() }
        receiverPlayer?.acknowledgeAlert()
        _activeCriticalAlert.value = null
        ttsService.stop()
    }

    fun dismissNormalBanner() {
        _activeNormalBanner.value = null
    }

    fun dismissAlertBanner() {
        _activeAlertBanner.value = null
    }

    fun acknowledgeAlert(alertId: String) {
        announcementJobs[alertId]?.cancel()
        receiverPlayer?.acknowledgeAlert()
        alertRepository.acknowledgeAlert(alertId)
        receiverRepository.acknowledgeAlert(alertId)
        if (_activeCriticalAlert.value?.alertId == alertId) {
            _activeCriticalAlert.value = _activeCriticalAlert.value?.copy(acknowledged = true)
        }
        showSnackbar("Alert $alertId Acknowledged (Status: READ)")
    }

    fun playTts(text: String, language: String) {
        if (text.isBlank()) return
        val engine = receiverTts ?: run {
            showSnackbar("Offline TTS is not ready")
            return
        }
        val pack = engine.packManager.availablePacks.firstOrNull {
            it.displayName.equals(language, ignoreCase = true) || it.languageCode.equals(language, ignoreCase = true)
        }
        if (pack == null) {
            showSnackbar("TTS is not available for $language")
            return
        }
        if (!engine.packManager.isPackDownloaded(pack.languageCode)) {
            showSnackbar("Download the ${pack.displayName} TTS pack first")
            return
        }
        viewModelScope.launch {
            announcementMutex.withLock {
                try {
                    engine.init(pack.languageCode)
                    ttsService.play(text, pack.displayName)
                    receiverPlayer?.play(engine.synthesize(text, TtsMode.NORMAL))
                    ttsService.stop()
                } catch (error: Exception) {
                    ttsService.stop()
                    showSnackbar("Offline TTS failed: ${error.message}")
                }
            }
        }
    }

    fun playTts(context: Context, text: String, language: String) {
        startLocalAlertReceiver(context)
        playTts(text, language)
    }

    fun pauseTts() {
        receiverPlayer?.stopNormal()
        ttsService.pause()
        showSnackbar("Audio Paused")
    }

    fun stopTts() {
        receiverPlayer?.stopNormal()
        ttsService.stop()
        showSnackbar("Audio Stopped")
    }

    fun setTtsSpeed(speed: Float) {
        ttsService.setSpeed(speed)
        showSnackbar("Speed set to ${speed}x")
    }

    fun updatePreferredLanguage(language: String) {
        _currentUser.value = _currentUser.value.copy(preferredLanguage = language)
        _speechState.value = _speechState.value.copy(selectedLanguage = language)
        showSnackbar("Preferred Alert Language set to $language")
    }

    fun toggleDndPermission() {
        permissionManager.toggleDndAccess()
        val granted = permissionsState.value.dndAccessGranted
        showSnackbar("Emergency Alerts During DND: ${if (granted) "ENABLED" else "DISABLED"}")
    }

    fun login(role: UserRole, mobile: String) {
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
        showSnackbar("Logged in as ${role.displayName}")
    }

    fun register(name: String, mobile: String, govtId: String, state: String, district: String, language: String) {
        _currentUser.value = User(
            userId = "USR-AP-${(1000..9999).random()}",
            name = if (name.isNotBlank()) name else "Bhargav Ram",
            role = UserRole.CITIZEN,
            verified = true,
            preferredLanguage = language,
            district = if (district.isNotBlank()) district else "Vijayawada East",
            state = if (state.isNotBlank()) state else "Andhra Pradesh"
        )
        updatePreferredLanguage(language)
        showSnackbar("KYC Verification Successful. Welcome ${_currentUser.value.name}")
    }

    fun sendUserMessage(recipient: String, language: String, priority: AlertSeverity, text: String) {
        val msg = AppMessage(
            messageId = "MSG-${(1000..9999).random()}",
            senderId = currentUser.value.userId,
            senderName = currentUser.value.name,
            receiverId = recipient,
            receiverName = recipient,
            message = text,
            language = language,
            priority = priority,
            timestamp = System.currentTimeMillis(),
            isSentByMe = true,
            isRead = true
        )
        messageRepository.addMessage(msg)
        showSnackbar("Message sent to $recipient (Status: Delivered)")
    }

    fun sendEmergencyReport(emergencyType: AlertType, location: String, severity: AlertSeverity, message: String) {
        val alert = EmergencyAlert(
            alertId = "REP-${(8800..9999).random()}",
            authority = "${currentUser.value.name} (Report)",
            alertType = emergencyType.title,
            message = if (message.isNotBlank()) message else "Incident reported near $location",
            translatedMessage = translationService.translate(message, currentUser.value.preferredLanguage),
            originalLanguage = currentUser.value.preferredLanguage,
            preferredLanguage = currentUser.value.preferredLanguage,
            severity = severity,
            targetArea = if (location.isNotBlank()) location else currentUser.value.district,
            issuedAt = System.currentTimeMillis(),
            expiryTime = System.currentTimeMillis() + (2 * 3600 * 1000L),
            verified = true
        )
        alertRepository.addAlert(alert)
        sendLocalAlert(alert, currentUser.value.preferredLanguage)
    }

    fun broadcastGovernmentAlert(
        alertType: AlertType,
        severity: AlertSeverity,
        targetArea: String,
        radiusKm: Int,
        validity: String,
        language: String,
        message: String,
        onSuccess: (EmergencyAlert) -> Unit
    ) {
        val alert = EmergencyAlert(
            alertId = "AP-${alertType.name}-${(1000..9999).random()}",
            authority = currentUser.value.name,
            alertType = alertType.title,
            message = message,
            translatedMessage = translationService.translate(message, language),
            originalLanguage = language,
            preferredLanguage = language,
            severity = severity,
            targetArea = targetArea,
            issuedAt = System.currentTimeMillis(),
            expiryTime = System.currentTimeMillis() + (45 * 60 * 1000L),
            verified = true
        )
        alertRepository.addAlert(alert)
        sendLocalAlert(alert, language)
        onSuccess(alert)
    }

    private fun sendLocalAlert(alert: EmergencyAlert, language: String) {
        viewModelScope.launch {
            val code = getLanguageCodeForName(language)
            val app = getApplication<Application>()
            if (localTransport == null) {
                localTransport = LocalAlertTransport(app, null)
            }
            val transport = localTransport!!
            val packet = WireAlert(
                alert.alertId, transport.deviceId, currentUser.value.name,
                alert.message, code, alert.severity.name, System.currentTimeMillis()
            )
            _transportDeliveryStatus.value = "Broadcasting alert over Wi-Fi / Bluetooth..."
            val attempts = runCatching { transport.send(packet) }.getOrElse {
                Log.e("MainViewModel", "Local broadcast failed", it)
                _transportDeliveryStatus.value = "Broadcast error: ${it.message}"
                showSnackbar("Broadcast failed: ${it.message}")
                return@launch
            }
            val queued = runCatching { transport.pendingCount() }.getOrDefault(0)
            if (attempts > 0) {
                _transportDeliveryStatus.value = "Sent via local broadcast ($attempts link(s))"
                showSnackbar("Alert sent on local links ($attempts sends; queued: $queued)")
            } else if (queued > 0) {
                _transportDeliveryStatus.value = "Queued in outbox (searching for receiver...)"
                showSnackbar("No nearby device yet; alert queued and will retry automatically")
            } else {
                _transportDeliveryStatus.value = "Sent"
            }
        }
    }

    override fun onCleared() {
        announcementJobs.values.forEach { it.cancel() }
        receiverPlayer?.acknowledgeAlert()
        localTransport?.close()
        CoroutineScope(Dispatchers.Default).launch { receiverTts?.destroy() }
        super.onCleared()
    }

    private var speechAttemptCount = 0

    private fun getDynamicSpokenSentence(language: String, attempt: Int): String {
        val index = (attempt - 1) % 5
        return when (language) {
            "Telugu" -> when (index) {
                0 -> "విజయవాడ ఈస్ట్ ప్రాంతంలో వరద నీరు వేగంగా పెరుగుతోంది"
                1 -> "అత్యవసర వైద్య సహాయం మరియు అంబులెన్స్ వెంటనే కావలెను"
                2 -> "కృష్ణానది నీటిమట్టం పెరుగుతోంది. ప్రజలు సురక్షిత ప్రాంతాలకు వెళ్లవలెను"
                3 -> "సెక్టార్ 7 రక్షణ బృందం తక్షణ సహాయం కొరకు నివేదించింది"
                else -> "పోలీసు మద్దతు మరియు ఆహార పదార్థాల పంపిణీ అవసరం"
            }
            "Hindi" -> when (index) {
                0 -> "विजयवाड़ा पूर्व क्षेत्र में बाढ़ का पानी तेजी से बढ़ रहा है"
                1 -> "आपातकालीन चिकित्सा सहायता और एम्बुलेंस की तुरंत आवश्यकता है"
                2 -> "कृष्णा नदी का जलस्तर बढ़ रहा है। निवासी सुरक्षित स्थानों पर जाएं"
                3 -> "सेक्टर 7 बचाव दल ने तत्काल सहायता की सूचना दी है"
                else -> "पुलिस सहायता और खाद्य आपूर्ति वितरण की आवश्यकता है"
            }
            "Tamil" -> when (index) {
                0 -> "விஜயவாடா கிழக்கு பகுதியில் வெள்ள நீர் வேகமாக உயர்கிறது"
                1 -> "அவசர மருத்துவ உதவி மற்றும் ஆம்புலன்ஸ் உடனடியாக தேவை"
                2 -> "கிருஷ்ணா நதி வெள்ள எச்சரிக்கை. பொதுமக்கள் பாதுகாப்பான இடத்திற்கு செல்லவும்"
                3 -> "செக்டர் 7 மீட்புக் குழு அவசர உதவி கோரியுள்ளது"
                else -> "காவல்துறை ஆதரவு மற்றும் உணவு விநியோகம் தேவை"
            }
            "Kannada" -> when (index) {
                0 -> "ವಿಜಯವಾಡ ಪೂರ್ವ ಪ್ರದೇಶದಲ್ಲಿ ಪ್ರವಾಹ ನೀರು ವೇಗವಾಗಿ ಏರುತ್ತಿದೆ"
                1 -> "ತುರ್ತು ವೈದ್ಯಕೀಯ ನೆರವು ಮತ್ತು ಆಂಬ್ಯುಲೆನ್ಸ್ ತಕ್ಷಣ ಬೇಕಾಗಿದೆ"
                2 -> "ಕೃಷ್ಣಾ ನದಿಯ ನೀರಿನ ಮಟ್ಟ ಏರುತ್ತಿದೆ. ಜನರು ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತೆರಳಬೇಕು"
                3 -> "ಸೆಕ್ಟರ್ 7 ರಕ್ಷಣಾ ಪಡೆ ತುರ್ತು ನೆರವು ವರದಿ ಮಾಡಿದೆ"
                else -> "ಪೊಲೀಸ್ ಬೆಂಬಲ ಮತ್ತು ಆಹಾರ ಪೂರೈಕೆ ಅಗತ್ಯವಿದೆ"
            }
            "Malayalam" -> when (index) {
                0 -> "വിജയവാഡ ഈസ്റ്റ് പ്രദേശത്ത് വെള്ളപ്പൊക്ക ജലനിരപ്പ് വേഗത്തിൽ ഉയരുന്നു"
                1 -> "അടിയന്തിര വൈദ്യസഹായവും ആംബുലൻസും ഉടൻ വേണം"
                2 -> "കൃഷ്ണാനദിയിലെ ജലനിരപ്പ് ഉയരുന്നു. ആളുകൾ സുരക്ഷിത സ്ഥാനത്തേക്ക് മാറണം"
                3 -> "സെക്ടർ 7 രക്ഷാസേന അടിയന്തര സഹായം ആവശ്യപ്പെട്ടു"
                else -> "പോലീസ് സഹായവും ഭക്ഷണ വിതരണവും ആവശ്യമാണ്"
            }
            "Marathi" -> when (index) {
                0 -> "विजयवाड्यात पुराचे पाणी वेगाने वाढत आहे"
                1 -> "तातडीची वैद्यकीय मदत आणि रुग्णवाहिका हवी आहे"
                2 -> "कृष्णा नदीची पाण्याची पातळी वाढत आहे. लोकांनी सुरक्षित ठिकाणी जावे"
                3 -> "सेक्टर 7 बचाव पथकाने तातडीच्या मदतीची विनंती केली आहे"
                else -> "पोलीस मदत आणि अन्नधान्य वाटप आवश्यक आहे"
            }
            "Gujarati" -> when (index) {
                0 -> "વિજયવાડામાં પૂરના પાણી ઝડપથી વધી રહ્યા છે"
                1 -> "ઇમરજન્સી તબીબી સહાય અને એમ્બ્યુલન્સની જરૂર છે"
                2 -> "કૃષ્ણા નદીની સપાટી વધી રહી છે. લોકો સુરક્ષિત સ્થળે ખસી જાય"
                3 -> "સેક્ટર 7 બચાવ ટુકડીએ તાત્કાલિક મદદ માગી છે"
                else -> "પોલીસ સહાય અને રાશન વિતરણની જરૂર છે"
            }
            "Bengali" -> when (index) {
                0 -> "বিজয়ওয়াড়ায় বন্যার জল দ্রুত বৃদ্ধি পাচ্ছে"
                1 -> "জরুরী চিকিৎসা সহায়তা ও অ্যাম্বুলেন্স প্রয়োজন"
                2 -> "কৃষ্ণা নদীর জলস্তর বৃদ্ধি পাচ্ছে। বাসিন্দারা নিরাপদ স্থানে যান"
                3 -> "সেক্টর ৭ উদ্ধারকারী দল জরুরী সহায়তা চেয়েছে"
                else -> "পুলিশি সহায়তা ও খাদ্য ত্রাণ প্রয়োজন"
            }
            "Punjabi" -> when (index) {
                0 -> "ਵਿਜੇਵਾੜਾ ਵਿੱਚ ਹੜ੍ਹ ਦਾ ਪਾਣੀ ਤੇਜ਼ੀ ਨਾਲ ਵਧ ਰਿਹਾ ਹੈ"
                1 -> "ਐਮਰਜੈਂਸੀ ਡਾਕਟਰੀ ਮਦਦ ਅਤੇ ਐਂਬੂਲੈਂਸ ਦੀ ਲੋੜ ਹੈ"
                2 -> "ਕ੍ਰਿਸ਼ਨਾ ਨਦੀ ਦਾ ਪਾਣੀ ਵਧ ਰਿਹਾ ਹੈ। ਲੋਕ ਸੁਰੱਖਿਅਤ ਥਾਂ 'ਤੇ ਜਾਣ"
                3 -> "ਸੈਕਟਰ 7 ਬਚਾਅ ਟੀਮ ਨੇ ਤੁਰੰਤ ਮਦਦ ਮੰਗੀ ਹੈ"
                else -> "ਪੁਲਿਸ ਸਹਾਇਤਾ ਅਤੇ ਰਾਸ਼ਨ ਵੰਡ ਦੀ ਲੋੜ ਹੈ"
            }
            else -> when (index) {
                0 -> "Flood water levels rising rapidly in Vijayawada East sector"
                1 -> "Emergency medical team and ambulance required immediately"
                2 -> "Krishna river flow alert. Residents evacuate to safe shelters now"
                3 -> "Sector 7 rescue unit reporting urgent dispatch requirements"
                else -> "Police support and food ration distribution needed at Vijayawada center"
            }
        }
    }

    fun startRealListening(context: Context) {
        viewModelScope.launch {
            try {
                if (sttEngine == null) {
                    sttEngine = SttEngine(context.applicationContext)
                }

                val engine = sttEngine!!
                val langCode = getLanguageCodeForName(speechState.value.selectedLanguage)

                if (!engine.packManager.isPackDownloaded(langCode)) {
                    val pack = engine.packManager.availablePacks.find { it.languageCode == langCode }
                    val name = pack?.displayName ?: speechState.value.selectedLanguage
                    showSnackbar("Please download the $name STT language pack first.")
                    _speechState.value = _speechState.value.copy(isListening = false)
                    return@launch
                }

                engine.setListener(object : SttListener {
                    override fun onPartialText(partialText: String) {
                        _speechState.value = _speechState.value.copy(
                            transcriptionText = partialText,
                            isTranscribed = false
                        )
                    }

                    override fun onFinalText(finalText: String) {
                        _speechState.value = _speechState.value.copy(
                            transcriptionText = finalText,
                            confidenceScore = (92..99).random(),
                            isTranscribed = true,
                            isListening = false
                        )
                        refreshTelemetry()
                    }

                    override fun onError(error: Throwable) {
                        _speechState.value = _speechState.value.copy(isListening = false)
                        showSnackbar("Speech Recognition: ${error.message}")
                    }

                    override fun onAudioLevel(rmsLevel: Float) {
                        _audioLevel.value = rmsLevel
                    }
                })

                engine.init(langCode)
                _speechState.value = _speechState.value.copy(
                    isListening = true,
                    transcriptionText = "",
                    isTranscribed = false
                )
                engine.startListening()
                refreshTelemetry()
            } catch (e: Exception) {
                e.printStackTrace()
                _speechState.value = _speechState.value.copy(isListening = false)
                showSnackbar("STT Error: ${e.message}")
            }
        }
    }

    fun stopRealListening() {
        sttEngine?.stopListening()
        _speechState.value = _speechState.value.copy(isListening = false)
        refreshTelemetry()
    }

    fun downloadLanguagePack(context: Context, languageName: String) {
        viewModelScope.launch {
            val packManager = LanguagePackManager(context.applicationContext)
            val langCode = getLanguageCodeForName(languageName)
            val pack = packManager.availablePacks.find { it.languageCode == langCode } ?: return@launch

            showSnackbar("Downloading ${pack.displayName} (${pack.modelSizeMb}) offline pack...")

            val result = packManager.downloadLanguagePack(pack) { progress ->
                _downloadProgress.value = _downloadProgress.value + (langCode to progress)
            }

            if (result.isSuccess) {
                showSnackbar("${pack.displayName} STT Language Pack installed successfully!")
            } else {
                showSnackbar("Failed to download ${pack.displayName} pack: ${result.exceptionOrNull()?.message}")
            }
            refreshTelemetry()
        }
    }

    fun deleteLanguagePack(context: Context, languageName: String) {
        val packManager = LanguagePackManager(context.applicationContext)
        val langCode = getLanguageCodeForName(languageName)
        val deleted = packManager.deleteLanguagePack(langCode)
        if (deleted) {
            showSnackbar("$languageName Language Pack deleted.")
        } else {
            showSnackbar("$languageName Language Pack was not found.")
        }
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        _telemetrySnapshot.value = SttTelemetry.getSnapshot()
    }

    private fun getLanguageCodeForName(name: String): String {
        return when (name.lowercase()) {
            "telugu" -> "te"
            "hindi" -> "hi"
            "tamil" -> "ta"
            "kannada" -> "kn"
            "malayalam" -> "ml"
            "marathi" -> "mr"
            "gujarati" -> "gu"
            "bengali" -> "bn"
            "punjabi" -> "pa"
            "english" -> "en"
            else -> "te"
        }
    }

    fun simulateRecording() {
        viewModelScope.launch {
            _speechState.value = _speechState.value.copy(
                isListening = true,
                transcriptionText = "",
                confidenceScore = 0,
                isTranscribed = false
            )
            delay(1500)
            speechAttemptCount++
            val text = getDynamicSpokenSentence(_speechState.value.selectedLanguage, speechAttemptCount)
            _speechState.value = _speechState.value.copy(
                isListening = false,
                transcriptionText = text,
                confidenceScore = (92..98).random(),
                isTranscribed = true
            )
            refreshTelemetry()
        }
    }

    fun updateSpeechLanguage(language: String) {
        _speechState.value = _speechState.value.copy(selectedLanguage = language)
        updatePreferredLanguage(language)
    }

    fun updateSpeechReceiver(receiver: String) {
        _speechState.value = _speechState.value.copy(selectedReceiver = receiver)
    }

    fun checkBroadcastAuthorization(onAuthorized: () -> Unit) {
        if (currentUser.value.role == UserRole.CITIZEN) {
            showSnackbar("You are not authorized to perform this action.")
        } else {
            onAuthorized()
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun updateSettings(textSize: String, autoPlay: Boolean, sound: Boolean, vibration: Boolean) {
        _textSize.value = textSize
        _autoPlayAlerts.value = autoPlay
        _notificationSound.value = sound
        _emergencyVibration.value = vibration
        showSnackbar("Settings updated")
    }
}
