package com.example.thasmathjagratha.ui.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertCriticalBackground
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

data class IndianLang(val name: String, val nativeName: String, val isSupported: Boolean = true)

@Composable
fun PreferredLanguageScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val activeAlert = alerts.firstOrNull()

    val languages = listOf(
        IndianLang("Telugu", "తెలుగు"),
        IndianLang("Hindi", "हिन्दी"),
        IndianLang("English", "English"),
        IndianLang("Tamil", "தமிழ்"),
        IndianLang("Kannada", "ಕನ್ನಡ"),
        IndianLang("Malayalam", "മലയാളം"),
        IndianLang("Marathi", "मराठी"),
        IndianLang("Bengali", "বাংলা"),
        IndianLang("Gujarati", "ગુજરાતી"),
        IndianLang("Odia", "ଓଡ଼ିଆ • (Not supported yet)", isSupported = false)
    )

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Language Conversion",
                subtitle = "Preferred Indian Speech & Text Engine",
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
            // Live Translation Preview Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Alert Conversion Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Original Alert (${activeAlert?.originalLanguage ?: "English"}):", fontSize = 11.sp, color = TextSecondary)
                    Text("“${activeAlert?.message ?: "Move away from low-lying areas immediately."}”", fontSize = 13.sp, color = TextPrimary)

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AlertCriticalBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Converted Alert (${user.preferredLanguage}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertCritical)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("“${activeAlert?.translatedMessage ?: "తక్కువ ప్రాంతాల నుండి వెంటనే సురక్షిత ప్రాంతాలకు వెళ్లండి."}”", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.playTts(activeAlert?.translatedMessage ?: "", user.preferredLanguage) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Audio (TTS)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Select Alert Language", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            languages.forEach { lang ->
                val isSelected = user.preferredLanguage.equals(lang.name, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (!lang.isSupported) {
                                viewModel.showSnackbar("${lang.name} is currently not supported for offline speech recognition / synthesis")
                            } else {
                                viewModel.updatePreferredLanguage(lang.name)
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            !lang.isSupported -> SurfaceWhite.copy(alpha = 0.5f)
                            isSelected -> NavyPrimary.copy(alpha = 0.08f)
                            else -> SurfaceWhite
                        }
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, NavyPrimary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) NavyPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${lang.name} (${lang.nativeName})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
