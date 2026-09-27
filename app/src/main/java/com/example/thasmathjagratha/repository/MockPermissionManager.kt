package com.example.thasmathjagratha.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EmergencyAlertPermissionsState(
    val notificationsGranted: Boolean = true,
    val highPriorityGranted: Boolean = true,
    val fullScreenAlertsGranted: Boolean = true,
    val dndAccessGranted: Boolean = false
)

class MockPermissionManager {

    private val _permissionsState = MutableStateFlow(EmergencyAlertPermissionsState())
    val permissionsState: StateFlow<EmergencyAlertPermissionsState> = _permissionsState.asStateFlow()

    fun toggleDndAccess() {
        val current = _permissionsState.value.dndAccessGranted
        _permissionsState.value = _permissionsState.value.copy(dndAccessGranted = !current)
    }

    fun grantAllPermissions() {
        _permissionsState.value = EmergencyAlertPermissionsState(
            notificationsGranted = true,
            highPriorityGranted = true,
            fullScreenAlertsGranted = true,
            dndAccessGranted = true
        )
    }
}
