package com.example.thasmathjagratha.ui.devices

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.model.NearbyDevice
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun NearbyDevicesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val devices by viewModel.nearbyDevices.collectAsState()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Nearby Mesh Devices",
                subtitle = "Local P2P Direct & Bluetooth Peers",
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Emergency Nodes (${devices.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedButton(
                    onClick = { viewModel.showSnackbar("Scanning for nearby mesh peers...") },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { dev ->
                    DeviceCard(
                        device = dev,
                        onToggleConnect = { viewModel.toggleDeviceConnection(dev.deviceId) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: NearbyDevice,
    onToggleConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = if (device.connected) AlertVerified else NavyPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (device.connected) Color(0xFFE8F5E9) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = if (device.connected) "Connected" else "In Range",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (device.connected) AlertVerified else Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.type} • Distance: ${device.distance}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Signal: ${device.signal}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (device.connected) {
                OutlinedButton(
                    onClick = onToggleConnect,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect", fontSize = 11.sp, color = Color.Red)
                }
            } else {
                Button(
                    onClick = onToggleConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Connect", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", fontSize = 11.sp)
                }
            }
        }
    }
}
