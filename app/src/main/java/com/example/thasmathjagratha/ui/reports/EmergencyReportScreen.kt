package com.example.thasmathjagratha.ui.reports

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun EmergencyReportScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(AlertType.FLOOD) }
    var location by remember { mutableStateOf("Prakasam Barrage, Vijayawada") }
    var selectedSeverity by remember { mutableStateOf(AlertSeverity.HIGH) }
    var message by remember { mutableStateOf("Water overflow near residential colony. Families need assistance.") }
    var isGpsAttached by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var typeExpanded by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AlertVerified,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Report Sent Successfully",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Your emergency report has been routed to the District Control Room and nearby rescue units. GPS coordinates attached.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Send Emergency Report",
                subtitle = "Distress signal to District Control Room",
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
            Text(
                text = "Incident Classification",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Emergency Type Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedType.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Emergency Type") },
                    leadingIcon = { Icon(Icons.Default.ReportProblem, contentDescription = null, tint = AlertCritical) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true },
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.88f)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Severity Level Selector
            Text(
                text = "Severity Level",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AlertSeverity.values().forEach { severity ->
                    val isSelected = selectedSeverity == severity
                    val chipColor = when (severity) {
                        AlertSeverity.CRITICAL -> AlertCritical
                        AlertSeverity.HIGH -> AlertCritical
                        AlertSeverity.IMPORTANT -> Color(0xFFED6C02)
                        AlertSeverity.NORMAL -> Color(0xFF1976D2)
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { selectedSeverity = severity },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) chipColor else SurfaceWhite
                        ),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = severity.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SurfaceWhite else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Input & Attach Location Button
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Incident Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { isGpsAttached = !isGpsAttached },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = if (isGpsAttached) AlertVerified else NavyPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGpsAttached) "GPS Coordinates Attached (16.5062° N, 80.6480° E) ✓" else "Attach Current GPS Location",
                    fontSize = 12.sp,
                    color = if (isGpsAttached) AlertVerified else NavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Message text input with Voice microphone button
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Emergency Situation Details") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            isRecording = !isRecording
                            if (isRecording) {
                                message = "Water level rising fast. 4 families stranded near Prakasam Barrage."
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Details",
                            tint = if (isRecording) AlertCritical else NavyPrimary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.sendEmergencyReport(
                        emergencyType = selectedType,
                        location = location,
                        severity = selectedSeverity,
                        message = message
                    )
                    showSuccessDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertCritical),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send to Control Room",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
