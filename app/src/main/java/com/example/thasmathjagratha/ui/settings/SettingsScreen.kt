package com.example.thasmathjagratha.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToLanguage: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val autoPlayAlerts by viewModel.autoPlayAlerts.collectAsState()
    val notificationSound by viewModel.notificationSound.collectAsState()
    val emergencyVibration by viewModel.emergencyVibration.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()

    var showAboutModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Application Settings",
                subtitle = "Alert Receiver Preferences & System Configuration",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Background Alert Receiver System Status (Informational)
            Text("System Status Indicators", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusInfoRow(title = "Background Alert Receiver", statusText = "ACTIVE (Always On)")
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusInfoRow(title = "Critical Alert Service", statusText = "ACTIVE (Priority Overrides)")
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusInfoRow(title = "Mesh Peer Relaying", statusText = "ENABLED")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Preferences & Accessibility", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingClickableRow(
                        icon = Icons.Default.Language,
                        title = "Preferred Language",
                        value = user.preferredLanguage,
                        onClick = onNavigateToLanguage
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Auto Play Critical Alerts (TTS)",
                        checked = autoPlayAlerts,
                        onCheckedChange = { viewModel.updateSettings(textSize, it, notificationSound, emergencyVibration) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Emergency Vibration",
                        checked = emergencyVibration,
                        onCheckedChange = { viewModel.updateSettings(textSize, autoPlayAlerts, notificationSound, it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.TextFormat,
                        title = "Text Size",
                        value = textSize,
                        onClick = {
                            val next = when (textSize) {
                                "Normal" -> "Large"
                                "Large" -> "Extra Large"
                                else -> "Normal"
                            }
                            viewModel.updateSettings(next, autoPlayAlerts, notificationSound, emergencyVibration)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.Notifications,
                        title = "Speech Speed",
                        value = "${ttsState.speechSpeed}x",
                        onClick = {
                            val next = when (ttsState.speechSpeed) {
                                0.75f -> 1.0f
                                1.0f -> 1.25f
                                else -> 0.75f
                            }
                            viewModel.setTtsSpeed(next)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Permissions & Security", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingClickableRow(
                        icon = Icons.Default.Security,
                        title = "Emergency Alert Permissions",
                        value = "Configure DND Access",
                        onClick = onNavigateToPermissions
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Data Privacy & Mesh Encryption",
                        value = "AES-256 GCM",
                        onClick = { viewModel.showSnackbar("Traffic payload is end-to-end encrypted.") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.Info,
                        title = "About ThasmathJagratha",
                        value = "v1.0.0 SIH Unified",
                        onClick = { showAboutModal = !showAboutModal }
                    )
                }
            }

            if (showAboutModal) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ThasmathJagratha Unified System", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Multilingual Intelligent Emergency Communication Platform. Features unified auto-receiving background architecture, simulated TTS playback in 10 Indian languages, DND policy integration, and mesh loop duplicate filtering.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusInfoRow(title: String, statusText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
            Text(text = statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertVerified, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun SettingClickableRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NavyPrimary)
        )
    }
}
