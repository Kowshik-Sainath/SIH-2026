package com.example.thasmathjagratha.service

import com.example.thasmathjagratha.transport.WireAlert
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReceiverListeningState(val label: String) {
    IDLE_LISTENING("Listening..."),
    RECEIVING("Receiving..."),
    SPEAKING("Speaking")
}

object ReceiverStateHolder {
    private val _status = MutableStateFlow(ReceiverListeningState.IDLE_LISTENING)
    val status: StateFlow<ReceiverListeningState> = _status.asStateFlow()

    private val _currentSpeakingLang = MutableStateFlow<String?>(null)
    val currentSpeakingLang: StateFlow<String?> = _currentSpeakingLang.asStateFlow()

    private val _lastReceivedAlert = MutableStateFlow<WireAlert?>(null)
    val lastReceivedAlert: StateFlow<WireAlert?> = _lastReceivedAlert.asStateFlow()

    private val _incomingAlerts = MutableSharedFlow<WireAlert>(extraBufferCapacity = 64)
    val incomingAlerts: SharedFlow<WireAlert> = _incomingAlerts.asSharedFlow()

    fun updateStatus(newStatus: ReceiverListeningState, lang: String? = null) {
        _status.value = newStatus
        _currentSpeakingLang.value = lang
    }

    fun emitReceivedAlert(alert: WireAlert) {
        _lastReceivedAlert.value = alert
        _incomingAlerts.tryEmit(alert)
    }
}
