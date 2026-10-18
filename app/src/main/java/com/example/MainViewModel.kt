package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActionLogItem
import com.example.data.local.AssistantDatabase
import com.example.data.local.ConversationMessage
import com.example.data.local.NoteItem
import com.example.data.remote.GeminiAssistantAgent
import com.example.data.remote.ToolActionRequest
import com.example.engine.actions.AppLauncher
import com.example.engine.actions.DeviceController
import com.example.engine.actions.InstalledAppInfo
import com.example.engine.voice.ModularWakeWordEngine
import com.example.engine.voice.SpeechRecognitionState
import com.example.engine.voice.SpeechRecognizerManager
import com.example.engine.voice.TextToSpeechManager
import com.example.engine.voice.WakeWordDetector
import com.example.service.BwmAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = AssistantDatabase.getDatabase(context)
    private val dao = database.assistantDao()

    val appLauncher = AppLauncher(context)
    val deviceController = DeviceController(context)
    val ttsManager = TextToSpeechManager(context)
    private val geminiAgent = GeminiAssistantAgent()

    // State flows
    private val _uiState = MutableStateFlow(AssistantUiState.IDLE)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _currentInterimText = MutableStateFlow("")
    val currentInterimText: StateFlow<String> = _currentInterimText.asStateFlow()

    private val _currentStatusMessage = MutableStateFlow("Ready for commands. Tap the orb or say 'Hey BWM'")
    val currentStatusMessage: StateFlow<String> = _currentStatusMessage.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    val selectedLanguage = MutableStateFlow("en-IN")

    private val _isWakeWordEnabled = MutableStateFlow(true)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ConfirmationDialogData?>(null)
    val pendingConfirmation: StateFlow<ConfirmationDialogData?> = _pendingConfirmation.asStateFlow()

    // Database flows
    val messages: StateFlow<List<ConversationMessage>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteItem>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionLogs: StateFlow<List<ActionLogItem>> = dao.getRecentActionLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    // Voice recognition & Wake word
    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private val wakeWordEngine: WakeWordDetector = ModularWakeWordEngine(context)

    private var activeProcessingJob: Job? = null

    init {
        initSpeechRecognizer()
        refreshApps()
        startWakeWordEngineIfEnabled()

        // Track TTS state to reset assistant to IDLE when done speaking
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _uiState.value == AssistantUiState.SPEAKING) {
                    _uiState.value = AssistantUiState.IDLE
                    _currentStatusMessage.value = "Listening for 'Hey BWM' or tap microphone"
                    startWakeWordEngineIfEnabled()
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizerManager = SpeechRecognizerManager(context) { finalTranscription ->
            handleUserInput(finalTranscription)
        }

        viewModelScope.launch {
            speechRecognizerManager?.recognitionState?.collect { recState ->
                when (recState) {
                    is SpeechRecognitionState.Listening -> {
                        _uiState.value = AssistantUiState.LISTENING
                        _currentStatusMessage.value = "Listening... Speak in Hindi or English"
                    }
                    is SpeechRecognitionState.PartialResult -> {
                        _currentInterimText.value = recState.text
                    }
                    is SpeechRecognitionState.FinalResult -> {
                        _currentInterimText.value = ""
                    }
                    is SpeechRecognitionState.Error -> {
                        _uiState.value = AssistantUiState.ERROR
                        _currentStatusMessage.value = recState.message
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(3000)
                            if (_uiState.value == AssistantUiState.ERROR) {
                                _uiState.value = AssistantUiState.IDLE
                                _currentStatusMessage.value = "Ready for commands"
                                startWakeWordEngineIfEnabled()
                            }
                        }
                    }
                    is SpeechRecognitionState.Idle -> {
                        if (_uiState.value == AssistantUiState.LISTENING) {
                            _uiState.value = AssistantUiState.IDLE
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            speechRecognizerManager?.audioRmsDb?.collect { rms ->
                _audioRmsDb.value = rms
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appLauncher.getInstalledLauncherApps()
            _installedApps.value = apps
        }
    }

    private fun startWakeWordEngineIfEnabled() {
        if (_isWakeWordEnabled.value && _uiState.value == AssistantUiState.IDLE) {
            wakeWordEngine.start { attachedCommand ->
                viewModelScope.launch(Dispatchers.Main) {
                    ttsManager.stop()
                    if (!attachedCommand.isNullOrBlank()) {
                        handleUserInput(attachedCommand)
                    } else {
                        startVoiceListening()
                    }
                }
            }
        }
    }

    fun toggleWakeWord(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        if (enabled) {
            startWakeWordEngineIfEnabled()
            _currentStatusMessage.value = "Wake-word 'Hey BWM' enabled"
        } else {
            wakeWordEngine.stop()
            _currentStatusMessage.value = "Wake-word disabled"
        }
    }

    fun triggerHeyBwmTest() {
        wakeWordEngine.simulateWakeWord(null)
    }

    fun toggleVoiceListening() {
        if (_uiState.value == AssistantUiState.LISTENING) {
            stopVoiceListening()
        } else {
            startVoiceListening()
        }
    }

    private fun startVoiceListening() {
        ttsManager.stop()
        wakeWordEngine.stop()
        _currentInterimText.value = ""
        speechRecognizerManager?.currentLanguageCode = selectedLanguage.value
        speechRecognizerManager?.startListening()
    }

    private fun stopVoiceListening() {
        speechRecognizerManager?.stopListening()
        _uiState.value = AssistantUiState.IDLE
        _currentInterimText.value = ""
        _currentStatusMessage.value = "Ready for commands"
        startWakeWordEngineIfEnabled()
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _uiState.value = AssistantUiState.IDLE
        startWakeWordEngineIfEnabled()
    }

    fun handleUserInput(rawText: String) {
        val query = rawText.trim()
        if (query.isBlank()) return

        activeProcessingJob?.cancel()
        wakeWordEngine.stop()
        speechRecognizerManager?.stopListening()

        activeProcessingJob = viewModelScope.launch {
            _uiState.value = AssistantUiState.PROCESSING
            _currentInterimText.value = ""
            _currentStatusMessage.value = "Processing command..."

            // Save user message to database
            dao.insertMessage(
                ConversationMessage(
                    role = "user",
                    content = query
                )
            )

            // Gather recent history for contextual conversations
            val historyPairs = messages.value.takeLast(6).map { it.role to it.content }

            val agentResponse = withContext(Dispatchers.IO) {
                geminiAgent.processCommand(query, historyPairs)
            }

            if (agentResponse.requiresConfirmation && agentResponse.toolAction != null) {
                _uiState.value = AssistantUiState.IDLE
                _pendingConfirmation.value = ConfirmationDialogData(
                    title = "Confirm Action",
                    message = agentResponse.confirmationPrompt ?: "Are you sure you want to proceed with this action?",
                    onConfirm = {
                        _pendingConfirmation.value = null
                        executeActionAndRespond(agentResponse.toolAction, agentResponse.responseText)
                    },
                    onCancel = {
                        _pendingConfirmation.value = null
                        val cancelMsg = "Action cancelled."
                        viewModelScope.launch {
                            dao.insertMessage(ConversationMessage(role = "assistant", content = cancelMsg))
                            ttsManager.speak(cancelMsg)
                        }
                    }
                )
            } else if (agentResponse.toolAction != null) {
                executeActionAndRespond(agentResponse.toolAction, agentResponse.responseText)
            } else {
                // Pure conversational response
                onAssistantFinished(
                    displayText = agentResponse.responseText,
                    spokenText = agentResponse.responseText,
                    actionType = null,
                    isSuccess = true
                )
            }
        }
    }

    private fun executeActionAndRespond(action: ToolActionRequest, spokenResponse: String) {
        viewModelScope.launch {
            _uiState.value = AssistantUiState.EXECUTING
            _currentStatusMessage.value = "Executing action..."

            val (success, detail, spokenResult) = withContext(Dispatchers.IO) {
                performToolAction(action)
            }

            val finalSpeech = if (spokenResult.isNotBlank()) spokenResult else spokenResponse

            onAssistantFinished(
                displayText = finalSpeech,
                spokenText = finalSpeech,
                actionType = action.javaClass.simpleName,
                actionDetail = detail,
                isSuccess = success
            )
        }
    }

    private suspend fun performToolAction(action: ToolActionRequest): Triple<Boolean, String, String> {
        return when (action) {
            is ToolActionRequest.LaunchApp -> {
                val res = appLauncher.launchAppByName(action.appName)
                dao.insertActionLog(ActionLogItem(toolName = "LaunchApp", parameters = action.appName, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.ToggleFlashlight -> {
                val res = deviceController.toggleFlashlight(action.enable)
                dao.insertActionLog(ActionLogItem(toolName = "ToggleFlashlight", parameters = "enable=${action.enable}", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.AdjustVolume -> {
                val res = deviceController.adjustVolume(action.direction, action.levelPercent)
                dao.insertActionLog(ActionLogItem(toolName = "AdjustVolume", parameters = "${action.direction} ${action.levelPercent ?: ""}", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.GetBatteryStatus -> {
                val res = deviceController.getBatteryStatus()
                dao.insertActionLog(ActionLogItem(toolName = "GetBatteryStatus", parameters = "", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.SetTimer -> {
                val res = deviceController.setTimer(action.seconds, action.label)
                dao.insertActionLog(ActionLogItem(toolName = "SetTimer", parameters = "${action.seconds}s (${action.label})", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.SetAlarm -> {
                val res = deviceController.setAlarm(action.hour, action.minute, action.message)
                dao.insertActionLog(ActionLogItem(toolName = "SetAlarm", parameters = "${action.hour}:${action.minute}", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.OpenSettings -> {
                val res = deviceController.openSettings(action.settingType)
                dao.insertActionLog(ActionLogItem(toolName = "OpenSettings", parameters = action.settingType, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.SaveNote -> {
                dao.insertNote(NoteItem(title = action.title, content = action.content))
                val msg = "Note saved: \"${action.title}\""
                dao.insertActionLog(ActionLogItem(toolName = "SaveNote", parameters = action.title, result = msg, isSuccess = true))
                Triple(true, msg, msg)
            }
            is ToolActionRequest.GetNotes -> {
                val recentNotes = dao.getAllNotes()
                val list = withContext(Dispatchers.IO) {
                    dao.searchNotes("")
                }
                val msg = if (list.isEmpty()) {
                    "You have no saved notes."
                } else {
                    "Here are your latest notes: " + list.take(3).joinToString { it.title }
                }
                Triple(true, msg, msg)
            }
            is ToolActionRequest.DialPhone -> {
                val res = deviceController.openDialer(action.phoneNumber)
                dao.insertActionLog(ActionLogItem(toolName = "DialPhone", parameters = action.phoneNumber, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.OpenUrl -> {
                val res = deviceController.openUrl(action.url)
                dao.insertActionLog(ActionLogItem(toolName = "OpenUrl", parameters = action.url, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.DeviceNavigation -> {
                val success = BwmAccessibilityService.performGlobal(action.action)
                val msg = if (success) {
                    "Performed ${action.action} navigation"
                } else {
                    "To navigate automatically, please enable BWM AI in Accessibility Settings."
                }
                dao.insertActionLog(ActionLogItem(toolName = "DeviceNavigation", parameters = action.action, result = msg, isSuccess = success))
                Triple(success, msg, msg)
            }
        }
    }

    private suspend fun onAssistantFinished(
        displayText: String,
        spokenText: String,
        actionType: String?,
        actionDetail: String? = null,
        isSuccess: Boolean
    ) {
        dao.insertMessage(
            ConversationMessage(
                role = "assistant",
                content = displayText,
                actionType = actionType,
                actionDetail = actionDetail,
                isActionSuccess = isSuccess
            )
        )

        _currentStatusMessage.value = displayText
        _uiState.value = AssistantUiState.SPEAKING
        ttsManager.speak(spokenText)
    }

    fun addManualNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertNote(NoteItem(title = title, content = content))
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNote(note)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllMessages()
            dao.clearActionLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroy()
        wakeWordEngine.stop()
        ttsManager.shutdown()
    }
}
