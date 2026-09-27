package com.example.thasmathjagratha.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
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

@Composable
fun AlertTimer(
    expiryTimeMillis: Long,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val remainingMins = ((expiryTimeMillis - System.currentTimeMillis()) / (1000 * 60)).coerceAtLeast(0)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isExpired) Color(0xFFF1F5F9) else AlertCritical.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (isExpired) Color.Gray else AlertCritical,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isExpired) "Expired" else "Valid for ${remainingMins}m",
                color = if (isExpired) Color.Gray else AlertCritical,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
