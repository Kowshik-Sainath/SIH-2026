package com.example.thasmathjagratha.ui.messages

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.components.VerifiedBadge
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AppMessage
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    onNavigateToMessageType: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var quickMessageText by remember { mutableStateOf("") }

    val tabs = listOf("All", "Sent", "Received", "Emergency")

    val filteredMessages = when (selectedTabIndex) {
        1 -> messages.filter { it.isSentByMe }
        2 -> messages.filter { !it.isSentByMe }
        3 -> messages.filter { it.priority == AlertSeverity.HIGH || it.priority == AlertSeverity.CRITICAL }
        else -> messages
    }

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Emergency Communications",
                subtitle = "Direct & Mesh Relayed Message Logs"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToMessageType,
                containerColor = NavyPrimary,
                contentColor = SurfaceWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Compose Message")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceWhite,
                contentColor = NavyPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMessages) { msg ->
                        AppMessageCard(msg = msg)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Send Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickMessageText,
                        onValueChange = { quickMessageText = it },
                        placeholder = { Text("Type quick message to Control Room...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (quickMessageText.isNotBlank()) {
                                viewModel.sendUserMessage("Control Room", "Telugu", AlertSeverity.NORMAL, quickMessageText)
                                quickMessageText = ""
                            }
                        },
                        containerColor = NavyPrimary,
                        contentColor = SurfaceWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AppMessageCard(msg: AppMessage) {
    val isEmergency = msg.priority == AlertSeverity.CRITICAL || msg.priority == AlertSeverity.HIGH

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEmergency) Icons.Default.ReportProblem else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isEmergency) AlertCritical else NavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (msg.isSentByMe) "To: ${msg.receiverName ?: "Control Room"}" else "From: ${msg.senderName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Delivered",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = msg.message,
                fontSize = 13.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerifiedBadge(text = "Verified Message")

                Text(
                    text = if (msg.isSentByMe) "Delivered via Mesh" else "Verified Received",
                    fontSize = 10.sp,
                    color = AlertVerified,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
