package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AssistantUiState
import com.example.MainViewModel
import com.example.data.local.ConversationMessage
import com.example.service.VoiceAssistantService
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.QuickActionChips
import com.example.ui.components.WaveformVisualizer
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
import com.example.ui.theme.WarningAmber

@Composable
fun AssistantMainScreen(
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val interimText by viewModel.currentInterimText.collectAsState()
    val statusMessage by viewModel.currentStatusMessage.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val isServiceRunning by VoiceAssistantService.isServiceRunning.collectAsState()
    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()
    val logoTheme by viewModel.logoTheme.collectAsState()
    val isContinuousListening by viewModel.isContinuousListening.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, interimText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    pendingConfirmation?.let { conf ->
        AlertDialog(
            onDismissRequest = conf.onCancel,
            title = { Text(conf.title, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(conf.message, color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = conf.onConfirm,
                    modifier = Modifier.testTag("confirm_action_btn")
                ) {
                    Text("Confirm", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = conf.onCancel,
                    modifier = Modifier.testTag("cancel_action_btn")
                ) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyDark)
    ) {
        // --- TOP APP BAR (CLEAN & MINIMAL HEADER) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning || isWakeWordEnabled) RadiantEmerald else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = assistantName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = logoTheme.primaryColor
                    )
                }
                Text(
                    text = "Context-Aware Voice Assistant",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language Selector Pill
                Surface(
                    color = SurfaceVariantNavy,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                    modifier = Modifier
                        .clickable {
                            val next = when (selectedLang) {
                                "en-IN" -> "hi-IN"
                                "hi-IN" -> "en-US"
                                else -> "en-IN"
                            }
                            viewModel.selectedLanguage.value = next
                        }
                        .testTag("lang_toggle_pill")
                ) {
                    Text(
                        text = when (selectedLang) {
                            "hi-IN" -> "🇮🇳 हिन्दी"
                            "en-IN" -> "🇮🇳 Hinglish"
                            else -> "🇺🇸 English"
                        },
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Wake Word State Badge
                Surface(
                    color = if (isWakeWordEnabled) RadiantEmerald.copy(alpha = 0.15f) else SurfaceVariantNavy,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isWakeWordEnabled) RadiantEmerald.copy(alpha = 0.6f) else CardBorderNavy
                    ),
                    modifier = Modifier
                        .clickable { viewModel.toggleWakeWord(!isWakeWordEnabled) }
                        .testTag("wake_word_top_badge")
                ) {
                    Text(
                        text = if (isWakeWordEnabled) "⚡ Hey $assistantName" else "⚡ Wake OFF",
                        color = if (isWakeWordEnabled) RadiantEmerald else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- CENTER VOICE HUB ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OrbVisualizer(
                state = uiState,
                rmsDb = audioRmsDb,
                size = 140.dp,
                logoTheme = logoTheme,
                onClick = {
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.toggleVoiceListening()
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = when (uiState) {
                    AssistantUiState.CAPTURING_COMMAND, AssistantUiState.LISTENING -> RadiantEmerald.copy(alpha = 0.15f)
                    AssistantUiState.LISTENING_FOR_WAKE_WORD -> NeonCyan.copy(alpha = 0.12f)
                    AssistantUiState.PROCESSING -> ElectricViolet.copy(alpha = 0.15f)
                    AssistantUiState.EXECUTING -> WarningAmber.copy(alpha = 0.15f)
                    AssistantUiState.ERROR -> ErrorRed.copy(alpha = 0.15f)
                    else -> SurfaceVariantNavy
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (uiState) {
                        AssistantUiState.CAPTURING_COMMAND, AssistantUiState.LISTENING -> RadiantEmerald
                        AssistantUiState.LISTENING_FOR_WAKE_WORD -> NeonCyan.copy(alpha = 0.5f)
                        AssistantUiState.PROCESSING -> ElectricViolet
                        AssistantUiState.EXECUTING -> WarningAmber
                        AssistantUiState.ERROR -> ErrorRed
                        else -> CardBorderNavy
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusDotColor = when (uiState) {
                        AssistantUiState.CAPTURING_COMMAND, AssistantUiState.LISTENING -> RadiantEmerald
                        AssistantUiState.LISTENING_FOR_WAKE_WORD -> NeonCyan
                        AssistantUiState.PROCESSING -> ElectricViolet
                        AssistantUiState.EXECUTING -> WarningAmber
                        AssistantUiState.SPEAKING -> NeonCyan
                        AssistantUiState.ERROR -> ErrorRed
                        AssistantUiState.IDLE -> NeonCyan.copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = interimText.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "“$interimText”",
                    color = RadiantEmerald,
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                )
            }

            WaveformVisualizer(
                rmsDb = audioRmsDb,
                isListening = uiState == AssistantUiState.LISTENING,
                isSpeaking = isSpeaking,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            if (isWakeWordEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.triggerHeyBwmTest() }
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("wake_word_quick_trigger")
                ) {
                    Text(
                        text = "⚡ Say \"Hey BWM\" anytime or tap to test",
                        fontSize = 11.sp,
                        color = RadiantEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- QUICK ACTION CHIPS ---
        QuickActionChips(
            onActionClick = { cmd ->
                viewModel.handleUserInput(cmd)
            }
        )

        // --- CONVERSATION TRANSCRIPT ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Namaste! Main aapka BWM AI assistant hoon.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the glowing Orb or Mic button below to speak, or tap a quick action like 'Torch On', 'Battery', or 'Open WhatsApp'.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ConversationBubble(msg)
            }
        }

        // --- BOTTOM VOICE & INPUT BAR ---
        Surface(
            color = SurfaceNavy,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { viewModel.toggleVoiceListening() },
                    containerColor = if (uiState.isListening) RadiantEmerald else NeonCyan,
                    contentColor = Color(0xFF001F25),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("voice_input_fab")
                ) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone Button",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask or command BWM AI...", color = TextMuted, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderNavy,
                        focusedContainerColor = SurfaceVariantNavy,
                        unfocusedContainerColor = SurfaceVariantNavy
                    ),
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val query = textInput
                                    textInput = ""
                                    viewModel.handleUserInput(query)
                                },
                                modifier = Modifier.testTag("send_text_command_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonCyan)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_command_input")
                )

                if (isSpeaking) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.testTag("stop_speaking_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Stop Speech", tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(msg: ConversationMessage) {
    val isUser = msg.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) SurfaceVariantNavy else SurfaceNavy
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) ElectricViolet.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUser) "You" else "BWM Assistant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) ElectricViolet else NeonCyan
                    )

                    if (msg.actionType != null) {
                        Surface(
                            color = if (msg.isActionSuccess == true) RadiantEmerald.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = msg.actionType,
                                fontSize = 10.sp,
                                color = if (msg.isActionSuccess == true) RadiantEmerald else ErrorRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.content,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
        }
    }
}
