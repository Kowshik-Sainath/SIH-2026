package com.example.thasmathjagratha.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.ReceivedAlert
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.AlertWarning
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary

@Composable
fun AlertCard(
    alert: ReceivedAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertCritical
        AlertSeverity.HIGH -> Color(0xFFED6C02)
        AlertSeverity.IMPORTANT -> AlertWarning
        AlertSeverity.NORMAL -> Color(0xFF1976D2)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.5.dp, if (alert.expired) Color.LightGray else borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = if (alert.expired) Color.Gray else borderColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.alertType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alert.expired) Color.Gray else TextPrimary
                    )
                }

                if (alert.expired) {
                    VerificationBadge(state = VerificationState.EXPIRED)
                } else {
                    SeverityChip(severity = alert.severity)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = alert.authority,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertCritical, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${alert.location} • Preferred Language: ${alert.preferredLanguage}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${alert.translatedMessage}”",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alert.verified) {
                    VerificationBadge(state = VerificationState.VERIFIED)
                } else {
                    VerificationBadge(state = VerificationState.UNVERIFIED)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CellTower, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hop ${alert.hopCount}/${alert.maxHops}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
            }
        }
    }
}
