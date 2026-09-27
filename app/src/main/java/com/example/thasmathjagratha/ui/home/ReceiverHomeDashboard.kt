package com.example.thasmathjagratha.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.AlertCard
import com.example.thasmathjagratha.components.MeshStatusCard
import com.example.thasmathjagratha.components.ReceiverStatusCard
import com.example.thasmathjagratha.model.ReceivedAlert
import com.example.thasmathjagratha.model.ReceiverProfile
import com.example.thasmathjagratha.repository.SimulatedEventType
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertInfo
import com.example.thasmathjagratha.theme.AlertWarning
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun ReceiverHomeDashboard(
    viewModel: MainViewModel,
    onNavigateToAlerts: () -> Unit,
    onNavigateToReceiverMain: () -> Unit,
    onNavigateToMesh: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val meshStatus by viewModel.meshStatus.collectAsState()

    val unreadCount = alerts.count { !it.acknowledged && !it.isExpired }
    val latestAlert = alerts.firstOrNull { !it.isExpired } ?: alerts.firstOrNull()

    val profile = ReceiverProfile(
        name = user.name,
        preferredLanguage = user.preferredLanguage,
        district = user.district,
        state = user.state
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Status Top Card Banner
        ReceiverStatusCard(
            profile = profile,
            isSimulating = false,
            onSimulateClick = { viewModel.triggerSimulatedEvent(SimulatedEventType.CRITICAL_GOVT_ALERT) }
        )

        Column(modifier = Modifier.padding(16.dp)) {

            // Receiver Quick Summary Strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryMetric(title = "Unread Alerts", value = "$unreadCount", color = AlertCritical)
                    SummaryMetric(title = "Peers Connected", value = "${meshStatus.nearbyPeers}", color = NavyPrimary)
                    SummaryMetric(title = "Duplicates Blocked", value = "${meshStatus.duplicatesBlocked}", color = Color(0xFFED6C02))
                    SummaryMetric(title = "Last Sync", value = "Just now", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dashboard Grid Cards
            Text(text = "Receiver Modules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ReceiverGridCard(
                    title = "Receiver Hub",
                    subtitle = "Main alert monitor",
                    icon = Icons.Default.Radio,
                    iconColor = AlertCritical,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToReceiverMain
                )
                Spacer(modifier = Modifier.width(10.dp))
                ReceiverGridCard(
                    title = "All Alerts ($unreadCount new)",
                    subtitle = "Received broadcasts",
                    icon = Icons.Default.NotificationsActive,
                    iconColor = AlertWarning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAlerts
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ReceiverGridCard(
                    title = "Mesh Network",
                    subtitle = "${meshStatus.nearbyPeers} peers connected",
                    icon = Icons.Default.CellTower,
                    iconColor = AlertInfo,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMesh
                )
                Spacer(modifier = Modifier.width(10.dp))
                ReceiverGridCard(
                    title = "Language",
                    subtitle = profile.preferredLanguage,
                    icon = Icons.Default.Language,
                    iconColor = NavyPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLanguage
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ReceiverGridCard(
                    title = "Alert History",
                    subtitle = "${alerts.size} records saved",
                    icon = Icons.Default.History,
                    iconColor = NavyPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
                Spacer(modifier = Modifier.width(10.dp))
                ReceiverGridCard(
                    title = "Receiver Settings",
                    subtitle = "Preferences & TTS",
                    icon = Icons.Default.Settings,
                    iconColor = Color.Gray,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mesh Relay Card
            MeshStatusCard(status = meshStatus, onClick = onNavigateToMesh)

            Spacer(modifier = Modifier.height(20.dp))

            // Latest Active Alert Preview
            Text(text = "Latest Critical Broadcast", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            if (latestAlert != null) {
                AlertCard(
                    alert = ReceivedAlert(
                        alertId = latestAlert.alertId,
                        alertType = latestAlert.alertType,
                        authority = latestAlert.authority,
                        message = latestAlert.message,
                        translatedMessage = latestAlert.translatedMessage,
                        originalLanguage = latestAlert.originalLanguage,
                        preferredLanguage = latestAlert.preferredLanguage,
                        severity = latestAlert.severity,
                        location = latestAlert.targetArea,
                        issuedAt = latestAlert.issuedAt,
                        expiryTime = latestAlert.expiryTime,
                        verified = latestAlert.verified,
                        acknowledged = latestAlert.acknowledged,
                        expired = latestAlert.isExpired,
                        hopCount = latestAlert.hopCount,
                        maxHops = latestAlert.maxHops
                    ),
                    onClick = { onNavigateToAlertDetail(latestAlert.alertId) }
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ReceiverGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
