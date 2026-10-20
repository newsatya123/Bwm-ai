package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onOpenDrawer: () -> Unit,
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
    val pendingContactChoice by viewModel.pendingContactChoice.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()
    val logoTheme by viewModel.logoTheme.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, interimText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Confirmation Dialog for critical actions (Calling / WhatsApp)
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

    // Contact Disambiguation Dialog (Multiple matching contacts)
    pendingContactChoice?.let { choice ->
        AlertDialog(
            onDismissRequest = choice.onDismiss,
            title = {
                Text(
                    text = choice.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = choice.prompt,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    choice.contacts.forEach { contact ->
                        Surface(
                            color = SurfaceVariantNavy,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { choice.onContactChosen(contact) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(logoTheme.primaryColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = logoTheme.primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = contact.getMaskedNumber(),
                                        color = NeonCyan,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = choice.onDismiss,
                    modifier = Modifier.testTag("dismiss_contact_disambiguation_btn")
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
        // --- TOP APP BAR (CLEAN, PROPERLY ALIGNED HEADER - NO OVERSIZED VERTICAL BAR) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Hamburger Menu Icon + Assistant Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("open_navigation_drawer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = logoTheme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning || isWakeWordEnabled) RadiantEmerald else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assistantName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = logoTheme.primaryColor
                        )
                    }
                    Text(
                        text = "Jarvis Autonomous Assistant",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right: Language Selector Pill & Compact Wake Word Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SurfaceVariantNavy,
                    shape = RoundedCornerShape(10.dp),
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
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Compact status dot indicating wake-word engine state
                Surface(
                    color = if (isWakeWordEnabled) RadiantEmerald.copy(alpha = 0.15f) else SurfaceVariantNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isWakeWordEnabled) RadiantEmerald.copy(alpha = 0.5f) else CardBorderNavy
                    ),
                    modifier = Modifier
                        .clickable { viewModel.toggleWakeWord(!isWakeWordEnabled) }
                        .testTag("wake_word_compact_indicator")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isWakeWordEnabled) "⚡ Hey $assistantName" else "⚡ Wake OFF",
                            color = if (isWakeWordEnabled) RadiantEmerald else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- CENTER VOICE HUB (GLOWING ORB + STATUS) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OrbVisualizer(
                state = uiState,
                rmsDb = audioRmsDb,
                size = 135.dp,
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

            // Real-Time Waveform or Status Message Pill
            if (uiState.isListening) {
                WaveformVisualizer(
                    rmsDb = audioRmsDb,
                    isListening = true,
                    isSpeaking = isSpeaking,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(30.dp)
                )
            } else {
                Surface(
                    color = SurfaceVariantNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState) {
                                        AssistantUiState.LISTENING_FOR_WAKE_WORD -> NeonCyan
                                        AssistantUiState.CAPTURING_COMMAND, AssistantUiState.LISTENING -> RadiantEmerald
                                        AssistantUiState.PROCESSING -> ElectricViolet
                                        AssistantUiState.EXECUTING -> WarningAmber
                                        AssistantUiState.WAITING_FOR_CONFIRMATION -> WarningAmber
                                        AssistantUiState.SPEAKING -> NeonCyan
                                        AssistantUiState.ERROR -> ErrorRed
                                        AssistantUiState.IDLE -> TextMuted
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                interimText.isNotBlank() -> interimText
                                isSpeaking -> "Speaking: $statusMessage"
                                uiState == AssistantUiState.WAITING_FOR_CONFIRMATION -> "Waiting for confirmation / selection..."
                                uiState == AssistantUiState.PROCESSING -> "Jarvis thinking..."
                                uiState == AssistantUiState.EXECUTING -> "Executing requested task..."
                                uiState == AssistantUiState.LISTENING_FOR_WAKE_WORD -> "Listening for 'Hey $assistantName'..."
                                else -> statusMessage
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.isListening) {
                            IconButton(
                                onClick = { viewModel.toggleVoiceListening() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Wake Word Test Hint
            Text(
                text = "⚡ Say \"Hey $assistantName\" anytime or tap orb to speak",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        // --- CONVERSATION & TASK LOGS STREAM ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(messages) { msg ->
                ChatMessageItem(message = msg, logoTheme = logoTheme)
            }
        }

        // Quick action chips (WhatsApp, YouTube, Flashlight, Alarms)
        QuickActionChips(
            onActionClick = { cmd -> viewModel.handleUserInput(cmd) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // --- BOTTOM VOICE INPUT BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Microphone FAB
            FloatingActionButton(
                onClick = {
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.toggleVoiceListening()
                    }
                },
                containerColor = if (uiState.isListening) RadiantEmerald else logoTheme.primaryColor,
                contentColor = Color(0xFF001F25),
                elevation = FloatingActionButtonDefaults.elevation(2.dp),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_input_fab")
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone Button",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Input Field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Ask or command $assistantName...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = textInput.trim()
                                textInput = ""
                                viewModel.handleUserInput(text)
                            },
                            modifier = Modifier.testTag("send_command_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = logoTheme.primaryColor
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceNavy,
                    unfocusedContainerColor = SurfaceNavy,
                    focusedBorderColor = logoTheme.primaryColor,
                    unfocusedBorderColor = CardBorderNavy,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("user_text_input")
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ConversationMessage,
    logoTheme: com.example.data.local.AssistantLogoTheme
) {
    val isUser = message.role.equals("user", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) SurfaceVariantNavy else SurfaceNavy
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) logoTheme.primaryColor.copy(alpha = 0.4f) else CardBorderNavy
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "You" else "BWM Jarvis",
                    color = if (isUser) logoTheme.primaryColor else RadiantEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
