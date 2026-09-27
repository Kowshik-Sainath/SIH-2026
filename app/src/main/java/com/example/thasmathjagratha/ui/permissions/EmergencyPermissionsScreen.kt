package com.example.thasmathjagratha.ui.permissions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun EmergencyPermissionsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val permissionsState by viewModel.permissionsState.collectAsState()

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Emergency Alert Permissions",
                subtitle = "Android Policy & DND Access Setup",
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
                text = "Android System Policy Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Critical government broadcasts require special Android notification permissions to override silence during life-threatening disasters.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    PermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        isGranted = permissionsState.notificationsGranted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRow(
                        icon = Icons.Default.PriorityHigh,
                        title = "High Priority Notifications",
                        isGranted = permissionsState.highPriorityGranted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRow(
                        icon = Icons.Default.Fullscreen,
                        title = "Full Screen Alerts",
                        isGranted = permissionsState.fullScreenAlertsGranted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRow(
                        icon = Icons.Default.DoNotDisturbOn,
                        title = "Do Not Disturb (DND) Access",
                        isGranted = permissionsState.dndAccessGranted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DND Notice Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, Color(0xFFED6C02))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFED6C02),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "To allow critical government alerts during Do Not Disturb, enable Emergency Alert Access in Android settings.",
                        fontSize = 12.sp,
                        color = Color(0xFFB78103),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current DND Status Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (permissionsState.dndAccessGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (permissionsState.dndAccessGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (permissionsState.dndAccessGranted) AlertVerified else AlertCritical
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency Alerts During DND: ${if (permissionsState.dndAccessGranted) "ENABLED" else "DISABLED"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (permissionsState.dndAccessGranted) AlertVerified else AlertCritical
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.toggleDndPermission() },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (permissionsState.dndAccessGranted) "Revoke DND Permission" else "Allow Emergency Alert Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ) {
            Text(
                text = if (isGranted) "Granted" else "Not Granted",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) AlertVerified else AlertCritical,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
