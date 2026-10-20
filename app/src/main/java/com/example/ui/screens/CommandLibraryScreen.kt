package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
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

data class CommandItem(
    val category: String,
    val command: String,
    val alternative: String? = null,
    val language: String = "Hindi / Hinglish / English",
    val requiredPermission: String? = null,
    val supportedApps: String = "BWM Assistant / System",
    val expectedResult: String,
    val icon: ImageVector
)

@Composable
fun CommandLibraryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        "All",
        "Wake Word",
        "WhatsApp",
        "Calling",
        "Play Store",
        "YouTube",
        "Apps",
        "Navigation",
        "Device Controls",
        "Multi-Step Tasks",
        "Notes & Alarms",
        "AI Assistant"
    )

    val allCommands = remember {
        listOf(
            CommandItem(
                category = "Wake Word",
                command = "Hey BWM",
                alternative = "Suno BWM / Ok BWM",
                language = "English & Hindi",
                requiredPermission = "Microphone",
                supportedApps = "Global (Foreground & Background)",
                expectedResult = "Assistant wakes up silently and immediately captures voice command without beeps.",
                icon = Icons.Default.Mic
            ),
            CommandItem(
                category = "WhatsApp",
                command = "Raman ko hii bhejo",
                alternative = "Raman ko message bhejo: main ghar aa raha hoon",
                language = "Hinglish / Hindi",
                requiredPermission = "Contacts & Accessibility",
                supportedApps = "WhatsApp & WhatsApp Business",
                expectedResult = "Searches contact, opens WhatsApp chat, and sends or asks confirmation before sending.",
                icon = Icons.AutoMirrored.Filled.Chat
            ),
            CommandItem(
                category = "WhatsApp",
                command = "Papa ki chat kholo",
                alternative = "Raman ka WhatsApp chat kholo",
                language = "Hinglish / Hindi",
                requiredPermission = "Contacts",
                supportedApps = "WhatsApp",
                expectedResult = "Finds Papa's phone number and opens the WhatsApp conversation directly.",
                icon = Icons.AutoMirrored.Filled.Chat
            ),
            CommandItem(
                category = "WhatsApp",
                command = "WhatsApp mein search karo office",
                alternative = "Search in WhatsApp office group",
                language = "Hindi / English",
                requiredPermission = "Accessibility",
                supportedApps = "WhatsApp",
                expectedResult = "Clicks search bar in WhatsApp and enters the query.",
                icon = Icons.AutoMirrored.Filled.Chat
            ),
            CommandItem(
                category = "Calling",
                command = "Raman ko call karo",
                alternative = "Papa ko phone lagao",
                language = "Hinglish / Hindi",
                requiredPermission = "Contacts & Phone",
                supportedApps = "System Phone / Dialer",
                expectedResult = "Resolves contact from address book and prepares phone call.",
                icon = Icons.Default.Call
            ),
            CommandItem(
                category = "Calling",
                command = "9876543210 par call karo",
                alternative = "Dial 9876543210",
                language = "English & Hindi",
                requiredPermission = "Phone",
                supportedApps = "System Dialer",
                expectedResult = "Opens system dialer with the phone number pre-filled.",
                icon = Icons.Default.Call
            ),
            CommandItem(
                category = "Calling",
                command = "Recent calls kholo",
                alternative = "Call log check karo",
                language = "Hinglish",
                requiredPermission = "None",
                supportedApps = "System Call Log",
                expectedResult = "Opens the device recent call logs screen.",
                icon = Icons.Default.Call
            ),
            CommandItem(
                category = "Play Store",
                command = "Play Store kholo",
                alternative = "Google Play Store open karo",
                language = "Hindi & English",
                requiredPermission = "None",
                supportedApps = "Google Play Store",
                expectedResult = "Launches Google Play Store app.",
                icon = Icons.Default.ShoppingBag
            ),
            CommandItem(
                category = "Play Store",
                command = "Instagram download karo",
                alternative = "Play Store par Instagram search karo",
                language = "Hinglish / Hindi",
                requiredPermission = "None",
                supportedApps = "Google Play Store",
                expectedResult = "Opens Instagram directly in Play Store for verified installation.",
                icon = Icons.Default.ShoppingBag
            ),
            CommandItem(
                category = "YouTube",
                command = "YouTube par songs search karo",
                alternative = "Arijit Singh ke songs chalao",
                language = "Hindi & English",
                requiredPermission = "Accessibility (for auto-play)",
                supportedApps = "YouTube",
                expectedResult = "Searches YouTube and auto-plays first video using accessibility click.",
                icon = Icons.Default.VideoLibrary
            ),
            CommandItem(
                category = "YouTube",
                command = "Pause karo",
                alternative = "Play karo / Agla video chalao",
                language = "Hindi & English",
                requiredPermission = "Accessibility / Media",
                supportedApps = "YouTube & Media Players",
                expectedResult = "Context-aware control: pauses or resumes currently active YouTube playback.",
                icon = Icons.Default.VideoLibrary
            ),
            CommandItem(
                category = "Navigation",
                command = "Chat mein neeche scroll karo",
                alternative = "Scroll down / Scroll up",
                language = "Hindi & English",
                requiredPermission = "Accessibility",
                supportedApps = "WhatsApp, YouTube, Browser, Any App",
                expectedResult = "Performs context-aware forward/backward scroll gesture in active app.",
                icon = Icons.Default.Navigation
            ),
            CommandItem(
                category = "Navigation",
                command = "Go back",
                alternative = "Peeche jao / Back karo",
                language = "English & Hindi",
                requiredPermission = "Accessibility",
                supportedApps = "All Apps",
                expectedResult = "Executes global BACK action.",
                icon = Icons.Default.Navigation
            ),
            CommandItem(
                category = "Navigation",
                command = "Close current app",
                alternative = "Home screen par jao / Home jao",
                language = "English & Hindi",
                requiredPermission = "Accessibility / System",
                supportedApps = "All Apps",
                expectedResult = "Dismisses active application screen safely and returns to Home screen.",
                icon = Icons.Default.Navigation
            ),
            CommandItem(
                category = "Device Controls",
                command = "Torch chalu karo",
                alternative = "Flashlight band karo",
                language = "Hindi & English",
                requiredPermission = "Camera (Hardware Torch)",
                supportedApps = "System Hardware",
                expectedResult = "Toggles phone rear camera LED flashlight immediately.",
                icon = Icons.Default.ElectricBolt
            ),
            CommandItem(
                category = "Device Controls",
                command = "Volume 50 percent kar do",
                alternative = "Volume badhao / Awaz kam karo / Mute karo",
                language = "Hindi & English",
                requiredPermission = "None",
                supportedApps = "Android Audio Manager",
                expectedResult = "Adjusts media volume to requested percentage or level.",
                icon = Icons.Default.Tune
            ),
            CommandItem(
                category = "Device Controls",
                command = "Battery kitni hai?",
                alternative = "Battery percentage batao",
                language = "Hindi & English",
                requiredPermission = "None",
                supportedApps = "System Battery Manager",
                expectedResult = "Speaks battery percentage and charging status.",
                icon = Icons.Default.Tune
            ),
            CommandItem(
                category = "Multi-Step Tasks",
                command = "Torch on karo aur YouTube chalao",
                alternative = "Pehle YouTube kholo, phir search karo",
                language = "Hindi & Hinglish",
                requiredPermission = "Relevant App Permissions",
                supportedApps = "Multi-Task Queue",
                expectedResult = "Executes multi-step compound actions sequentially one-by-one via autonomous task queue.",
                icon = Icons.Default.ElectricBolt
            ),
            CommandItem(
                category = "Multi-Step Tasks",
                command = "Current task cancel karo",
                alternative = "Queue pause karo / Agla task skip karo / Abhi kya kar rahe ho?",
                language = "Hindi & Hinglish",
                requiredPermission = "None",
                supportedApps = "BWM Task Queue",
                expectedResult = "Manages the sequential task execution queue on the fly.",
                icon = Icons.Default.ElectricBolt
            ),
            CommandItem(
                category = "Notes & Alarms",
                command = "5 minute ka timer lagao",
                alternative = "Subah 7 baje ka alarm lagao",
                language = "Hindi & English",
                requiredPermission = "None",
                supportedApps = "System Clock",
                expectedResult = "Sets system countdown timer or alarm.",
                icon = Icons.Default.Alarm
            ),
            CommandItem(
                category = "Notes & Alarms",
                command = "Note likho: Kal bank jana hai",
                alternative = "Save voice note groceries",
                language = "Hindi & English",
                requiredPermission = "None",
                supportedApps = "BWM Assistant Local DB",
                expectedResult = "Saves voice note in local Room database.",
                icon = Icons.Default.Alarm
            ),
            CommandItem(
                category = "AI Assistant",
                command = "What is artificial intelligence?",
                alternative = "Translate to English: main kal aaunga",
                language = "Hindi & English",
                requiredPermission = "Internet (Gemini AI)",
                supportedApps = "Gemini AI & Offline NLP",
                expectedResult = "Provides intelligent, concise spoken and text AI response.",
                icon = Icons.Default.Psychology
            )
        )
    }

    val filteredCommands = remember(searchQuery, selectedCategory) {
        allCommands.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.command.contains(searchQuery, ignoreCase = true) ||
                    (item.alternative?.contains(searchQuery, ignoreCase = true) == true) ||
                    item.expectedResult.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyDark)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("command_library_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Command Library", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Tested & Verified Voice Commands", color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search voice commands...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceNavy,
                unfocusedContainerColor = SurfaceNavy,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CardBorderNavy,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("command_search_input")
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonCyan else CardBorderNavy
                    ),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Command Cards
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCommands) { cmd ->
                CommandCard(cmd = cmd, onTry = {
                    viewModel.handleUserInput(cmd.command)
                    onBack()
                })
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CommandCard(
    cmd: CommandItem,
    onTry: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(cmd.icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "\"${cmd.command}\"",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = cmd.category,
                            color = RadiantEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (!cmd.alternative.isNullOrBlank()) {
                        Text(
                            text = "Alternative: \"${cmd.alternative}\"",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "Result: ${cmd.expectedResult}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cmd.requiredPermission?.let { perm ->
                            Surface(
                                color = WarningAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Requires: $perm",
                                    color = WarningAmber,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = NeonCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onTry() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Try Command", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
