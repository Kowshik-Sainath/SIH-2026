package com.example.thasmathjagratha.ui.receiver

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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.thasmathjagratha.components.ForwardingProgress
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun ForwardingStatusScreen(
    alertId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val receivedAlerts by viewModel.receivedAlerts.collectAsState()
    val alert = receivedAlerts.find { it.alertId == alertId } ?: receivedAlerts.firstOrNull()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Controlled Mesh Forwarding",
                subtitle = "P2P Loop Prevention & Relay Hub",
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
                // Forwarding Overview Card
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
                            Text("Forwarding Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                                Text(
                                    text = if (alert.forwarded) "ACTIVE RELAY" else "INACTIVE",
                                    color = AlertVerified,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailRow(label = "Current Alert ID", value = alert.alertId)
                        DetailRow(label = "Target Geographic Area", value = alert.location)
                        DetailRow(label = "Hop Limit Count", value = "${alert.hopCount} / ${alert.maxHops} Hops")
                        DetailRow(label = "Nearby Discovered Peers", value = "7 Devices")
                        DetailRow(label = "Forwarded Successfully", value = "${alert.forwardedDeviceCount} Devices")
                        DetailRow(label = "Failed / Dropped Packets", value = "1 Packet")
                        DetailRow(label = "Time Remaining", value = "18 minutes")

                        Spacer(modifier = Modifier.height(16.dp))

                        ForwardingProgress(
                            alertId = alert.alertId,
                            hopCount = alert.hopCount,
                            maxHops = alert.maxHops,
                            nearbyDevicesCount = 7,
                            successCount = alert.forwardedDeviceCount,
                            failedCount = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.showSnackbar("Rescanning mesh relay nodes...") },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Relay Nodes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
