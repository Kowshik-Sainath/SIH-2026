package com.example.thasmathjagratha.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.UserRoleBadge
import com.example.thasmathjagratha.model.UserRole
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.AlertInfo
import com.example.thasmathjagratha.theme.AlertWarning
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun MessageTypeScreen(
    viewModel: MainViewModel,
    onNavigateToEmergencyReport: () -> Unit,
    onNavigateToGovtAlertCreation: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Select Transmission Type",
                subtitle = "Choose broadcast channel and scope",
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operational Channels",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                UserRoleBadge(role = user.role)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Private Message
            OptionCard(
                title = "Private Communication",
                subtitle = "Direct encrypted message to specific Control Room or Field Unit",
                icon = Icons.Default.Message,
                iconColor = AlertInfo,
                isAuthorized = true,
                disabledText = null,
                onClick = onNavigateToMessages
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Emergency Report
            OptionCard(
                title = "Emergency Incident Report",
                subtitle = "Submit verified distress signal with GPS location & severity tag",
                icon = Icons.Default.ReportProblem,
                iconColor = AlertCritical,
                isAuthorized = true,
                disabledText = null,
                onClick = onNavigateToEmergencyReport
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Public Alert Broadcast
            val isPublicAlertAuthorized = user.role != UserRole.CITIZEN
            OptionCard(
                title = "Public Government Alert",
                subtitle = "City-wide or regional emergency broadcast notice",
                icon = Icons.Default.Campaign,
                iconColor = AlertWarning,
                isAuthorized = isPublicAlertAuthorized,
                disabledText = if (!isPublicAlertAuthorized) "Not Authorized (Citizens cannot broadcast public alerts)" else null,
                onClick = {
                    if (isPublicAlertAuthorized) {
                        onNavigateToGovtAlertCreation()
                    } else {
                        viewModel.showSnackbar("You are not authorized to perform this action.")
                    }
                }
            )
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isAuthorized: Boolean,
    disabledText: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAuthorized) SurfaceWhite else Color(0xFFF1F5F9)
        ),
        border = if (!isAuthorized) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isAuthorized) iconColor else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAuthorized) TextPrimary else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (!isAuthorized && disabledText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertCriticalBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Not Authorized",
                            tint = AlertCritical,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = disabledText,
                            color = AlertCritical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
