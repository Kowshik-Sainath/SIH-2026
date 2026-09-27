package com.example.thasmathjagratha.ui.alerts

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.AlertTimer
import com.example.thasmathjagratha.components.SeverityChip
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.TtsPlayer
import com.example.thasmathjagratha.components.VerifiedBadge
import com.example.thasmathjagratha.repository.MockTranslationService
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun ReceivedAlertScreen(
    alertId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val alerts by viewModel.alerts.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val alert = alerts.find { it.alertId == alertId } ?: alerts.firstOrNull()

    val translationService = remember { MockTranslationService() }
    val receiverLanguages = listOf("Telugu", "Hindi", "English", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali")

    var selectedReceiverLang by remember { mutableStateOf(alert?.preferredLanguage ?: "Telugu") }
    var receiverLangExpanded by remember { mutableStateOf(false) }
    var isMapDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Emergency Alert Details",
                subtitle = "Official National Safety Bulletin",
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
        ) {
            if (alert != null) {
                val currentTranslatedText = remember(alert.message, selectedReceiverLang) {
                    translationService.translate(alert.message, selectedReceiverLang)
                }

                // Urgent Header Banner
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
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "CRITICAL EMERGENCY ALERT",
                                color = SurfaceWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Alert ID: ${alert.alertId} | Priority: ${alert.severity.displayName.uppercase()}",
                                color = SurfaceWhite.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {

                    // Alert Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = alert.alertType,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                SeverityChip(severity = alert.severity)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Authority: ${alert.authority}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            VerifiedBadge(text = "Government Verified Signal")

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertCritical, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Target Area: ${alert.targetArea}",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CellTower, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mesh Relay: ${alert.hopCount} Hop(s) | Spoken Lang: ${alert.originalLanguage}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Receiver Listening Language Dropdown (10 Languages)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Language to Listen (10 Languages):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedReceiverLang,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Receiver Audio Language") },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().clickable { receiverLangExpanded = true },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                DropdownMenu(
                                    expanded = receiverLangExpanded,
                                    onDismissRequest = { receiverLangExpanded = false }
                                ) {
                                    receiverLanguages.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang) },
                                            onClick = {
                                                selectedReceiverLang = lang
                                                receiverLangExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Translated Message Details Body
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertCriticalBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Converted Text ($selectedReceiverLang):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertCritical
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“$currentTranslatedText”",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TTS Voice Synthesis Component
                    TtsPlayer(
                        isPlaying = ttsState.isPlaying,
                        isPaused = ttsState.isPaused,
                        language = selectedReceiverLang,
                        speechSpeed = ttsState.speechSpeed,
                        onPlay = { viewModel.playTts(context, currentTranslatedText, selectedReceiverLang) },
                        onPause = { viewModel.pauseTts() },
                        onStop = { viewModel.stopTts() },
                        onSpeedChange = { speed -> viewModel.setTtsSpeed(speed) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // View Map Action
                    OutlinedButton(
                        onClick = { isMapDialogVisible = !isMapDialogVisible },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "View Map Zone")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Incident Map Zone", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Map Dialog Popup
            if (isMapDialogVisible) {
                AlertDialog(
                    onDismissRequest = { isMapDialogVisible = false },
                    title = { Text("Emergency Target Zone Map", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Target Area: ${alert?.targetArea}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Coordinates: 16.5062° N, 80.6480° E (Vijayawada)", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.LightGray.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Interactive GIS Incident Polygon Overlay", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { isMapDialogVisible = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
