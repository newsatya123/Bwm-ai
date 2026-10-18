package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TextPrimary

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val command: String
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("Hey BWM", Icons.Default.ElectricBolt, "hey bwm what can you do?"),
        QuickAction("Torch On", Icons.Default.FlashlightOn, "turn on flashlight"),
        QuickAction("Battery", Icons.Default.BatteryChargingFull, "check battery status"),
        QuickAction("Volume Up", Icons.AutoMirrored.Filled.VolumeUp, "increase volume"),
        QuickAction("Timer 5m", Icons.Default.Timer, "set timer for 5 minutes"),
        QuickAction("Alarm 7AM", Icons.Default.Alarm, "set alarm for 7:00 am"),
        QuickAction("Open WhatsApp", Icons.Default.Apps, "open whatsapp"),
        QuickAction("Open YouTube", Icons.Default.Apps, "open youtube"),
        QuickAction("Open Camera", Icons.Default.Apps, "open camera"),
        QuickAction("New Note", Icons.AutoMirrored.Filled.NoteAdd, "save note: Meeting at 4 PM"),
        QuickAction("Phone Dialer", Icons.Default.Phone, "dial phone 198"),
        QuickAction("Wi-Fi Settings", Icons.Default.Settings, "open wifi settings")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            AssistChip(
                onClick = { onActionClick(action.command) },
                label = { Text(action.label, color = TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = NeonCyan
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = SurfaceNavy
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = CardBorderNavy
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("chip_${action.label.replace(" ", "_")}")
            )
        }
    }
}
