package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ActionsLogScreen
import com.example.ui.screens.AppsListScreen
import com.example.ui.screens.AssistantMainScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PermissionsDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenDestination {
    HOME,
    APPS,
    NOTES,
    HISTORY,
    PERMISSIONS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BwmAssistantApp()
            }
        }
    }
}

@Composable
fun BwmAssistantApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    var currentScreen by remember { mutableStateOf(ScreenDestination.HOME) }

    // Proactively request Microphone & Notification permissions on initial launch
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Handle back button for sub-screens
    if (currentScreen != ScreenDestination.HOME) {
        BackHandler {
            currentScreen = ScreenDestination.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { target ->
            when (target) {
                ScreenDestination.HOME -> {
                    AssistantMainScreen(
                        viewModel = viewModel,
                        onNavigateToApps = { currentScreen = ScreenDestination.APPS },
                        onNavigateToNotes = { currentScreen = ScreenDestination.NOTES },
                        onNavigateToHistory = { currentScreen = ScreenDestination.HISTORY },
                        onNavigateToPermissions = { currentScreen = ScreenDestination.PERMISSIONS },
                        onNavigateToSettings = { currentScreen = ScreenDestination.SETTINGS }
                    )
                }
                ScreenDestination.APPS -> {
                    AppsListScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                }
                ScreenDestination.NOTES -> {
                    NotesScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                }
                ScreenDestination.HISTORY -> {
                    ActionsLogScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                }
                ScreenDestination.PERMISSIONS -> {
                    PermissionsDashboardScreen(
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                }
                ScreenDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenDestination.HOME }
                    )
                }
            }
        }
    }
}
