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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.thasmathjagratha.components.SeverityChip
import com.example.thasmathjagratha.components.VerifiedBadge
import com.example.thasmathjagratha.model.EmergencyAlert
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AlertType
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun GovtAlertCreationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(AlertType.FLOOD) }
    var selectedSeverity by remember { mutableStateOf(AlertSeverity.CRITICAL) }
    var targetArea by remember { mutableStateOf("Vijayawada East") }
    var radiusKm by remember { mutableFloatStateOf(10f) }
    var selectedLanguage by remember { mutableStateOf("Telugu") }
    var message by remember { mutableStateOf("కృష్ణా నది నీటిమట్టం వేగంగా పెరుగుతోంది. లోతట్టు ప్రాంతాల ప్రజలు వెంటనే సురక్షిత ప్రదేశాలకు వెళ్లండి.") }
    var validity by remember { mutableStateOf("30 minutes") }

    var isPreviewing by remember { mutableStateOf(false) }
    var publishedAlert by remember { mutableStateOf<EmergencyAlert?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    val languages = listOf("Telugu", "Hindi", "English", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali")

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Create Official Government Alert",
                subtitle = "Authorized Emergency Broadcast Hub",
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
            if (publishedAlert != null) {
                // Success Screen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = AlertVerified,
                            modifier = Modifier.size(60.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Alert Broadcast Started",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertVerified
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BackgroundLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Alert ID: ${publishedAlert?.alertId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Priority: ${publishedAlert?.severity?.displayName}", fontSize = 13.sp, color = AlertCritical)
                                Text("Coverage: $radiusKm km radius (${publishedAlert?.targetArea})", fontSize = 13.sp)
                                Text("Target Area: ${publishedAlert?.targetArea}", fontSize = 13.sp)
                                Text("Delivery: Not yet confirmed", fontSize = 13.sp, color = AlertVerified, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done & Return to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Official Broadcast Parameters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    VerifiedBadge(text = "Verified Authority")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Type & Language
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedType.title,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alert Type") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            AlertType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.title) },
                                    onClick = {
                                        selectedType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedLanguage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Language") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { langExpanded = true },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Severity Level Selection
                Text("Severity Priority", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    AlertSeverity.values().forEach { severity ->
                        val isSelected = selectedSeverity == severity
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clickable { selectedSeverity = severity },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AlertCritical else SurfaceWhite
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                Text(
                                    text = severity.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SurfaceWhite else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetArea,
                    onValueChange = { targetArea = it },
                    label = { Text("Target Area / Sector") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Broadcast Radius Coverage: ${radiusKm.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = radiusKm,
                    onValueChange = { radiusKm = it },
                    valueRange = 1f..50f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Official Alert Message") },
                    supportingText = { Text("Write in $selectedLanguage; receiving devices speak this text as sent.") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = validity,
                    onValueChange = { validity = it },
                    label = { Text("Validity Duration") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isPreviewing = !isPreviewing },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Preview, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            viewModel.broadcastGovernmentAlert(
                                alertType = selectedType,
                                severity = selectedSeverity,
                                targetArea = targetArea,
                                radiusKm = radiusKm.toInt(),
                                language = selectedLanguage,
                                message = message,
                                validity = validity,
                                onSuccess = { alert -> publishedAlert = alert }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCritical),
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Alert", fontWeight = FontWeight.Bold)
                    }
                }

                if (isPreviewing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertCritical)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("ALERT PREVIEW", color = AlertCritical, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(selectedType.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Target Area: $targetArea ($radiusKm km)", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(message, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
