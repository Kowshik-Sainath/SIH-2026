package com.example.thasmathjagratha.ui.receiver

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.AlertTimer
import com.example.thasmathjagratha.components.SeverityChip
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.TtsPlayer
import com.example.thasmathjagratha.components.VerificationBadge
import com.example.thasmathjagratha.components.VerificationState
import com.example.thasmathjagratha.service.ReceiverListeningState
import com.example.thasmathjagratha.service.ReceiverStateHolder
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun ReceiverMainScreen(
    viewModel: MainViewModel,
    onNavigateToForwarding: (String) -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToRoleSelection: () -> Unit = {},
    onNavigateToTts: () -> Unit = {}
) {
    val context = LocalContext.current
    val alerts by viewModel.alerts.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val receiverStatus by ReceiverStateHolder.status.collectAsState()
    val currentSpeakingLang by ReceiverStateHolder.currentSpeakingLang.collectAsState()
    val activeAlert = alerts.firstOrNull { !it.isExpired } ?: alerts.firstOrNull()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "TTS Mode (Receiver)",
                subtitle = "Continuous Alert Listener & Speech Synthesis — SIH 26173",
                roleBadge = "TTS Mode (Receiver)",
                onChangeRoleClick = {
                    viewModel.changeRole(context)
                    onNavigateToRoleSelection()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Receiver Mode Status Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (receiverStatus) {
                        ReceiverListeningState.IDLE_LISTENING -> AlertVerified
                        ReceiverListeningState.RECEIVING -> Color(0xFFF57C00)
                        ReceiverListeningState.SPEAKING -> NavyPrimary
                    }
                    val statusIcon = when (receiverStatus) {
                        ReceiverListeningState.IDLE_LISTENING -> Icons.Default.Hearing
                        ReceiverListeningState.RECEIVING -> Icons.Default.CellTower
                        ReceiverListeningState.SPEAKING -> Icons.Default.VolumeUp
                    }
                    val statusTitle = when (receiverStatus) {
                        ReceiverListeningState.IDLE_LISTENING -> "LISTENING FOR ALERTS"
                        ReceiverListeningState.RECEIVING -> "RECEIVING BROADCAST"
                        ReceiverListeningState.SPEAKING -> "SPEAKING ALERT (${currentSpeakingLang?.uppercase() ?: "AUDIO"})"
                    }
                    val statusSubtitle = when (receiverStatus) {
                        ReceiverListeningState.IDLE_LISTENING -> "UDP:28154 & Bluetooth RFCOMM background service active"
                        ReceiverListeningState.RECEIVING -> "Processing incoming alert packet..."
                        ReceiverListeningState.SPEAKING -> "On-device neural TTS playback with USAGE_ALARM"
                    }

                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                        Text(
                            text = statusSubtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Offline TTS Voice Packs Management Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Offline TTS Voice Packs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Download Indian voice packs (VITS & Piper)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Button(
                        onClick = onNavigateToTts,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Packs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (activeAlert != null) {
                // Active Alert Warning Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AlertCritical)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceWhite.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ACTIVE EMERGENCY ALERT",
                                color = SurfaceWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Alert ID: ${activeAlert.alertId} | Language: ${activeAlert.originalLanguage}",
                                color = SurfaceWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {

                    // Verification & Source Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeAlert.alertType,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                SeverityChip(severity = activeAlert.severity)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sender: ${activeAlert.authority}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VerificationBadge(state = if (activeAlert.verified) VerificationState.VERIFIED else VerificationState.UNVERIFIED)
                                AlertTimer(expiryTimeMillis = activeAlert.expiryTime, isExpired = activeAlert.isExpired)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Received Spoken Message Card (Direct speech text, no translation)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Received Spoken Message (${activeAlert.originalLanguage}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AlertVerified.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (activeAlert.receivedVia.isNotBlank()) activeAlert.receivedVia else "Voice Relay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertVerified,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AlertCriticalBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "“${activeAlert.message}”",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(14.dp),
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TTS Voice Synthesis Component
                    TtsPlayer(
                        isPlaying = ttsState.isPlaying,
                        isPaused = ttsState.isPaused,
                        language = activeAlert.originalLanguage,
                        speechSpeed = ttsState.speechSpeed,
                        onPlay = { viewModel.playTts(context, activeAlert.message, activeAlert.originalLanguage) },
                        onPause = { viewModel.pauseTts() },
                        onStop = { viewModel.stopTts() },
                        onSpeedChange = { speed -> viewModel.setTtsSpeed(speed) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Acknowledge Action Button
                    if (!activeAlert.acknowledged) {
                        Button(
                            onClick = { viewModel.acknowledgeAlert(activeAlert.alertId) },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acknowledge & Confirm Receipt", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertVerified)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alert Receipt Confirmed & Logged", color = AlertVerified, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // Standby: Waiting for incoming transmissions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Standby: Listening for Transmissions",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This device is configured as the Audio Receiver (TTS Mode). When any sender device transmits an emergency alert via local Wi-Fi or Bluetooth, this device will receive it and speak it aloud.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Multi-Tier Transport Info Badges
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BackgroundLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CellTower, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("BLE Beacon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("0x0A99 Zero-Pair", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BackgroundLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Speech Channel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("P2P/Hotspot 28155", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BackgroundLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = AlertVerified, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Wi-Fi UDP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Port 28154", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BackgroundLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Bluetooth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Classic RFCOMM", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
