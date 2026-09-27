package com.example.thasmathjagratha.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.SeverityChip
import com.example.thasmathjagratha.components.VerifiedBadge
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.EmergencyAlert
import com.example.thasmathjagratha.model.UserRole
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertInfo
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.AlertWarning
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.NavySecondary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSpeak: () -> Unit,
    onNavigateToSendMessage: () -> Unit,
    onNavigateToEmergencyReport: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToMeshNetwork: () -> Unit,
    onNavigateToGovtAlertCreation: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val meshStatus by viewModel.meshStatus.collectAsState()

    val latestAlert = alerts.firstOrNull { !it.isExpired } ?: alerts.firstOrNull()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "ThasmathJagratha",
                subtitle = "Multilingual Intelligent Emergency Communication",
                selectedLanguage = user.preferredLanguage,
                onLanguageClick = onNavigateToLanguage
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
        ) {
            // User Greeting Header & Alert Receiver Active Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyPrimary)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "User: ${user.name}",
                                color = SurfaceWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = SurfaceWhite.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Role: ${user.role.displayName} | ${user.district}",
                                    color = SurfaceWhite.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        VerifiedBadge(text = if (user.verified) "Verified" else "Pending")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // NON-TOGGLEABLE Informational Receiver Active Indicator
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = AlertVerified, modifier = Modifier.size(8.dp)) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alert Receiver Active",
                                    color = AlertVerified,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Connectivity Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Listening", color = SurfaceWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // Main Push-To-Talk Emergency Speech Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSpeak() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySecondary)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AlertCritical,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Push To Talk",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Push-To-Talk Emergency Speech",
                                color = SurfaceWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Instant speech-to-text in ${user.preferredLanguage}",
                                color = SurfaceWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role-based Create Official Alert (Government role only)
                if (user.role == UserRole.GOVERNMENT) {
                    Button(
                        onClick = onNavigateToGovtAlertCreation,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCritical),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Official Government Alert", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Emergency Communications & Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Action Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    HomeActionCard(
                        title = "Push to Talk",
                        subtitle = "Voice STT & TTS",
                        icon = Icons.Default.Mic,
                        iconColor = AlertCritical,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSpeak
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    HomeActionCard(
                        title = "Send Message",
                        subtitle = "Direct dispatch",
                        icon = Icons.Default.Send,
                        iconColor = NavyPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSendMessage
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    HomeActionCard(
                        title = "Emergency Report",
                        subtitle = "Submit distress signal",
                        icon = Icons.Default.ReportProblem,
                        iconColor = AlertCritical,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEmergencyReport
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    HomeActionCard(
                        title = "View Alerts",
                        subtitle = "${alerts.size} active broadcasts",
                        icon = Icons.Default.NotificationsActive,
                        iconColor = AlertWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAlerts
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    HomeActionCard(
                        title = "View Messages",
                        subtitle = "Direct inbox logs",
                        icon = Icons.Default.Message,
                        iconColor = AlertInfo,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMessages
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    HomeActionCard(
                        title = "Network Status",
                        subtitle = "${meshStatus.nearbyPeers} peers in range",
                        icon = Icons.Default.CellTower,
                        iconColor = AlertVerified,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMeshNetwork
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Latest Broadcast Preview
                Text(text = "Active Verified Alert Bulletin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                if (latestAlert != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAlertDetail(latestAlert.alertId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(latestAlert.alertType, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                SeverityChip(severity = latestAlert.severity)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Authority: ${latestAlert.authority} • ${latestAlert.targetArea}", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("“${latestAlert.translatedMessage}”", fontSize = 13.sp, color = TextPrimary, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
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
