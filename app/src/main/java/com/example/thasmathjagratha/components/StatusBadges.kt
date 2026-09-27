package com.example.thasmathjagratha.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.UserRole
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.AlertInfo
import com.example.thasmathjagratha.theme.AlertInfoBackground
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.AlertVerifiedBackground
import com.example.thasmathjagratha.theme.AlertWarning
import com.example.thasmathjagratha.theme.AlertWarningBackground
import com.example.thasmathjagratha.theme.NavyPrimary

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    text: String = "Verified Govt Authority"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AlertVerifiedBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = AlertVerified,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = AlertVerified,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SeverityChip(
    severity: AlertSeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (severity) {
        AlertSeverity.CRITICAL -> Pair(AlertCriticalBackground, AlertCritical)
        AlertSeverity.HIGH -> Pair(AlertCriticalBackground, AlertCritical)
        AlertSeverity.IMPORTANT -> Pair(AlertWarningBackground, AlertWarning)
        AlertSeverity.NORMAL -> Pair(AlertInfoBackground, AlertInfo)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (severity == AlertSeverity.CRITICAL || severity == AlertSeverity.HIGH) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = severity.displayName.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UserRoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NavyPrimary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = role.displayName,
                color = NavyPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
