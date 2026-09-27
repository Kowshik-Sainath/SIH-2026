package com.example.thasmathjagratha.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun ReceiverSettingsScreen(
    viewModel: MainViewModel,
    onNavigateToLanguage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val profile by viewModel.receiverProfile.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val autoPlayAlerts by viewModel.autoPlayAlerts.collectAsState()
    val notificationSound by viewModel.notificationSound.collectAsState()
    val emergencyVibration by viewModel.emergencyVibration.collectAsState()

    var showAboutModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Receiver Preferences & Settings",
                subtitle = "TTS, Relay Permissions & Accessibility",
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
            Text("Alerts & Voice Synthesis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingClickableRow(
                        icon = Icons.Default.Language,
                        title = "Preferred Alert Language",
                        value = profile.preferredLanguage,
                        onClick = onNavigateToLanguage
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Auto Play Critical Alerts (TTS)",
                        checked = autoPlayAlerts,
                        onCheckedChange = { isChecked ->
                            viewModel.updateSettings(textSize, isChecked, notificationSound, emergencyVibration)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Emergency Loud Alert Sound",
                        checked = notificationSound,
                        onCheckedChange = { isChecked ->
                            viewModel.updateSettings(textSize, autoPlayAlerts, isChecked, emergencyVibration)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Emergency Vibration Pattern",
                        checked = emergencyVibration,
                        onCheckedChange = { isChecked ->
                            viewModel.updateSettings(textSize, autoPlayAlerts, notificationSound, isChecked)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Mesh Relay & Accessibility", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingSwitchRow(
                        icon = Icons.Default.CellTower,
                        title = "Allow P2P Mesh Forwarding Relay",
                        checked = profile.meshRelayEnabled,
                        onCheckedChange = { viewModel.toggleMeshRelay() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.TextFormat,
                        title = "Text Size Accessibility",
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
                        icon = Icons.Default.PrivacyTip,
                        title = "Mesh Data Privacy & Encryption",
                        value = "AES-GCM Hash",
                        onClick = { viewModel.showSnackbar("Peer traffic payload is end-to-end encrypted.") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingClickableRow(
                        icon = Icons.Default.Info,
                        title = "About ThasmathJagratha Receiver Engine",
                        value = "v1.0.0 SIH",
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
                            Text("ThasmathJagratha Receiver Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SIH Emergency Receiver system. Validates digital signatures of incoming alert packets, prevents loop duplicates via Bloom filters, translates alert text into 10 Indian languages, and synthesizes localized TTS audio.",
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
