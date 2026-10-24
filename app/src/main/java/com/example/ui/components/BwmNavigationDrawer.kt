package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Screen
import com.example.data.local.AssistantLogoTheme
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

data class DrawerMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val screen: Screen,
    val settingsTab: String? = null
)

@Composable
fun BwmNavigationDrawerContent(
    currentScreen: Screen,
    currentSettingsTab: String,
    assistantName: String,
    logoTheme: AssistantLogoTheme,
    onNavigate: (Screen, String?) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val menuItems = listOf(
        DrawerMenuItem("assistant", "Assistant", "Voice & chat interface", Icons.Default.SmartToy, Screen.Main),
        DrawerMenuItem("apps", "Apps & App Actions", "Launch & automate apps", Icons.Default.Apps, Screen.Apps),
        DrawerMenuItem("commands", "Command Library", "Searchable voice prompts", Icons.Default.LibraryBooks, Screen.CommandLibrary),
        DrawerMenuItem("tasks", "Task Queue", "Autonomous multi-task engine", Icons.Default.Sync, Screen.TaskQueue),
        DrawerMenuItem("notes", "Notes & Reminders", "Saved voice memos", Icons.AutoMirrored.Filled.Note, Screen.Notes),
        DrawerMenuItem("history", "History & Logs", "Past actions & outcomes", Icons.Default.History, Screen.History),
        DrawerMenuItem("voice_settings", "Voice & Language", "Male voice, pitch, rate", Icons.Default.Mic, Screen.Settings, "voice"),
        DrawerMenuItem("ai_settings", "AI Brain (Oxyy & Gemini)", "Oxyy AI, Gemini, & Offline Jarvis", Icons.Default.Psychology, Screen.Settings, "ai"),
        DrawerMenuItem("custom_commands", "Custom Voice Shortcuts", "Create trigger phrases & automations", Icons.Default.ElectricBolt, Screen.Settings, "commands"),
        DrawerMenuItem("permissions", "Permissions & Access", "Microphone, Contacts, Accessibility", Icons.Default.Security, Screen.Permissions),
        DrawerMenuItem("automation", "Automation Settings", "WhatsApp & app controls", Icons.Default.Tune, Screen.Settings, "automation"),
        DrawerMenuItem("privacy", "Privacy & Security", "Zero tracking & data safety", Icons.Default.Lock, Screen.Settings, "privacy"),
        DrawerMenuItem("about", "About BWM", "Jarvis Assistant v1.6.0", Icons.Default.Info, Screen.Settings, "about")
    )

    ModalDrawerSheet(
        drawerContainerColor = DeepNavyDark,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(logoTheme.primaryColor.copy(alpha = 0.2f))
                        .border(1.5.dp, logoTheme.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = logoTheme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = assistantName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RadiantEmerald)
                        )
                    }
                    Text(
                        text = "Jarvis Autonomous Assistant",
                        color = logoTheme.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                color = CardBorderNavy,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Items
            menuItems.forEach { item ->
                val isSelected = when (item.screen) {
                    Screen.Settings -> currentScreen == Screen.Settings && (item.settingsTab == null || currentSettingsTab == item.settingsTab)
                    else -> currentScreen == item.screen
                }

                val containerBg = if (isSelected) logoTheme.primaryColor.copy(alpha = 0.15f) else Color.Transparent
                val contentColor = if (isSelected) logoTheme.primaryColor else TextPrimary
                val iconTint = if (isSelected) logoTheme.primaryColor else TextSecondary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg)
                        .clickable {
                            onNavigate(item.screen, item.settingsTab)
                            onCloseDrawer()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("drawer_item_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isSelected) logoTheme.primaryColor.copy(alpha = 0.2f) else SurfaceVariantNavy,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.subtitle,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(logoTheme.primaryColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer version
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "BWM AI v1.3.0 • On-Device & Cloud",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
