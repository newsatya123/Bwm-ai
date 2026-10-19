package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.VoiceAssistantService
import com.example.ui.components.AppBottomBar
import com.example.ui.screens.AppsListScreen
import com.example.ui.screens.AssistantMainScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BWMTheme
import com.example.ui.theme.DeepNavyDark

sealed class Screen {
    object Main : Screen()
    object Apps : Screen()
    object Notes : Screen()
    object History : Screen()
    object Permissions : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BWMTheme {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                val logoTheme by viewModel.logoTheme.collectAsState()

                // Request runtime permissions on first launch (Audio, Notifications, Contacts)
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
                    val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
                }

                LaunchedEffect(Unit) {
                    val needed = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.READ_CONTACTS)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            needed.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (needed.isNotEmpty()) {
                        permissionsLauncher.launch(needed.toTypedArray())
                    }
                }

                // Handle intent if launched from notification or voice action
                LaunchedEffect(intent) {
                    handleIncomingIntent(intent, viewModel)
                }

                Scaffold(
                    containerColor = DeepNavyDark,
                    bottomBar = {
                        AppBottomBar(
                            currentScreen = currentScreen,
                            onNavigate = { screen -> currentScreen = screen },
                            logoTheme = logoTheme
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        when (currentScreen) {
                            Screen.Main -> {
                                AssistantMainScreen(
                                    viewModel = viewModel,
                                    onNavigateToApps = { currentScreen = Screen.Apps },
                                    onNavigateToNotes = { currentScreen = Screen.Notes },
                                    onNavigateToHistory = { currentScreen = Screen.History },
                                    onNavigateToPermissions = { currentScreen = Screen.Permissions },
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
                                )
                            }
                            Screen.Apps -> {
                                BackHandler { currentScreen = Screen.Main }
                                AppsListScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Main }
                                )
                            }
                            Screen.Notes -> {
                                BackHandler { currentScreen = Screen.Main }
                                NotesScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Main }
                                )
                            }
                            Screen.History -> {
                                BackHandler { currentScreen = Screen.Main }
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Main }
                                )
                            }
                            Screen.Permissions -> {
                                BackHandler { currentScreen = Screen.Main }
                                PermissionsScreen(
                                    onBack = { currentScreen = Screen.Main }
                                )
                            }
                            Screen.Settings -> {
                                BackHandler { currentScreen = Screen.Main }
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Main }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?, viewModel: MainViewModel) {
        val action = intent?.action ?: return
        if (action == VoiceAssistantService.ACTION_START_LISTENING || action == Intent.ACTION_VOICE_COMMAND) {
            viewModel.toggleVoiceListening()
        }
    }
}
