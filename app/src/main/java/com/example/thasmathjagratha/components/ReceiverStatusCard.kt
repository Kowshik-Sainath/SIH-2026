package com.example.thasmathjagratha.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.model.ReceiverProfile
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite

@Composable
fun ReceiverStatusCard(
    profile: ReceiverProfile,
    isSimulating: Boolean,
    onSimulateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NavyPrimary)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ThasmathJagratha Receiver",
                        color = SurfaceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = AlertVerified, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Status: RECEIVING ALERTS",
                            color = AlertVerified,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                VerifiedBadge(text = if (profile.verified) "Verified Node" else "Unverified")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoTag(icon = Icons.Default.LocationOn, label = "Area", value = profile.district)
                InfoTag(icon = Icons.Default.Language, label = "Preferred Lang", value = profile.preferredLanguage)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation Trigger Button
            Button(
                onClick = onSimulateClick,
                enabled = !isSimulating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceWhite,
                    contentColor = NavyPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSimulating) "Simulating Incoming Alert Packet..." else "Simulate Incoming Critical Alert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun InfoTag(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceWhite.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = label, color = SurfaceWhite.copy(alpha = 0.7f), fontSize = 9.sp)
                Text(text = value, color = SurfaceWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
