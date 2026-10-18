package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.MainViewModel
import com.example.data.local.AssistantLogoTheme
import com.example.ui.components.OrbVisualizer
import com.example.AssistantUiState
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.DeepNavyDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.SurfaceVariantNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantName by viewModel.assistantName.collectAsState()
    val logoTheme by viewModel.logoTheme.collectAsState()
    val isContinuousListening by viewModel.isContinuousListening.collectAsState()
    val autoBgListening by viewModel.autoBackgroundListening.collectAsState()
    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    var nameInput by remember(assistantName) { mutableStateOf(assistantName) }
    var nameSavedToast by remember { mutableStateOf(false) }

    var speechRate by remember { mutableFloatStateOf(viewModel.ttsManager.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(viewModel.ttsManager.speechPitch) }
    var ttsEnabled by remember { mutableFloatStateOf(if (viewModel.ttsManager.isTtsEnabled) 1f else 0f) }

    val hasGeminiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyDark)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = logoTheme.primaryColor
                )
            }
            Text(
                text = "Assistant Settings & Customization",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 1. ASSISTANT NAME CUSTOMIZATION
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = logoTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistant Name", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Customize the name of your assistant (e.g. BWM AI, Jarvis, Arya, Nova). The app UI and wake word will adapt to this name.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                nameSavedToast = false
                            },
                            singleLine = true,
                            placeholder = { Text("Enter name e.g. BWM AI", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = logoTheme.primaryColor,
                                unfocusedBorderColor = CardBorderNavy,
                                focusedContainerColor = SurfaceVariantNavy,
                                unfocusedContainerColor = SurfaceVariantNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("assistant_name_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                viewModel.updateAssistantName(nameInput)
                                nameSavedToast = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = logoTheme.primaryColor,
                                contentColor = DeepNavyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_assistant_name_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (nameSavedToast) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "✓ Assistant name updated to \"$assistantName\"!",
                            color = RadiantEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. ASSISTANT LOGO & ORB THEME CUSTOMIZATION
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = logoTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistant Logo & Orb Theme", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose your assistant's glowing visual theme and avatar identity:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Orb Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantNavy, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrbVisualizer(
                            state = AssistantUiState.IDLE,
                            rmsDb = 0f,
                            size = 72.dp,
                            logoTheme = logoTheme
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = logoTheme.displayName,
                                color = logoTheme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = logoTheme.description,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme selector buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AssistantLogoTheme.entries.forEach { theme ->
                            val isSelected = theme == logoTheme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SurfaceVariantNavy else SurfaceNavy)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) theme.primaryColor else CardBorderNavy,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateLogoTheme(theme) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("theme_btn_${theme.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(theme.primaryColor, theme.secondaryColor)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DeepNavyDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = theme.displayName.split(" ").first(),
                                    color = if (isSelected) theme.primaryColor else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 3. CONTINUOUS LISTENING MODE (Requirement 2)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isContinuousListening) RadiantEmerald.copy(alpha = 0.5f) else CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hearing, contentDescription = null, tint = RadiantEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continuous Listening Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = isContinuousListening,
                            onCheckedChange = { viewModel.setContinuousListening(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyDark,
                                checkedTrackColor = RadiantEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceVariantNavy
                            ),
                            modifier = Modifier.testTag("continuous_listening_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jab aap ek bar mic on karte hain, toh command poori hone ke baad bhi assistant lagatar sunta rahega bina baar-baar mic dabaye.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 4. LISTEN OVER YOUTUBE & OTHER APPS (Requirement 3)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (autoBgListening) NeonCyan.copy(alpha = 0.4f) else CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listen Over YouTube & Other Apps", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = autoBgListening,
                            onCheckedChange = { viewModel.setAutoBackgroundListening(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceVariantNavy
                            ),
                            modifier = Modifier.testTag("auto_bg_listening_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jab aap YouTube ya kisi doosri app ko command se open karenge, tab background microphone service automatically active rahegi jisse aap app ke upar se 'Hey BWM' bol kar ya notification se commands chala sakein.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 5. MULTI-TASKING CAPABILITY EXPLANATION (Requirement 1)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Multi-Tasking Engine (Active)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aap ek saath multiple commands jod kar bol sakte hain. Assistant sabhi actions ko ek ke baad ek execute karega:\n\n• \"Turn on flashlight and open youtube\"\n• \"Torch on karo aur youtube chalao\"\n• \"Volume badhao aur camera open karo\"",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 6. WAKE WORD SETTINGS
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = logoTheme.primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wake Word (\"Hey $assistantName\")", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = isWakeWordEnabled,
                            onCheckedChange = { viewModel.toggleWakeWord(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyDark,
                                checkedTrackColor = RadiantEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceVariantNavy
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows hands-free trigger when saying \"Hey $assistantName\" or \"Suno $assistantName\" anytime within the app.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 7. LANGUAGE SELECTOR
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = logoTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speech Recognition Language", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val languages = listOf(
                        "en-IN" to "Hinglish / Indian English (en-IN)",
                        "hi-IN" to "Hindi (हिन्दी) (hi-IN)",
                        "en-US" to "English (US) (en-US)"
                    )

                    languages.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedLang == code,
                                onClick = { viewModel.selectedLanguage.value = code },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = logoTheme.primaryColor,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 8. TEXT-TO-SPEECH
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = logoTheme.primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice Output (TTS)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = ttsEnabled == 1f,
                            onCheckedChange = {
                                val enabled = it
                                ttsEnabled = if (enabled) 1f else 0f
                                viewModel.ttsManager.isTtsEnabled = enabled
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyDark,
                                checkedTrackColor = RadiantEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceVariantNavy
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Speech Rate: ${String.format("%.1f", speechRate)}x", color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            viewModel.ttsManager.speechRate = it
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = logoTheme.primaryColor, activeTrackColor = logoTheme.primaryColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Speech Pitch: ${String.format("%.1f", speechPitch)}x", color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            viewModel.ttsManager.speechPitch = it
                        },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = logoTheme.secondaryColor, activeTrackColor = logoTheme.secondaryColor)
                    )
                }
            }

            // 9. AI ENGINE STATUS
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Reasoning Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            text = if (hasGeminiKey) "GEMINI PRO / FLASH" else "HYBRID OFFLINE NLP",
                            color = if (hasGeminiKey) RadiantEmerald else logoTheme.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasGeminiKey) {
                            "Gemini API key is active. Full natural language intelligence and dynamic multi-step actions are enabled."
                        } else {
                            "Built-in offline Hindi & English intent parser is active with multi-tasking capability. To connect Google Gemini API, add your key to the AI Studio Secrets panel."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 10. ABOUT APP
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = logoTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About $assistantName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$assistantName — Personal Phone Assistant", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Version 1.1.0 • Multi-Tasking & Continuous Listening Enabled", color = TextMuted, fontSize = 12.sp)
                    Text("Native Android Assistant • Jetpack Compose • Kotlin", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
