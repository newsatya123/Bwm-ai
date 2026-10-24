package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.MainViewModel
import com.example.data.local.AssistantLogoTheme
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
fun AiBrainSettingsContent(
    viewModel: MainViewModel,
    logoTheme: AssistantLogoTheme,
    modifier: Modifier = Modifier
) {
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

    var oxyyKeyInput by remember(oxyyApiKey) { mutableStateOf(oxyyApiKey) }
    var oxyyBaseUrlInput by remember(oxyyBaseUrl) { mutableStateOf(oxyyBaseUrl) }
    var oxyyModelInput by remember(oxyyModelId) { mutableStateOf(oxyyModelId) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var configSavedSuccess by remember { mutableStateOf(false) }

    val hasGeminiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. MASTER AI BRAIN & EXECUTION MODE CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorderNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = logoTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("AI Brain Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Hybrid Intelligence (Oxyy + Gemini + Local Jarvis)", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = isAiBrainEnabled,
                        onCheckedChange = { viewModel.setAiBrainEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("ai_brain_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Operational Execution Mode", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Triple("auto", "Automatic Hybrid Mode (Recommended)", "Offline NLP executes phone commands instantly; Cloud AI reasons on questions."),
                    Triple("online_only", "Online Cloud Only", "Always queries the active cloud model. Requires active internet."),
                    Triple("offline_only", "Offline Local Only", "100% on-device Jarvis engine. Zero data upload, ultra-fast response.")
                ).forEach { (modeKey, title, desc) ->
                    val isSelected = aiExecutionMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) logoTheme.primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { viewModel.setAiExecutionMode(modeKey) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setAiExecutionMode(modeKey) },
                            colors = RadioButtonDefaults.colors(selectedColor = logoTheme.primaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(desc, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. ACTIVE AI PROVIDER SELECTOR CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorderNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeviceHub, contentDescription = null, tint = ElectricViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Intelligence Provider", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Select which AI service powers conversation and command planning.", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    Triple("oxyy", "Oxyy AI (Official Cloud API)", "https://api.oxyy.ai/v1 • High performance OpenAI-compatible"),
                    Triple("gemini", "Google Gemini AI", "Built-in Gemini reasoning & multimodal synthesis"),
                    Triple("offline", "On-Device Offline NLP", "Local rule-based pattern engine running directly in RAM")
                ).forEach { (pKey, title, desc) ->
                    val isSelected = activeAiProvider == pKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else SurfaceVariantNavy)
                            .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setActiveAiProvider(pKey) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setActiveAiProvider(pKey) },
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
                Text("Fallback Secondary Provider", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("offline" to "Offline NLP", "gemini" to "Gemini", "none" to "None").forEach { (fKey, label) ->
                        val isSelected = aiFallbackProvider == fKey
                        OutlinedButton(
                            onClick = { viewModel.setAiFallbackProvider(fKey) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) RadiantEmerald.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isSelected) RadiantEmerald else TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) RadiantEmerald else CardBorderNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // 3. OXYY AI API CONFIGURATION CARD (Detailed official integration)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (activeAiProvider == "oxyy") NeonCyan.copy(alpha = 0.5f) else CardBorderNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Oxyy AI Configuration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Official documentation: https://oxyy.ai/docs • Base: api.oxyy.ai/v1", color = TextSecondary, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(14.dp))

                // API Base URL
                Text("API Base URL", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = oxyyBaseUrlInput,
                    onValueChange = { oxyyBaseUrlInput = it },
                    singleLine = true,
                    placeholder = { Text("https://api.oxyy.ai/v1", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantNavy,
                        unfocusedContainerColor = SurfaceVariantNavy,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderNavy,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (oxyyBaseUrlInput != "https://api.oxyy.ai/v1") {
                            IconButton(onClick = { oxyyBaseUrlInput = "https://api.oxyy.ai/v1" }) {
                                Text("Reset", color = NeonCyan, fontSize = 11.sp)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("oxyy_base_url_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // API Key input with password masking
                Text("API Key (Bearer Token)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = oxyyKeyInput,
                    onValueChange = {
                        oxyyKeyInput = it
                        configSavedSuccess = false
                    },
                    singleLine = true,
                    placeholder = { Text("Enter your Oxyy API key", color = TextMuted) },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantNavy,
                        unfocusedContainerColor = SurfaceVariantNavy,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderNavy,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isApiKeyVisible) "Hide Key" else "Show Key",
                                tint = NeonCyan
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("oxyy_api_key_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Model ID Selector + Refresh Models
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Model ID", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = { viewModel.refreshAiModels() },
                        enabled = !isRefreshingModels,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.height(32.dp).testTag("refresh_models_btn")
                    ) {
                        if (isRefreshingModels) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loading...", fontSize = 10.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh Models", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = oxyyModelInput,
                        onValueChange = {
                            oxyyModelInput = it
                            configSavedSuccess = false
                        },
                        singleLine = true,
                        placeholder = { Text("e.g. oxyy-fast-chat or custom ID", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariantNavy,
                            unfocusedContainerColor = SurfaceVariantNavy,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorderNavy,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (cachedAiModels.isNotEmpty()) {
                                IconButton(onClick = { showModelMenu = !showModelMenu }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Choose Model", tint = NeonCyan)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("oxyy_model_id_input")
                    )

                    DropdownMenu(
                        expanded = showModelMenu && cachedAiModels.isNotEmpty(),
                        onDismissRequest = { showModelMenu = false },
                        modifier = Modifier.background(SurfaceVariantNavy)
                    ) {
                        cachedAiModels.forEach { modelInfo ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(modelInfo.id, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(modelInfo.name, color = TextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    oxyyModelInput = modelInfo.id
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                if (modelRefreshError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Error refreshing models: $modelRefreshError", color = WarningAmber, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Save Config + Test Connection + Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.setOxyyBaseUrl(oxyyBaseUrlInput)
                            viewModel.setOxyyApiKey(oxyyKeyInput)
                            viewModel.setOxyyModelId(oxyyModelInput)
                            configSavedSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = logoTheme.primaryColor, contentColor = Color(0xFF001F25)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_ai_config_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Config", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            // Ensure inputs are applied prior to test
                            viewModel.setOxyyBaseUrl(oxyyBaseUrlInput)
                            viewModel.setOxyyApiKey(oxyyKeyInput)
                            viewModel.setOxyyModelId(oxyyModelInput)
                            viewModel.testAiConnection()
                        },
                        enabled = !isTestingConnection,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.weight(1f).testTag("test_ai_connection_btn")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test API", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (configSavedSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = RadiantEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "✓ Configuration saved securely in Android storage.",
                            color = RadiantEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (connectionTestResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isSuccess = connectionTestResult?.startsWith("✓") == true
                    Surface(
                        color = if (isSuccess) RadiantEmerald.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = connectionTestResult ?: "",
                            color = if (isSuccess) RadiantEmerald else WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.clearOxyyConfiguration()
                        oxyyKeyInput = ""
                        oxyyModelInput = "oxyy-fast-chat"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("clear_ai_config_btn")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear API Key & Delete Config", fontSize = 11.sp)
                }
            }
        }

        // 4. INFERENCE HYPERPARAMETERS CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorderNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = RadiantEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inference Hyperparameters", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature: ${String.format("%.2f", aiTemperature)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (aiTemperature < 0.4f) "Precise / Strict" else if (aiTemperature > 0.9f) "Creative" else "Balanced", color = TextSecondary, fontSize = 11.sp)
                }
                Slider(
                    value = aiTemperature,
                    onValueChange = { viewModel.setAiTemperature(it) },
                    valueRange = 0.0f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = RadiantEmerald, activeTrackColor = RadiantEmerald),
                    modifier = Modifier.testTag("ai_temperature_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Tokens Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Response Tokens: $aiMaxTokens", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Response length limit", color = TextSecondary, fontSize = 11.sp)
                }
                Slider(
                    value = aiMaxTokens.toFloat(),
                    onValueChange = { viewModel.setAiMaxTokens(it.toInt()) },
                    valueRange = 128f..4096f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = RadiantEmerald, activeTrackColor = RadiantEmerald),
                    modifier = Modifier.testTag("ai_max_tokens_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Request Timeout Chips
                Text("Request Timeout", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { timeout ->
                        val isSelected = aiTimeoutSeconds == timeout
                        OutlinedButton(
                            onClick = { viewModel.setAiTimeoutSeconds(timeout) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isSelected) NeonCyan else TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else CardBorderNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${timeout}s", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
