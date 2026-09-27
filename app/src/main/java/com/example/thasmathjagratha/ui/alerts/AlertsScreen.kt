package com.example.thasmathjagratha.ui.alerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.AlertCard
import com.example.thasmathjagratha.components.EmptyState
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.ReceivedAlert
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    onNavigateToAlertDetail: (String) -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Critical", "Warnings", "Local")

    val filteredAlerts = when (selectedFilter) {
        "Critical" -> alerts.filter { it.severity == AlertSeverity.CRITICAL }
        "Warnings" -> alerts.filter { it.severity == AlertSeverity.HIGH || it.severity == AlertSeverity.IMPORTANT }
        else -> alerts
    }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Emergency Alerts Hub",
                subtitle = "Verified Government & Field Warnings"
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
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NavyPrimary else Color.White,
                        modifier = Modifier.clickable { selectedFilter = filter },
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAlerts.isEmpty()) {
                EmptyState(
                    title = "No $selectedFilter Alerts",
                    subtitle = "No active broadcasts matching this filter."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAlerts) { alert ->
                        AlertCard(
                            alert = ReceivedAlert(
                                alertId = alert.alertId,
                                alertType = alert.alertType,
                                authority = alert.authority,
                                message = alert.message,
                                translatedMessage = alert.translatedMessage,
                                originalLanguage = alert.originalLanguage,
                                preferredLanguage = alert.preferredLanguage,
                                severity = alert.severity,
                                location = alert.targetArea,
                                issuedAt = alert.issuedAt,
                                expiryTime = alert.expiryTime,
                                verified = alert.verified,
                                acknowledged = alert.acknowledged,
                                expired = alert.isExpired,
                                hopCount = alert.hopCount,
                                maxHops = alert.maxHops
                            ),
                            onClick = { onNavigateToAlertDetail(alert.alertId) }
                        )
                    }
                }
            }
        }
    }
}
