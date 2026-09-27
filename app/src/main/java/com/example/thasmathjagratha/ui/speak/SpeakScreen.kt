package com.example.thasmathjagratha.ui.speak

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.stt.manager.LanguagePackManager
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun SpeakScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTts: () -> Unit
) {
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRealListening(context)
        } else {
            viewModel.showSnackbar("Microphone permission required. Running STT simulation.")
            viewModel.simulateRecording()
        }
    }

    val speechState by viewModel.speechState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val telemetry by viewModel.telemetrySnapshot.collectAsState()
    val ttsLogs by TtsTelemetry.lines.collectAsState()
    val deliveryStatus by viewModel.transportDeliveryStatus.collectAsState()

    val packManager = remember { LanguagePackManager(context) }
    val availablePacks = packManager.availablePacks

    val languages = availablePacks.map { it.displayName }
    val receivers = listOf("Control Room", "Rescue Team 1", "Nearby Officers", "Public Broadcast")

    var langExpanded by remember { mutableStateOf(false) }
    var receiverExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }
    var showTelemetryDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "STT Mode (Sender)",
                subtitle = "Push-To-Talk Speech-to-Text — SIH 26173",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                selectedLanguage = speechState.selectedLanguage,
                roleBadge = "STT Mode (Sender)",
                onChangeRoleClick = {
                    viewModel.changeRole(context)
                    onNavigateBack()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language & Receiver Dropdowns
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = speechState.selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Speech Language") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { langExpanded = true }
                    )
                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    viewModel.updateSpeechLanguage(lang)
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = speechState.selectedReceiver,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Receiver") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { receiverExpanded = true }
                    )
                    DropdownMenu(
                        expanded = receiverExpanded,
                        onDismissRequest = { receiverExpanded = false }
                    ) {
                        receivers.forEach { rec ->
                            DropdownMenuItem(
                                text = { Text(rec) },
                                onClick = {
                                    viewModel.updateSpeechReceiver(rec)
                                    receiverExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sender Mode Operational Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.07f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (speechState.isListening) AlertCritical else AlertVerified,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (speechState.isListening) "PTT: RECORDING MIC..." else "PTT: IDLE (READY)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (speechState.isListening) AlertCritical else AlertVerified
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavyPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Lang: ${speechState.selectedLanguage}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transport Status: $deliveryStatus",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Microphone Button (Push-To-Talk)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                if (speechState.isListening) {
                    Surface(
                        shape = CircleShape,
                        color = AlertCritical.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize().scale(pulseScale)
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = AlertCritical.copy(alpha = 0.4f),
                        modifier = Modifier.size(130.dp).scale(pulseScale * 0.9f)
                    ) {}
                }

                Surface(
                    shape = CircleShape,
                    color = if (speechState.isListening) AlertCritical else NavyPrimary,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(110.dp)
                        .clickable {
                            if (speechState.isListening) {
                                viewModel.stopRealListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startRealListening(context)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Push to Talk",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (speechState.isListening) "Listening & Streaming STT..." else "Tap Microphone for Push-To-Talk",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (speechState.isListening) AlertCritical else TextPrimary
            )
            Text(
                text = "INT8 ONNX Runtime (Indic) + sherpa-onnx NeMo Conformer (English)",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Waveform Meter
            Card(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerY = canvasHeight / 2
                        val barWidth = 6f
                        val gap = 10f
                        val numBars = (canvasWidth / (barWidth + gap)).toInt()

                        for (i in 0 until numBars) {
                            val x = i * (barWidth + gap)
                            val levelMultiplier = if (speechState.isListening) {
                                (0.15f + (i % 4) * 0.15f + audioLevel * 2.0f).coerceIn(0.1f, 0.95f)
                            } else {
                                0.1f
                            }
                            val barHeight = canvasHeight * levelMultiplier
                            drawLine(
                                color = if (speechState.isListening) AlertCritical else NavyPrimary,
                                start = Offset(x, centerY - barHeight / 2),
                                end = Offset(x, centerY + barHeight / 2),
                                strokeWidth = barWidth
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transcription Result Card
            if (speechState.isTranscribed || speechState.transcriptionText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AlertVerified,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Offline STT Transcription Result",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "Confidence: ${speechState.confidenceScore}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertVerified,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { isEditing = false },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Text("Done Editing")
                            }
                        } else {
                            Text(
                                text = "“${speechState.transcriptionText}”",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    editableText = speechState.transcriptionText
                                    isEditing = !isEditing
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditing) "Cancel" else "Edit", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { viewModel.sendVoiceMessage() },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Broadcast Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Language Pack Management Card (10 Offline Packs)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline Language Packs (9 Indic + English)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        IconButton(onClick = { showTelemetryDialog = true }) {
                            Icon(Icons.Default.Speed, contentDescription = "Telemetry Diagnostics", tint = NavyPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    availablePacks.forEach { pack ->
                        val isDownloaded = packManager.isPackDownloaded(pack.languageCode)
                        val code = pack.languageCode
                        val prog = downloadProgress[code]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pack.displayName} (${pack.nativeName})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "sherpa-onnx NeMo Conformer (INT8) • ${pack.modelSizeMb}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                                if (prog != null && prog < 1.0f) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { prog },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )
                                }
                            }

                            if (isDownloaded) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Installed",
                                        tint = AlertVerified,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteLanguagePack(context, pack.displayName) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.downloadLanguagePack(context, pack.displayName) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = NavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Telemetry Diagnostics Modal
            if (showTelemetryDialog) {
                AlertDialog(
                    onDismissRequest = { showTelemetryDialog = false },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = NavyPrimary) },
                    title = { Text("Speech Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column {
                            Text("Active Language: ${telemetry.languageCode}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Model Load Time: ${telemetry.modelLoadTimeMs} ms", fontSize = 12.sp)
                            Text("Avg Inference Time: ${String.format(Locale.US, "%.1f", telemetry.avgInferenceTimeMs)} ms/chunk", fontSize = 12.sp)
                            Text("Real-Time Factor (RTF): ${String.format(Locale.US, "%.2f", telemetry.realTimeFactor)}", fontSize = 12.sp)
                            Text("Memory Footprint: ${String.format(Locale.US, "%.1f", telemetry.memoryUsageMb)} MB", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Recent Logs:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            telemetry.logs.takeLast(5).forEach { log ->
                                Text("• $log", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("TTS:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            ttsLogs.takeLast(5).forEach { log -> Text("• $log", fontSize = 10.sp) }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showTelemetryDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
