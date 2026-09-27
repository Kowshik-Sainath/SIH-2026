package com.example.thasmathjagratha.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.repository.SimulatedEventType
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.SurfaceWhite

@Composable
fun SimulateAlertFab(
    onSimulate: (SimulatedEventType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = AlertCritical,
            contentColor = SurfaceWhite
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "Simulate Alert")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("⚡ Critical Govt Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.CRITICAL_GOVT_ALERT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("✉️ Normal Message", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.NORMAL_MESSAGE)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("⚠️ Important Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.IMPORTANT_ALERT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("🔶 High Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.HIGH_ALERT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("❓ Unverified Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.UNVERIFIED_ALERT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("🔄 Duplicate Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.DUPLICATE_ALERT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("⌛ Expired Alert", fontSize = 13.sp) },
                onClick = {
                    onSimulate(SimulatedEventType.EXPIRED_ALERT)
                    expanded = false
                }
            )
        }
    }
}
