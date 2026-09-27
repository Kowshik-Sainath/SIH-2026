package com.example.thasmathjagratha.tts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.thasmathjagratha.tts.engine.TtsEngine
import com.example.thasmathjagratha.tts.engine.TtsMode
import com.example.thasmathjagratha.tts.playback.TtsAudioPlayer
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun TtsDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { TtsEngine(context) }
    val player = remember { TtsAudioPlayer(context) }
    val scope = rememberCoroutineScope()
    val sampleTexts = mapOf(
        "te" to "వరద నీరు వేగంగా పెరుగుతోంది. సురక్షిత ప్రాంతాలకు వెళ్లండి.",
        "hi" to "बाढ़ का पानी तेजी से बढ़ रहा है। सुरक्षित स्थान पर जाएं।",
        "ta" to "வெள்ள நீர் வேகமாக உயர்ந்து வருகிறது. பாதுகாப்பான இடத்திற்கு செல்லவும்.",
        "kn" to "ಪ್ರವಾಹದ ನೀರು ವೇಗವಾಗಿ ಏರುತ್ತಿದೆ. ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತೆರಳಿ.",
        "mr" to "पुराचे पाणी वेगाने वाढत आहे. सुरक्षित ठिकाणी जा.",
        "bn" to "বন্যার জল দ্রুত বাড়ছে। নিরাপদ স্থানে যান।",
        "ml" to "വെള്ളപ്പൊക്ക ജലം വേഗത്തിൽ ഉയരുന്നു. സുരക്ഷിത സ്ഥാനത്തേക്ക് മാറുക.",
        "en" to "Flood waters rising rapidly. Evacuate immediately to high ground."
    )
    val packs = engine.packManager.availablePacks
    var language by remember { mutableStateOf("te") }
    var text by remember { mutableStateOf(sampleTexts["te"] ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var playingAlert by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Download a voice pack, then enter text.") }
    val logs by TtsTelemetry.lines.collectAsState()

    DisposableEffect(engine) {
        onDispose {
            player.acknowledgeAlert()
            player.stopNormal()
            CoroutineScope(Dispatchers.Default).launch { engine.destroy() }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Offline TTS demo")
        OutlinedButton(onClick = onBack) { Text("Back") }
        OutlinedButton(onClick = { expanded = true }) {
            Text(packs.first { it.languageCode == language }.displayName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            packs.forEach { pack ->
                DropdownMenuItem(text = { Text(pack.displayName) }, onClick = {
                    language = pack.languageCode
                    text = sampleTexts[pack.languageCode] ?: ""
                    expanded = false
                })
            }
        }
        Text(if (engine.packManager.isPackDownloaded(language)) "Pack ready" else "Pack needed")
        Button(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    engine.packManager.download(language)
                    status = "${language.uppercase()} pack ready"
                } catch (error: Throwable) { status = error.message ?: error.toString() }
                finally { busy = false }
            }
        }) { Text("Download voice pack") }
        OutlinedTextField(value = text, onValueChange = { text = it },
            label = { Text("Translated message to speak") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TtsMode.NORMAL, TtsMode.ALERT).forEach { mode ->
                Button(enabled = !busy && text.isNotBlank(), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            engine.init(language)
                            val result = engine.synthesize(text, mode)
                            status = "${result.synthesisTimeMs} ms · RTF ${"%.2f".format(result.realTimeFactor)}"
                            playingAlert = mode == TtsMode.ALERT
                            player.play(result)
                        } catch (error: Throwable) { status = error.message ?: error.toString() }
                        finally { playingAlert = false; busy = false }
                    }
                }) { Text(mode.name) }
            }
        }
        if (playingAlert) Button(onClick = { player.acknowledgeAlert() }) { Text("Acknowledge alert") }
        if (busy && !playingAlert) OutlinedButton(onClick = { player.stopNormal() }) { Text("Stop voice note") }
        Text(status)
        Text("TTS diagnostics")
        logs.forEach { Text(it) }
    }
}
