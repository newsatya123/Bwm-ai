package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.MainViewModel
import com.example.data.local.AssistantLogoTheme
import com.example.data.local.CustomCommandEntity
import com.example.ui.theme.CardBorderNavy
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
fun CustomCommandsSettingsContent(
    viewModel: MainViewModel,
    logoTheme: AssistantLogoTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customCommands by viewModel.customCommands.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for adding a command
    var triggerInput by remember { mutableStateOf("") }
    var alternativesInput by remember { mutableStateOf("") }
    var actionTypeSelection by remember { mutableStateOf("whatsapp_send") }
    var parameterInput by remember { mutableStateOf("") }
    var messagePayloadInput by remember { mutableStateOf("") }
    var requiresConfirmInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER & MANAGEMENT ACTIONS CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorderNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = logoTheme.primaryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Custom Voice Shortcuts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Create personal Hindi/English voice triggers for zero-latency automation", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            triggerInput = ""
                            alternativesInput = ""
                            actionTypeSelection = "whatsapp_send"
                            parameterInput = ""
                            messagePayloadInput = ""
                            requiresConfirmInput = false
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = logoTheme.primaryColor, contentColor = Color(0xFF001F25)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).testTag("add_custom_command_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Shortcut", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            exportedJson = viewModel.exportCustomCommandsJson()
                            showExportDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).testTag("export_commands_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            importInputText = ""
                            showImportDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RadiantEmerald),
                        border = BorderStroke(1.dp, RadiantEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).testTag("import_commands_btn")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 11.sp)
                    }
                }

                if (toastMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = RadiantEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(toastMessage ?: "", color = RadiantEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        // LIST OF CUSTOM COMMANDS
        if (customCommands.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Custom Voice Shortcuts Yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap '+ Add Shortcut' to create triggers like:\n• 'Mummy ko bolo' → WhatsApp send\n• 'Party music' → YouTube search\n• 'Instagram kholo' → Launch app",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            customCommands.forEach { cmd ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (cmd.isEnabled) CardBorderNavy else SurfaceVariantNavy),
                    modifier = Modifier.fillMaxWidth().testTag("custom_cmd_${cmd.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = when (cmd.actionType) {
                                        "whatsapp_send" -> RadiantEmerald.copy(alpha = 0.2f)
                                        "youtube_search" -> Color(0xFFFF4444).copy(alpha = 0.2f)
                                        else -> NeonCyan.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    val icon = when (cmd.actionType) {
                                        "whatsapp_send" -> Icons.AutoMirrored.Filled.Chat
                                        "youtube_search" -> Icons.Default.VideoLibrary
                                        else -> Icons.Default.Apps
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = when (cmd.actionType) {
                                            "whatsapp_send" -> RadiantEmerald
                                            "youtube_search" -> Color(0xFFFF5555)
                                            else -> NeonCyan
                                        },
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "\"${cmd.triggerPhrase}\"",
                                        color = if (cmd.isEnabled) TextPrimary else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (cmd.alternativePhrases.isNotBlank()) {
                                        Text(
                                            text = "Also: ${cmd.alternativePhrases}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = cmd.isEnabled,
                                onCheckedChange = { viewModel.toggleCustomCommand(cmd, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = RadiantEmerald),
                                modifier = Modifier.testTag("cmd_toggle_${cmd.id}")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val detailText = when (cmd.actionType) {
                            "whatsapp_send" -> "WhatsApp to ${cmd.actionParameter}: \"${cmd.messagePayload}\""
                            "launch_app" -> "Launch App: ${cmd.actionParameter}"
                            "youtube_search" -> "Play on YouTube: \"${cmd.actionParameter}\""
                            else -> "Action: ${cmd.actionType} (${cmd.actionParameter})"
                        }

                        Text(detailText, color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.testCustomCommand(cmd) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.height(30.dp).testTag("test_cmd_${cmd.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Now", fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.deleteCustomCommand(cmd) },
                                modifier = Modifier.size(30.dp).testTag("delete_cmd_${cmd.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD SHORTCUT DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SurfaceNavy,
            title = {
                Text("Add Custom Voice Shortcut", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Voice Trigger Phrase (e.g. 'mummy ko bolo')", color = TextSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = triggerInput,
                        onValueChange = { triggerInput = it },
                        singleLine = true,
                        placeholder = { Text("Enter voice trigger", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_cmd_trigger")
                    )

                    Text("Alternative Phrases (Optional, comma separated)", color = TextSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = alternativesInput,
                        onValueChange = { alternativesInput = it },
                        singleLine = true,
                        placeholder = { Text("e.g. mummy ko batao", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Action Type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "whatsapp_send" to "WhatsApp Message",
                        "launch_app" to "Launch Application",
                        "youtube_search" to "YouTube Search & Play"
                    ).forEach { (aType, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { actionTypeSelection = aType }
                        ) {
                            RadioButton(
                                selected = actionTypeSelection == aType,
                                onClick = { actionTypeSelection = aType },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    val paramLabel = when (actionTypeSelection) {
                        "whatsapp_send" -> "Recipient Name or Phone (e.g. Raman)"
                        "launch_app" -> "App Name (e.g. WhatsApp, Camera)"
                        else -> "Search Query (e.g. Arijit Singh songs)"
                    }
                    Text(paramLabel, color = TextSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = parameterInput,
                        onValueChange = { parameterInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_cmd_param")
                    )

                    if (actionTypeSelection == "whatsapp_send") {
                        Text("Pre-filled Message Content", color = TextSecondary, fontSize = 11.sp)
                        OutlinedTextField(
                            value = messagePayloadInput,
                            onValueChange = { messagePayloadInput = it },
                            placeholder = { Text("Message text to send", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_cmd_msg")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = requiresConfirmInput,
                            onCheckedChange = { requiresConfirmInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ask confirmation before executing", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (triggerInput.isNotBlank()) {
                            viewModel.addCustomCommand(
                                trigger = triggerInput,
                                alternatives = alternativesInput,
                                actionType = actionTypeSelection,
                                parameter = parameterInput,
                                message = messagePayloadInput,
                                requiresConfirmation = requiresConfirmInput
                            )
                            showAddDialog = false
                            toastMessage = "Custom shortcut added!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF001F25)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_new_cmd_dialog_btn")
                ) {
                    Text("Save Shortcut", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // EXPORT JSON DIALOG
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = SurfaceNavy,
            title = { Text("Export Custom Shortcuts JSON", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("You can backup or transfer your custom voice commands:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("BWM Commands", exportedJson)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                        toastMessage = "JSON copied to clipboard!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF001F25))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // IMPORT JSON DIALOG
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = SurfaceNavy,
            title = { Text("Import Custom Shortcuts JSON", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste previously exported JSON array here:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("[{\"trigger\": \"wife ko call\", ...}]", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariantNavy, unfocusedContainerColor = SurfaceVariantNavy,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = viewModel.importCustomCommandsJson(importInputText)
                        showImportDialog = false
                        toastMessage = if (count >= 0) "Imported $count commands successfully!" else "Failed to parse JSON!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald, contentColor = Color(0xFF001F25))
                ) {
                    Text("Import Now")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
