package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.MainViewModel
import com.example.service.VoiceAssistantService
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.DeepNavyDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.SurfaceVariantNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val isContinuous by viewModel.isContinuousListening.collectAsState()
    val isWakeWord by viewModel.isWakeWordEnabled.collectAsState()
    val isServiceRunning by VoiceAssistantService.isServiceRunning.collectAsState()

    var speechRate by remember { mutableFloatStateOf(viewModel.ttsManager.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(viewModel.ttsManager.speechPitch) }
    var isTtsEnabled by remember { mutableStateOf(viewModel.ttsManager.isTtsEnabled) }

    val hasGeminiApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Assistant Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure voice, Gemini AI, and background options",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. VOICE LANGUAGE SELECTION ---
        SectionHeader("Voice & Language")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Default Language", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf(
                        "hi-IN" to "🇮🇳 Hindi",
                        "en-IN" to "🇮🇳 Hinglish",
                        "en-US" to "🇺🇸 English"
                    )

                    languages.forEach { (code, label) ->
                        val isSelected = selectedLang == code
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceVariantNavy,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else CardBorderNavy
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_select_$code"),
                            onClick = { viewModel.selectedLanguage.value = code }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. LISTENING MODES ---
        SectionHeader("Background & Listening Modes")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Background Listening Foreground Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Listening Service", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text(
                            "Runs an ongoing foreground service with microphone notification to hear commands while using other apps.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleBackgroundService() },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = SurfaceVariantNavy),
                        modifier = Modifier.testTag("bg_service_switch")
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = CardBorderNavy)

                // Continuous Listening Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Conversation", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text(
                            "Automatically re-arms microphone after speaking for follow-up commands.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isContinuous,
                        onCheckedChange = { viewModel.isContinuousListening.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = SurfaceVariantNavy),
                        modifier = Modifier.testTag("continuous_mode_switch")
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = CardBorderNavy)

                // Wake Word Detector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word: 'Hey BWM'", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(
                                "Activates assistant hands-free when you say 'Hey BWM', 'Hi BWM', 'OK BWM', or 'Suno BWM'.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isWakeWord,
                            onCheckedChange = { viewModel.toggleWakeWord(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = SurfaceVariantNavy),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    if (isWakeWord) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.triggerHeyBwmTest() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RadiantEmerald),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RadiantEmerald.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_wake_word_btn")
                        ) {
                            Text("⚡ Test 'Hey BWM' Trigger Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Tip: You can say 'Hey BWM' alone to wake the assistant, or combine it with a command like 'Hey BWM, flashlight on karo' to execute instantly!",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. TEXT TO SPEECH (TTS) ---
        SectionHeader("Text-To-Speech (Spoken Feedback)")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spoken Voice Responses", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = {
                            isTtsEnabled = it
                            viewModel.ttsManager.isTtsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = SurfaceVariantNavy)
                    )
                }

                Text("Speech Rate: ${String.format("%.1fx", speechRate)}", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        viewModel.ttsManager.speechRate = it
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Text("Pitch: ${String.format("%.1fx", speechPitch)}", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        viewModel.ttsManager.speechPitch = it
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                )

                OutlinedButton(
                    onClick = {
                        viewModel.ttsManager.speak("Namaste! BWM AI voice output is working perfectly.")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Test Voice Output")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. GEMINI AI ENGINE STATUS ---
        SectionHeader("Gemini AI Engine")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Model: gemini-3.5-flash", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    }

                    Surface(
                        color = if (hasGeminiApiKey) RadiantEmerald.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (hasGeminiApiKey) "CLOUD ACTIVE" else "HYBRID LOCAL NLP",
                            color = if (hasGeminiApiKey) RadiantEmerald else ElectricViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (hasGeminiApiKey) {
                        "Gemini API key is active. Advanced function calling, tool execution, and natural dialogue reasoning are operational."
                    } else {
                        "Operating in Hybrid Local NLP Mode. Real phone controls (flashlight, volume, battery, timers, alarms, apps) work completely offline!"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. DATA PRIVACY & KILL SWITCH ---
        SectionHeader("Data & Kill Switch")
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Device Memory Wipe", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Instantly delete all stored conversations, voice commands, and action logs from device memory.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = DeepNavyDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("kill_switch_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Clear All Local History", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = NeonCyan,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
