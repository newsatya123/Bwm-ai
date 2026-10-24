package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.local.CustomCommandEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.MainViewModel
import com.example.data.local.AssistantLogoTheme
import com.example.data.local.SttProvider
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
    initialTab: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantName by viewModel.assistantName.collectAsState()
    val logoTheme by viewModel.logoTheme.collectAsState()
    val isContinuousListening by viewModel.isContinuousListening.collectAsState()
    val autoBgListening by viewModel.autoBackgroundListening.collectAsState()
    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsState()
    val micSensitivity by viewModel.micSensitivity.collectAsState()
    val sttProvider by viewModel.sttProvider.collectAsState()
    val isWhatsappDirectSend by viewModel.isWhatsappDirectSend.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    val voiceGender by viewModel.voiceGender.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val spokenConfirmation by viewModel.spokenConfirmation.collectAsState()
    val isScreenReadingEnabled by viewModel.isScreenReadingEnabled.collectAsState()
    val whatsappAutoReadOutLoud by viewModel.whatsappAutoReadOutLoud.collectAsState()

    // AI Brain Architecture States
    val isAiBrainEnabled by viewModel.isAiBrainEnabled.collectAsState()
    val activeAiProvider by viewModel.activeAiProvider.collectAsState()
    val aiExecutionMode by viewModel.aiExecutionMode.collectAsState()
    val oxyyApiKey by viewModel.oxyyApiKey.collectAsState()
    val oxyyBaseUrl by viewModel.oxyyBaseUrl.collectAsState()
    val oxyyModelId by viewModel.oxyyModelId.collectAsState()
    val aiTemperature by viewModel.aiTemperature.collectAsState()
    val aiMaxTokens by viewModel.aiMaxTokens.collectAsState()
    val aiTimeoutSeconds by viewModel.aiTimeoutSeconds.collectAsState()
    val aiFallbackProvider by viewModel.aiFallbackProvider.collectAsState()
    val cachedAiModels by viewModel.cachedAiModels.collectAsState()
    val isRefreshingModels by viewModel.isRefreshingModels.collectAsState()
    val modelRefreshError by viewModel.modelRefreshError.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    // Custom Commands State
    val customCommands by viewModel.customCommands.collectAsState()

    var activeTab by remember(initialTab) {
        mutableStateOf(
            when (initialTab) {
                "voice" -> "Voice & Audio"
                "ai" -> "AI Brain"
                "commands" -> "Commands"
                "automation" -> "Automation"
                "privacy" -> "Privacy & Safety"
                "about" -> "About BWM"
                else -> "General"
            }
        )
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            activeTab = when (initialTab) {
                "voice" -> "Voice & Audio"
                "ai" -> "AI Brain"
                "commands" -> "Commands"
                "automation" -> "Automation"
                "privacy" -> "Privacy & Safety"
                "about" -> "About BWM"
                else -> "General"
            }
        }
    }

    var nameInput by remember(assistantName) { mutableStateOf(assistantName) }
    var nameSavedToast by remember { mutableStateOf(false) }

    // AI Brain local inputs
    var oxyyKeyInput by remember(oxyyApiKey) { mutableStateOf(oxyyApiKey) }
    var oxyyBaseUrlInput by remember(oxyyBaseUrl) { mutableStateOf(oxyyBaseUrl) }
    var oxyyModelInput by remember(oxyyModelId) { mutableStateOf(oxyyModelId) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var configSavedToast by remember { mutableStateOf(false) }

    // Custom Commands local inputs
    var showAddCommandDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var showExportJsonDialog by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }
    var importJsonContent by remember { mutableStateOf("") }
    var commandImportToast by remember { mutableStateOf<String?>(null) }

    var hasContactsPerm by remember { mutableStateOf(viewModel.contactsManager.hasContactsPermission()) }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPerm = isGranted
    }

    val hasGeminiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    val tabs = listOf("General", "Voice & Audio", "AI Brain", "Commands", "Automation", "Privacy & Safety", "About BWM")

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
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Assistant Settings & Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure voice engine, wake word, & AI model",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Settings Tabs Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                Surface(
                    color = if (isSelected) logoTheme.primaryColor.copy(alpha = 0.2f) else SurfaceNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) logoTheme.primaryColor else CardBorderNavy
                    ),
                    modifier = Modifier.clickable { activeTab = tab }
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) logoTheme.primaryColor else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tab Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                "General" -> {
                    // 1. WAKE WORD TOGGLE CARD (CLEAN RESPONSIVE LAYOUT - FIXES SCREENSHOT 2 BUG)
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
                                text = if (isWakeWordEnabled) {
                                    "Hands-free silent wake word engine active. Bolne par 'Hey $assistantName' ya 'Suno $assistantName' detect karke command capture start karega (Zero annoying beep sounds)."
                                } else {
                                    "Wake word detection is disabled. Tap orb or microphone manually to speak."
                                },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            if (isWakeWordEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = RadiantEmerald.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "⚡ Silent RMS Energy Detector",
                                            color = RadiantEmerald,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.triggerHeyBwmTest() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("test_wake_word_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Wake Trigger", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. MICROPHONE SENSITIVITY (FIXES SCREENSHOT 2 BUG - CLEAN M3 SLIDER)
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
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Microphone Sensitivity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                Surface(
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${(micSensitivity * 100).toInt()}%",
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val sensitivityLabel = when {
                                micSensitivity <= 0.25f -> "Low Sensitivity — Best for noisy / crowded rooms (requires clear, loud voice)"
                                micSensitivity <= 0.55f -> "Balanced (Default) — Standard room environment, natural speech volume"
                                micSensitivity <= 0.8f -> "High Sensitivity — Quiet room, sensitive to normal speech"
                                else -> "Ultra Sensitive — Detects soft whispers & distance voice"
                            }
                            Text(
                                text = sensitivityLabel,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Slider(
                                value = micSensitivity,
                                onValueChange = { viewModel.setMicSensitivity(it) },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = SurfaceVariantNavy
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mic_sensitivity_slider")
                            )
                        }
                    }

                    // 3. ASSISTANT NAME CUSTOMIZATION
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = logoTheme.primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Assistant Name & Wake Word Phrase", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Customize your assistant's call sign. e.g. BWM, Jarvis, Friday, Arya.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceVariantNavy,
                                        unfocusedContainerColor = SurfaceVariantNavy,
                                        focusedBorderColor = logoTheme.primaryColor,
                                        unfocusedBorderColor = CardBorderNavy,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("assistant_name_input")
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateAssistantName(nameInput)
                                        nameSavedToast = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = logoTheme.primaryColor,
                                        contentColor = Color(0xFF001F25)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("save_assistant_name_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. LOGO & VISUAL THEMES
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
                                Text("Futuristic Glowing Orb Themes", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            AssistantLogoTheme.entries.forEach { theme ->
                                val isSelected = logoTheme == theme
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) theme.primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { viewModel.updateLogoTheme(theme) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(theme.primaryColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(theme.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(theme.description, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor)
                                    }
                                }
                            }
                        }
                    }
                }

                "Voice & Audio" -> {
                    // MALE / FEMALE VOICE CHOICES & PREVIEW
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Voice Synthesis & Persona", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select your preferred assistant voice profile. Includes a resonant Jarvis-style male acoustic persona.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Gender Options
                            listOf(
                                Triple("male", "Jarvis Male (Calm, Confident)", "Resonant, low-pitch authoritative assistant voice"),
                                Triple("female", "Female Voice (Clear, Friendly)", "Natural melodious speech tone"),
                                Triple("default", "System Default Voice", "Standard installed Android OS voice")
                            ).forEach { (id, title, desc) ->
                                val isSelected = voiceGender.equals(id, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { viewModel.setVoiceGender(id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setVoiceGender(id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(desc, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.previewVoice() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF001F25)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("preview_voice_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Voice (\"Sir, BWM Jarvis voice synthesis...\")", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // SPEECH RATE & PITCH SLIDERS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = RadiantEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Speech Rate & Acoustic Pitch", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Speech Rate: ${String.format("%.2f", speechRate)}x", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = speechRate,
                                onValueChange = { viewModel.setSpeechRate(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = RadiantEmerald, activeTrackColor = RadiantEmerald)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Acoustic Pitch: ${String.format("%.2f", speechPitch)}x", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = speechPitch,
                                onValueChange = { viewModel.setSpeechPitch(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = RadiantEmerald, activeTrackColor = RadiantEmerald)
                            )
                        }
                    }
                }

                "AI Brain" -> {
                    AiBrainSettingsContent(
                        viewModel = viewModel,
                        logoTheme = logoTheme
                    )
                }

                "Commands" -> {
                    CustomCommandsSettingsContent(
                        viewModel = viewModel,
                        logoTheme = logoTheme
                    )
                }

                "Automation" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = RadiantEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WhatsApp & Screen Reading Automation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Screen Reading Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Screen Reading & Awareness Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Allow assistant to read visible screen text, analyze web pages, and explain articles.", color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isScreenReadingEnabled,
                                    onCheckedChange = { viewModel.setScreenReadingEnabled(it) },
                                    modifier = Modifier.testTag("screen_reading_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // WhatsApp Auto Read Out Loud Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("WhatsApp Voice Read Out Loud", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Speak last visible WhatsApp message aloud via Jarvis TTS when requested.", color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = whatsappAutoReadOutLoud,
                                    onCheckedChange = { viewModel.setWhatsappAutoReadOutLoud(it) },
                                    modifier = Modifier.testTag("whatsapp_read_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Direct WhatsApp Send (No Dialog)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Send message directly without confirmation dialog pop-up.", color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isWhatsappDirectSend,
                                    onCheckedChange = { viewModel.setWhatsappDirectSend(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!hasContactsPerm) {
                                Button(
                                    onClick = { contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald, contentColor = Color(0xFF001F25)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Grant Contacts Permission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Surface(
                                    color = RadiantEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("✓ Contacts Permission Granted", color = RadiantEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }

                "Privacy & Safety" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Privacy & Security Guarantee", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Zero Audio Upload: Continuous wake word runs 100% on-device using local RMS energy detection.\n\n" +
                                        "2. No Ambient Recording: Voice recognizer only starts after 'Hey BWM' trigger or manual orb tap.\n\n" +
                                        "3. Screen Awareness Privacy: Screen reading operates purely on-demand via AccessibilityService / OCR when you issue a read or explain command. No background screenshots or continuous screen monitoring are stored.\n\n" +
                                        "4. WhatsApp Message Privacy: Chat reading only inspects the active conversation that you explicitly asked for. It never bypasses encryption or reads background chats.\n\n" +
                                        "5. Verified Confirmation: Calling and WhatsApp messages require user confirmation before execution by default.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                "About BWM" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("BWM AI Assistant v1.6.0 (Oxyy AI + Offline Jarvis)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Jarvis-inspired autonomous Android voice assistant with Screen Awareness, WhatsApp Chat Reading, Webpage Summaries, PDF Reading, on-device OCR, multi-task queue, and zero-beep wake-word activation.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Package: com.aistudio.bwmai.vxnpza", color = TextMuted, fontSize = 11.sp)
                            Text("Engine: Native Kotlin + Jetpack Compose M3", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
