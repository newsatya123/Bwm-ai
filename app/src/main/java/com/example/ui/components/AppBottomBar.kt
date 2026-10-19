package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    logoTheme: AssistantLogoTheme,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(Screen.Main, "Assistant", Icons.Default.Mic, "bottom_nav_assistant"),
        NavItem(Screen.Apps, "Apps", Icons.Default.Apps, "bottom_nav_apps"),
        NavItem(Screen.Notes, "Notes", Icons.AutoMirrored.Filled.Note, "bottom_nav_notes"),
        NavItem(Screen.History, "History", Icons.Default.History, "bottom_nav_history"),
        NavItem(Screen.Settings, "Settings", Icons.Default.Settings, "bottom_nav_settings")
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, CardBorderNavy, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = SurfaceNavy,
        tonalElevation = 8.dp
    ) {
        navItems.forEach { item ->
            val isSelected = currentScreen::class == item.screen::class
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepNavyDark,
                    selectedTextColor = logoTheme.primaryColor,
                    indicatorColor = logoTheme.primaryColor,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
