package com.example.thasmathjagratha.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun SendMessageScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var recipient by remember { mutableStateOf("Rescue Team 01") }
    var selectedLanguage by remember { mutableStateOf("Telugu") }
    var selectedPriority by remember { mutableStateOf(AlertSeverity.NORMAL) }
    var messageText by remember { mutableStateOf("Please update status of Sector 7 evacuation.") }
    var isSent by remember { mutableStateOf(false) }

    var recipientExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    val recipients = listOf("Rescue Team 01", "Control Room", "Field Officer 07", "Medical Unit 03", "Civil Defense Hub")
    val languages = listOf("Telugu", "Hindi", "English", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali", "Gujarati")

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Send Message",
                subtitle = "Multilingual Direct Dispatch",
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
            if (isSent) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sent",
                            tint = AlertVerified,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Message Sent", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AlertVerified)
                        Text("Status: Delivered via Mesh Relay", fontSize = 13.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Return to Messages", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text("Message Transmission Fields", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Recipient Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recipient") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { recipientExpanded = true },
                        shape = RoundedCornerShape(10.dp)
                    )
                    DropdownMenu(
                        expanded = recipientExpanded,
                        onDismissRequest = { recipientExpanded = false }
                    ) {
                        recipients.forEach { rec ->
                            DropdownMenuItem(
                                text = { Text(rec) },
                                onClick = {
                                    recipient = rec
                                    recipientExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { langExpanded = true },
                        shape = RoundedCornerShape(10.dp)
                    )
                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    selectedLanguage = lang
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Picker
                Text("Priority Level", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(AlertSeverity.NORMAL, AlertSeverity.IMPORTANT, AlertSeverity.HIGH).forEach { prio ->
                        val isSelected = selectedPriority == prio
                        val color = when (prio) {
                            AlertSeverity.NORMAL -> NavyPrimary
                            AlertSeverity.IMPORTANT -> Color(0xFFED6C02)
                            AlertSeverity.HIGH -> AlertCritical
                            else -> NavyPrimary
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clickable { selectedPriority = prio },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) color else SurfaceWhite
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                Text(
                                    text = prio.displayName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SurfaceWhite else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text Input Field + Optional Voice Mic Icon
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message Content") },
                    trailingIcon = {
                        IconButton(onClick = { messageText = "విజయవాడలో వరద నీరు పెరుగుతోంది" }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = NavyPrimary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.sendUserMessage(
                            recipient = recipient,
                            language = selectedLanguage,
                            priority = selectedPriority,
                            text = messageText
                        )
                        isSent = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Message", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
