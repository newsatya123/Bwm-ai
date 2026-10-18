package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.BwmAccessibilityService
import com.example.service.VoiceAssistantService
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.DeepNavyDark
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
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val isAccessibilityActive by BwmAccessibilityService.isServiceActive.collectAsState()
    val isServiceRunning by VoiceAssistantService.isServiceRunning.collectAsState()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("permissions_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }
            Text(
                text = "Permissions & Privacy",
                color = TextPrimary,
                fontSize = 20.sp,
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
            // Privacy Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantNavy),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = RadiantEmerald, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "BWM AI uses strict on-device permissions. Voice is recognized locally or through your personal Gemini API key. No unauthorized data leaves your device.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Microphone
            PermissionCard(
                title = "Microphone Access",
                description = "Required to recognize spoken voice commands and wake-word detection.",
                icon = Icons.Default.Mic,
                isGranted = micGranted,
                actionText = "Grant Permission",
                onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            // Notifications
            PermissionCard(
                title = "Notifications",
                description = "Required for persistent background voice assistant notification with quick controls.",
                icon = Icons.Default.Notifications,
                isGranted = notifGranted,
                actionText = "Grant Permission",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // Accessibility Service
            var showAccessibilityInfoDialog by remember { mutableStateOf(false) }

            if (showAccessibilityInfoDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAccessibilityInfoDialog = false },
                    title = { Text("Accessibility Permission", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                "Why BWM AI needs Accessibility:\n\n" +
                                "1. 📺 YouTube Automation: Automatically plays first search result, pauses/resumes, and skips to next video on voice command.\n" +
                                "2. 📱 Phone Navigation: Performs Back, Home, and Notification shade gestures hands-free.\n\n" +
                                "🔒 Privacy Guarantee: BWM AI never reads personal passwords, private messages, or financial information.\n\n" +
                                "How to enable:\n" +
                                "1. Tap 'Open Settings' below.\n" +
                                "2. Tap 'Installed apps' or 'Downloaded apps'.\n" +
                                "3. Tap 'BWM AI Screen Automation' and toggle ON.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAccessibilityInfoDialog = false
                                BwmAccessibilityService.openAccessibilitySettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF001F25))
                        ) {
                            Text("Open Settings", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showAccessibilityInfoDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = SurfaceNavy,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            PermissionCard(
                title = "YouTube & Screen Automation",
                description = "Enables voice controls for YouTube (search, autoplay first video, pause/resume, next) and phone navigation (Back, Home, Notifications).",
                icon = Icons.Default.AccessibilityNew,
                isGranted = isAccessibilityActive,
                actionText = if (isAccessibilityActive) "Configured (Active)" else "Enable in Settings",
                onAction = {
                    if (isAccessibilityActive) {
                        BwmAccessibilityService.openAccessibilitySettings(context)
                    } else {
                        showAccessibilityInfoDialog = true
                    }
                }
            )

            // Background Service Control Card
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
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                                contentDescription = null,
                                tint = if (isServiceRunning) RadiantEmerald else TextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Background Listening Service",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Surface(
                            color = if (isServiceRunning) RadiantEmerald.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isServiceRunning) "ACTIVE" else "STOPPED",
                                color = if (isServiceRunning) RadiantEmerald else ErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Runs a foreground service with an ongoing notification so BWM AI can listen while other apps are in use.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (isServiceRunning) {
                                VoiceAssistantService.stop(context)
                            } else {
                                VoiceAssistantService.start(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) ErrorRed else NeonCyan,
                            contentColor = Color(0xFF001F25)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_background_service_btn")
                    ) {
                        Text(
                            text = if (isServiceRunning) "Stop Background Service" else "Start Background Service",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // App Settings shortcut
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_system_app_settings_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open System App Settings", color = TextPrimary)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
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
                    Icon(icon, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) RadiantEmerald else WarningAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(description, color = TextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(10.dp))
            if (!isGranted) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF001F25)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
