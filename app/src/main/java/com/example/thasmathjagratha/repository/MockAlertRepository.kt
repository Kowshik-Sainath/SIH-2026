package com.example.thasmathjagratha.repository

import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.EmergencyAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAlertRepository {

    private val now = System.currentTimeMillis()
    private val oneHour = 3600 * 1000L

    private val seenAlertIds = mutableSetOf<String>()

    private val _alerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val alerts: StateFlow<List<EmergencyAlert>> = _alerts.asStateFlow()

    fun isDuplicate(alertId: String): Boolean {
        return seenAlertIds.contains(alertId)
    }

    fun addAlert(alert: EmergencyAlert) {
        seenAlertIds.add(alert.alertId)
        _alerts.value = listOf(alert) + _alerts.value
    }

    fun acknowledgeAlert(alertId: String) {
        _alerts.value = _alerts.value.map { alert ->
            if (alert.alertId == alertId) alert.copy(acknowledged = true) else alert
        }
    }
}
