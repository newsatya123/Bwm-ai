package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActionLogItem
import com.example.data.local.AssistantDatabase
import com.example.data.local.ConversationMessage
import com.example.data.local.NoteItem
import com.example.data.remote.AgentResponse
import com.example.data.remote.GeminiAssistantAgent
import com.example.data.remote.ToolActionRequest
import com.example.engine.actions.AppLauncher
import com.example.engine.actions.DeviceController
import com.example.engine.actions.InstalledAppInfo
import com.example.engine.actions.ToolExecutionResult
import com.example.engine.voice.ModularWakeWordEngine
import com.example.engine.voice.SpeechRecognitionState
import com.example.engine.voice.SpeechRecognizerManager
import com.example.engine.voice.TextToSpeechManager
import com.example.service.BwmAccessibilityService
import com.example.service.VoiceAssistantService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AssistantUiState {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING,
    ERROR
}

data class PendingConfirmation(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AssistantDatabase.getDatabase(application)
    private val dao = database.assistantDao()

    val messages = dao.getAllMessages().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val notes = dao.getAllNotes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val actionLogs = dao.getRecentActionLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Engines & Controllers
    val deviceController = DeviceController(application)
    val appLauncher = AppLauncher(application)
    val geminiAgent = GeminiAssistantAgent()
    val ttsManager = TextToSpeechManager(application)
    val wakeWordEngine = ModularWakeWordEngine(application)

    private val _uiState = MutableStateFlow(AssistantUiState.IDLE)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _currentInterimText = MutableStateFlow("")
    val currentInterimText: StateFlow<String> = _currentInterimText.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _currentStatusMessage = MutableStateFlow("Ready for your command")
    val currentStatusMessage: StateFlow<String> = _currentStatusMessage.asStateFlow()

    // Settings
    val selectedLanguage = MutableStateFlow("en-IN") // "en-IN", "hi-IN", "en-US"
    val isContinuousListening = MutableStateFlow(false)
    val isWakeWordEnabled = MutableStateFlow(false)

    val speechManager = SpeechRecognizerManager(application) { recognizedText ->
        handleUserInput(recognizedText)
    }

    val audioRmsDb: StateFlow<Float> = speechManager.audioRmsDb

    init {
        loadInstalledApps()
        observeSpeechState()
    }

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechManager.recognitionState.collect { state ->
                when (state) {
                    is SpeechRecognitionState.Listening -> {
                        _uiState.value = AssistantUiState.LISTENING
                        _currentStatusMessage.value = "Listening to you..."
                    }
                    is SpeechRecognitionState.PartialResult -> {
                        _currentInterimText.value = state.text
                    }
                    is SpeechRecognitionState.FinalResult -> {
                        _currentInterimText.value = state.text
                    }
                    is SpeechRecognitionState.Error -> {
                        _uiState.value = AssistantUiState.ERROR
                        _currentStatusMessage.value = state.message
                    }
                    is SpeechRecognitionState.Idle, SpeechRecognitionState.Ready -> {
                        if (_uiState.value == AssistantUiState.LISTENING) {
                            _uiState.value = AssistantUiState.IDLE
                        }
                    }
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appLauncher.getInstalledLauncherApps()
            _installedApps.value = apps
        }
    }

    fun toggleVoiceListening() {
        if (_uiState.value == AssistantUiState.LISTENING) {
            speechManager.stopListening()
            _uiState.value = AssistantUiState.IDLE
            _currentStatusMessage.value = "Listening stopped"
        } else {
            ttsManager.stop()
            _currentInterimText.value = ""
            speechManager.currentLanguageCode = selectedLanguage.value
            speechManager.startListening()
        }
    }

    fun handleUserInput(input: String) {
        if (input.isBlank()) return
        ttsManager.stop()
        speechManager.stopListening()

        _currentInterimText.value = ""
        _uiState.value = AssistantUiState.PROCESSING
        _currentStatusMessage.value = "Thinking..."

        viewModelScope.launch {
            // 1. Record User Message
            dao.insertMessage(ConversationMessage(role = "user", content = input))

            // 2. Fetch history context
            val historyList = messages.value.takeLast(6).map { it.role to it.content }

            // 3. Process via Gemini Agent (or fallback NLP)
            val agentResponse: AgentResponse = geminiAgent.processCommand(input, historyList)

            // 4. Handle Action
            if (agentResponse.toolAction != null) {
                if (agentResponse.requiresConfirmation && agentResponse.confirmationPrompt != null) {
                    _uiState.value = AssistantUiState.IDLE
                    _currentStatusMessage.value = "Action requires your confirmation"
                    _pendingConfirmation.value = PendingConfirmation(
                        title = "Confirm Action",
                        message = agentResponse.confirmationPrompt,
                        onConfirm = {
                            _pendingConfirmation.value = null
                            executeToolAction(agentResponse.toolAction, agentResponse.responseText)
                        },
                        onCancel = {
                            _pendingConfirmation.value = null
                            viewModelScope.launch {
                                dao.insertMessage(ConversationMessage(
                                    role = "assistant",
                                    content = "Action cancelled by user."
                                ))
                                _currentStatusMessage.value = "Action cancelled"
                                _uiState.value = AssistantUiState.IDLE
                            }
                        }
                    )
                } else {
                    executeToolAction(agentResponse.toolAction, agentResponse.responseText)
                }
            } else {
                // Pure conversational response
                dao.insertMessage(ConversationMessage(
                    role = "assistant",
                    content = agentResponse.responseText
                ))
                speakResponse(agentResponse.responseText)
            }
        }
    }

    private fun executeToolAction(action: ToolActionRequest, initialSpoken: String) {
        _uiState.value = AssistantUiState.EXECUTING
        _currentStatusMessage.value = "Executing action..."

        viewModelScope.launch(Dispatchers.IO) {
            val executionResult = when (action) {
                is ToolActionRequest.LaunchApp -> appLauncher.launchAppByName(action.appName)
                is ToolActionRequest.ToggleFlashlight -> deviceController.toggleFlashlight(action.enable)
                is ToolActionRequest.AdjustVolume -> deviceController.adjustVolume(action.direction, action.levelPercent)
                is ToolActionRequest.GetBatteryStatus -> deviceController.getBatteryStatus()
                is ToolActionRequest.SetTimer -> deviceController.setTimer(action.seconds, action.label)
                is ToolActionRequest.SetAlarm -> deviceController.setAlarm(action.hour, action.minute, action.message)
                is ToolActionRequest.OpenSettings -> deviceController.openSettings(action.settingType)
                is ToolActionRequest.DialPhone -> deviceController.openDialer(action.phoneNumber)
                is ToolActionRequest.OpenUrl -> deviceController.openUrl(action.url)
                is ToolActionRequest.SaveNote -> {
                    val noteId = dao.insertNote(NoteItem(title = action.title, content = action.content))
                    ToolExecutionResult(true, "Saved note '${action.title}' (ID: $noteId)")
                }
                is ToolActionRequest.GetNotes -> {
                    val noteList = dao.searchNotes("")
                    val summary = if (noteList.isEmpty()) "You have no saved notes."
                    else "You have ${noteList.size} notes: " + noteList.take(3).joinToString { it.title }
                    ToolExecutionResult(true, summary)
                }
                is ToolActionRequest.DeviceNavigation -> {
                    val success = BwmAccessibilityService.performGlobal(action.action)
                    if (success) {
                        ToolExecutionResult(true, "Navigated ${action.action}")
                    } else {
                        // If accessibility service is not enabled, guide user to enable it
                        deviceController.openSettings("accessibility")
                        ToolExecutionResult(false, "Please enable BWM AI Accessibility Service to perform '${action.action}' navigation.")
                    }
                }
            }

            // Log action in Room
            dao.insertActionLog(ActionLogItem(
                toolName = action::class.simpleName ?: "Action",
                parameters = action.toString(),
                result = executionResult.message,
                isSuccess = executionResult.isSuccess
            ))

            // Post response message
            val finalReply = if (executionResult.isSuccess) {
                if (initialSpoken.isNotBlank() && initialSpoken != executionResult.message) {
                    "$initialSpoken. ${executionResult.message}"
                } else executionResult.message
            } else {
                "Action failed: ${executionResult.message}"
            }

            dao.insertMessage(ConversationMessage(
                role = "assistant",
                content = finalReply,
                actionType = action::class.simpleName,
                actionDetail = executionResult.message,
                isActionSuccess = executionResult.isSuccess
            ))

            withContext(Dispatchers.Main) {
                _currentStatusMessage.value = executionResult.message
                speakResponse(finalReply)
            }
        }
    }

    private fun speakResponse(text: String) {
        _uiState.value = AssistantUiState.SPEAKING
        ttsManager.speak(text) {
            viewModelScope.launch {
                _uiState.value = AssistantUiState.IDLE
                _currentStatusMessage.value = "Ready"

                // Continuous listening feature: if enabled, start listening for follow-up!
                if (isContinuousListening.value) {
                    toggleVoiceListening()
                }
            }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        if (_uiState.value == AssistantUiState.SPEAKING) {
            _uiState.value = AssistantUiState.IDLE
            _currentStatusMessage.value = "Ready"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAllMessages()
            dao.clearActionLogs()
            _currentStatusMessage.value = "History cleared"
        }
    }

    fun addManualNote(title: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(NoteItem(title = title, content = content))
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            dao.deleteNote(note)
        }
    }

    fun toggleBackgroundService() {
        val appContext = getApplication<Application>()
        if (VoiceAssistantService.isServiceRunning.value) {
            VoiceAssistantService.stopService(appContext)
        } else {
            VoiceAssistantService.startService(appContext)
        }
    }

    fun toggleWakeWord(enabled: Boolean) {
        isWakeWordEnabled.value = enabled
        if (enabled) {
            wakeWordEngine.start { attachedCommand ->
                viewModelScope.launch {
                    deviceController.vibrate(80)
                    _currentStatusMessage.value = "⚡ 'Hey BWM' detected!"

                    if (!attachedCommand.isNullOrBlank()) {
                        // User said e.g. "Hey BWM open whatsapp" or "Hey BWM flashlight chalu karo"
                        handleUserInput(attachedCommand)
                    } else {
                        // User just said "Hey BWM"
                        val greeting = if (selectedLanguage.value == "hi-IN") "Haan ji, boliye!" else "Yes, I'm listening!"
                        _currentStatusMessage.value = greeting
                        speakResponse(greeting)
                    }
                }
            }
        } else {
            wakeWordEngine.stop()
        }
    }

    fun triggerHeyBwmTest(attachedCommand: String? = null) {
        if (!isWakeWordEnabled.value) {
            toggleWakeWord(true)
        }
        wakeWordEngine.simulateWakeWord(attachedCommand)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
        wakeWordEngine.stop()
    }
}
