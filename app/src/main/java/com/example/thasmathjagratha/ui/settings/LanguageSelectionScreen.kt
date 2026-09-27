package com.example.thasmathjagratha.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel

data class IndianLanguage(
    val englishName: String,
    val nativeName: String,
    val region: String,
    val isSupported: Boolean = true
)

@Composable
fun LanguageSelectionScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()

    val languages = listOf(
        IndianLanguage("Telugu", "తెలుగు", "Andhra Pradesh & Telangana"),
        IndianLanguage("Hindi", "हिन्दी", "National / North India"),
        IndianLanguage("English", "English", "Pan India"),
        IndianLanguage("Tamil", "தமிழ்", "Tamil Nadu"),
        IndianLanguage("Kannada", "ಕನ್ನಡ", "Karnataka"),
        IndianLanguage("Malayalam", "മലയാളം", "Kerala"),
        IndianLanguage("Marathi", "मराठी", "Maharashtra"),
        IndianLanguage("Bengali", "বাংলা", "West Bengal"),
        IndianLanguage("Gujarati", "ગુજરાતી", "Gujarat"),
        IndianLanguage("Odia", "ଓଡ଼ିଆ", "Odisha • (Not supported yet)", isSupported = false)
    )

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Language Selection",
                subtitle = "Indian Speech-to-Text & TTS Preference",
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
                .padding(16.dp)
        ) {
            Text(
                text = "Select Preferred Indian Language",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "All voice synthesis (TTS) & speech recognition (STT) will adapt to this language.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(languages) { lang ->
                    val isSelected = user.preferredLanguage.equals(lang.englishName, ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!lang.isSupported) {
                                    viewModel.showSnackbar("${lang.englishName} is currently not supported for offline speech recognition / synthesis")
                                } else {
                                    viewModel.updatePreferredLanguage(lang.englishName)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                !lang.isSupported -> SurfaceWhite.copy(alpha = 0.5f)
                                isSelected -> NavyPrimary.copy(alpha = 0.08f)
                                else -> SurfaceWhite
                            }
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, NavyPrimary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) NavyPrimary else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lang.englishName} (${lang.nativeName})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = lang.region,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = AlertVerified,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Selection", fontWeight = FontWeight.Bold)
            }
        }
    }
}
