package com.example.thasmathjagratha.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.AlertVerifiedBackground

enum class VerificationState {
    VERIFIED,
    UNVERIFIED,
    EXPIRED,
    DUPLICATE
}

@Composable
fun VerificationBadge(
    state: VerificationState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (state) {
        VerificationState.VERIFIED -> Quadruple(
            AlertVerifiedBackground,
            AlertVerified,
            Icons.Default.CheckCircle,
            "VERIFIED (Digital signature validated)"
        )
        VerificationState.UNVERIFIED -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFED6C02),
            Icons.Default.Warning,
            "UNVERIFIED (Source unvalidated)"
        )
        VerificationState.EXPIRED -> Quadruple(
            Color(0xFFF1F5F9),
            Color.Gray,
            Icons.Default.TimerOff,
            "EXPIRED (Validity ended)"
        )
        VerificationState.DUPLICATE -> Quadruple(
            AlertCriticalBackground,
            AlertCritical,
            Icons.Default.Block,
            "DUPLICATE (Already received & blocked)"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
