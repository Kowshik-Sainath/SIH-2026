package com.example.thasmathjagratha.ui.alerts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.AlertTimer
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.SeverityChip
import com.example.thasmathjagratha.components.VerificationBadge
import com.example.thasmathjagratha.components.VerificationState
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun AlertDetailScreen(
    alertId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()
    val alert = alerts.find { it.alertId == alertId } ?: alerts.firstOrNull()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Emergency Alert Metadata",
                subtitle = "Complete Network Dispatch Record",
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
            if (alert != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = alert.alertType, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            SeverityChip(severity = alert.severity)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AlertMetaRow("Alert ID", alert.alertId)
                        AlertMetaRow("Issuing Authority", alert.authority)
                        AlertMetaRow("Verification Status", if (alert.verified) "VERIFIED GOVERNMENT" else "UNVERIFIED")
                        AlertMetaRow("Target Scope", alert.targetArea)
                        AlertMetaRow("Original Language", alert.originalLanguage)
                        AlertMetaRow("Converted Language", alert.preferredLanguage)
                        AlertMetaRow("Mesh Hop Limit", "Hop ${alert.hopCount} / ${alert.maxHops}")
                        AlertMetaRow("Forwarding Status", "Relayed to 7 nearby peers")

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Original Broadcast Message:", fontSize = 11.sp, color = TextSecondary)
                        Text("“${alert.message}”", fontSize = 13.sp, color = TextPrimary)

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AlertCriticalBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Converted Message (${alert.preferredLanguage}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertCritical)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("“${alert.translatedMessage}”", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.playTts(alert.translatedMessage, alert.preferredLanguage) },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Audio (TTS)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
