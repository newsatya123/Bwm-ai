package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.VoiceAssistantService
import com.example.ui.components.BwmNavigationDrawerContent
import com.example.ui.screens.AppsListScreen
import com.example.ui.screens.AssistantMainScreen
import com.example.ui.screens.CommandLibraryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskQueueScreen
import com.example.ui.theme.BWMTheme
import com.example.ui.theme.DeepNavyDark
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    object Apps : Screen()
    object CommandLibrary : Screen()
    object TaskQueue : Screen()
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
                var currentSettingsTab by remember { mutableStateOf("General") }

                val assistantName by viewModel.assistantName.collectAsState()
                val logoTheme by viewModel.logoTheme.collectAsState()

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Request runtime permissions on first launch (Audio, Notifications, Contacts)
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

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

                // PDF document picker for voice-activated PDF reading
                val pdfPickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                cursor.getString(nameIndex)
                            } else "Document.pdf"
                        } ?: "Document.pdf"
                        viewModel.openPdfFromUri(uri, fileName)
                    }
                }

                val requestPdfPicker by viewModel.requestOpenPdfPicker.collectAsState()
                LaunchedEffect(requestPdfPicker) {
                    if (requestPdfPicker) {
                        viewModel.onPdfPickerConsumed()
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                }

                // Handle intent if launched from notification or voice action
                LaunchedEffect(intent) {
                    handleIncomingIntent(intent, viewModel)
                }

                // Handle Back button: close drawer first, or go back to Main screen
                BackHandler(enabled = drawerState.isOpen || currentScreen != Screen.Main) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else if (currentScreen != Screen.Main) {
                        currentScreen = Screen.Main
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        BwmNavigationDrawerContent(
                            currentScreen = currentScreen,
                            currentSettingsTab = currentSettingsTab,
                            assistantName = assistantName,
                            logoTheme = logoTheme,
                            onNavigate = { targetScreen, targetSettingsTab ->
                                if (targetSettingsTab != null) {
                                    currentSettingsTab = targetSettingsTab
                                }
                                currentScreen = targetScreen
                            },
                            onCloseDrawer = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        containerColor = DeepNavyDark
                        // Bottom navigation bar completely removed per user instruction
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
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        }
                                    )
                                }
                                Screen.Apps -> {
                                    AppsListScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.CommandLibrary -> {
                                    CommandLibraryScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.TaskQueue -> {
                                    TaskQueueScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Notes -> {
                                    NotesScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.History -> {
                                    HistoryScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Permissions -> {
                                    PermissionsScreen(
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
                                Screen.Settings -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        initialTab = currentSettingsTab,
                                        onBack = { currentScreen = Screen.Main }
                                    )
                                }
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
