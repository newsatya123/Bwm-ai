package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActionLogItem
import com.example.data.local.AppSettingsManager
import com.example.data.local.AssistantDatabase
import com.example.data.local.AssistantLogoTheme
import com.example.data.local.ConversationMessage
import com.example.data.local.NoteItem
import com.example.data.local.SttProvider
import com.example.data.remote.GeminiAssistantAgent
import com.example.data.remote.ToolActionRequest
import com.example.engine.actions.AppLauncher
import com.example.engine.actions.ContactInfo
import com.example.engine.actions.ContactsManager
import com.example.engine.actions.DeviceController
import com.example.engine.actions.InstalledAppInfo
import com.example.engine.voice.ModularWakeWordEngine
import com.example.engine.voice.SpeechRecognitionState
import com.example.engine.voice.SpeechRecognizerManager
import com.example.engine.voice.TextToSpeechManager
import com.example.engine.voice.WakeWordDetector
import com.example.service.BwmAccessibilityService
import com.example.service.VoiceAssistantService
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

    val settingsManager = AppSettingsManager(context)
    val assistantName: StateFlow<String> = settingsManager.assistantName
    val logoTheme: StateFlow<AssistantLogoTheme> = settingsManager.logoTheme
    val isContinuousListening: StateFlow<Boolean> = settingsManager.isContinuousListening
    val autoBackgroundListening: StateFlow<Boolean> = settingsManager.autoBackgroundListeningOnAppLaunch
    val isWakeWordEnabled: StateFlow<Boolean> = settingsManager.isWakeWordEnabled
    val micSensitivity: StateFlow<Float> = settingsManager.micSensitivity
    val sttProvider: StateFlow<SttProvider> = settingsManager.sttProvider
    val isWhatsappDirectSend: StateFlow<Boolean> = settingsManager.isWhatsappDirectSend

    val contactsManager = ContactsManager(context)
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

        viewModelScope.launch {
            micSensitivity.collect { sens ->
                wakeWordEngine.setSensitivity(sens)
            }
        }

        viewModelScope.launch {
            sttProvider.collect { prov ->
                speechRecognizerManager?.sttProvider = prov
            }
        }

        // Track TTS state to reset assistant or continue listening seamlessly
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _uiState.value == AssistantUiState.SPEAKING) {
                    if (isContinuousListening.value) {
                        _currentStatusMessage.value = "Continuous listening active... Speak your next command"
                        kotlinx.coroutines.delay(450)
                        startVoiceListening()
                    } else {
                        _uiState.value = AssistantUiState.IDLE
                        _currentStatusMessage.value = "Listening for '${assistantName.value}' or tap microphone"
                        startWakeWordEngineIfEnabled()
                    }
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizerManager = SpeechRecognizerManager(context) { finalTranscription ->
            handleUserInput(finalTranscription)
        }.apply {
            isContinuousMode = isContinuousListening.value
        }

        viewModelScope.launch {
            speechRecognizerManager?.recognitionState?.collect { recState ->
                when (recState) {
                    is SpeechRecognitionState.Listening -> {
                        _uiState.value = AssistantUiState.CAPTURING_COMMAND
                        _currentStatusMessage.value = if (isContinuousListening.value) {
                            "Listening for command... Speak in Hindi or English"
                        } else {
                            "Listening... Speak in Hindi or English"
                        }
                    }
                    is SpeechRecognitionState.PartialResult -> {
                        _currentInterimText.value = recState.text
                    }
                    is SpeechRecognitionState.FinalResult -> {
                        _currentInterimText.value = ""
                    }
                    is SpeechRecognitionState.Error -> {
                        _currentInterimText.value = ""
                        _uiState.value = AssistantUiState.IDLE
                        _currentStatusMessage.value = "Listening for '${assistantName.value}' or tap microphone"
                        startWakeWordEngineIfEnabled()
                    }
                    is SpeechRecognitionState.Idle -> {
                        if (_uiState.value == AssistantUiState.CAPTURING_COMMAND) {
                            _uiState.value = AssistantUiState.IDLE
                            startWakeWordEngineIfEnabled()
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
        if (isWakeWordEnabled.value && (_uiState.value == AssistantUiState.IDLE || _uiState.value == AssistantUiState.LISTENING_FOR_WAKE_WORD)) {
            _uiState.value = AssistantUiState.LISTENING_FOR_WAKE_WORD
            wakeWordEngine.setCustomAssistantName(assistantName.value)
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
        settingsManager.setWakeWordEnabled(enabled)
        if (enabled) {
            startWakeWordEngineIfEnabled()
            _currentStatusMessage.value = "Wake-word enabled ('Hey ${assistantName.value}')"
        } else {
            wakeWordEngine.stop()
            _uiState.value = AssistantUiState.IDLE
            _currentStatusMessage.value = "Wake-word disabled"
        }
    }

    fun setMicSensitivity(sensitivity: Float) {
        settingsManager.setMicSensitivity(sensitivity)
    }

    fun setSttProvider(provider: SttProvider) {
        settingsManager.setSttProvider(provider)
    }

    fun setWhatsappDirectSend(enabled: Boolean) {
        settingsManager.setWhatsappDirectSend(enabled)
    }

    fun triggerHeyBwmTest() {
        wakeWordEngine.simulateWakeWord(null)
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isListening) {
            stopVoiceListening()
        } else {
            startVoiceListening()
        }
    }

    fun startVoiceListening() {
        ttsManager.stop()
        wakeWordEngine.stop()
        _uiState.value = AssistantUiState.CAPTURING_COMMAND
        _currentInterimText.value = ""
        speechRecognizerManager?.isContinuousMode = isContinuousListening.value
        speechRecognizerManager?.currentLanguageCode = selectedLanguage.value
        speechRecognizerManager?.startListening()
    }

    fun stopVoiceListening() {
        speechRecognizerManager?.stopListening()
        _uiState.value = AssistantUiState.IDLE
        _currentInterimText.value = ""
        _currentStatusMessage.value = "Ready for commands"
        startWakeWordEngineIfEnabled()
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _uiState.value = AssistantUiState.IDLE
        if (isContinuousListening.value) {
            startVoiceListening()
        } else {
            startWakeWordEngineIfEnabled()
        }
    }

    fun setContinuousListening(enabled: Boolean) {
        settingsManager.setContinuousListening(enabled)
        speechRecognizerManager?.isContinuousMode = enabled
    }

    fun updateAssistantName(name: String) {
        settingsManager.setAssistantName(name)
    }

    fun updateLogoTheme(theme: AssistantLogoTheme) {
        settingsManager.setLogoTheme(theme)
    }

    fun setAutoBackgroundListening(enabled: Boolean) {
        settingsManager.setAutoBackgroundListening(enabled)
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

            // Contextual chat history
            val historyPairs = messages.value.takeLast(6).map { it.role to it.content }

            val agentResponse = withContext(Dispatchers.IO) {
                geminiAgent.processCommand(query, historyPairs)
            }

            val shouldSkipConfirmation = agentResponse.toolActions.all { action ->
                action is ToolActionRequest.WhatsAppSend && (action.directSend || isWhatsappDirectSend.value)
            }

            if (agentResponse.requiresConfirmation && !shouldSkipConfirmation && agentResponse.toolActions.isNotEmpty()) {
                _uiState.value = AssistantUiState.IDLE
                _pendingConfirmation.value = ConfirmationDialogData(
                    title = "Confirm Action",
                    message = agentResponse.confirmationPrompt ?: "Are you sure you want to proceed with this action?",
                    onConfirm = {
                        _pendingConfirmation.value = null
                        executeActionsAndRespond(agentResponse.toolActions, agentResponse.responseText)
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
            } else if (agentResponse.toolActions.isNotEmpty()) {
                // Multi-task or single task execution
                executeActionsAndRespond(agentResponse.toolActions, agentResponse.responseText)
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

    private fun executeActionsAndRespond(actions: List<ToolActionRequest>, defaultSpokenResponse: String) {
        if (actions.isEmpty()) {
            viewModelScope.launch {
                onAssistantFinished(
                    displayText = defaultSpokenResponse,
                    spokenText = defaultSpokenResponse,
                    actionType = null,
                    isSuccess = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = AssistantUiState.EXECUTING
            val speechResults = mutableListOf<String>()
            val detailsList = mutableListOf<String>()
            var allSuccess = true

            // Sequential multi-task execution loop
            for ((index, action) in actions.withIndex()) {
                _currentStatusMessage.value = "Executing task ${index + 1} of ${actions.size}..."
                val (success, detail, spokenResult) = withContext(Dispatchers.IO) {
                    performToolAction(action)
                }
                if (!success) allSuccess = false
                if (spokenResult.isNotBlank()) speechResults.add(spokenResult)
                if (detail.isNotBlank()) detailsList.add(detail)

                if (index < actions.size - 1) {
                    kotlinx.coroutines.delay(400)
                }
            }

            val finalSpeech = if (speechResults.isNotEmpty()) {
                speechResults.joinToString(", aur ")
            } else defaultSpokenResponse

            val combinedTypes = actions.joinToString(" + ") { it.javaClass.simpleName }

            onAssistantFinished(
                displayText = finalSpeech,
                spokenText = finalSpeech,
                actionType = combinedTypes,
                actionDetail = detailsList.joinToString(" | "),
                isSuccess = allSuccess
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
            is ToolActionRequest.YouTubeSearch -> {
                val res = deviceController.openYouTube(action.query, action.autoPlayFirst)
                dao.insertActionLog(ActionLogItem(toolName = "YouTubeSearch", parameters = "query=${action.query}, autoPlay=${action.autoPlayFirst}", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.YouTubeControl -> {
                val res = deviceController.controlYouTube(action.action)
                dao.insertActionLog(ActionLogItem(toolName = "YouTubeControl", parameters = action.action, result = res.message, isSuccess = res.isSuccess))
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
            is ToolActionRequest.WhatsAppSend -> {
                val isDirect = action.directSend || isWhatsappDirectSend.value
                val cleanRecipient = action.recipient.trim()

                // Check if recipient is already a raw phone number (digits)
                val isRawNumber = cleanRecipient.replace(Regex("[^0-9]"), "").length >= 7
                if (isRawNumber) {
                    val res = deviceController.openWhatsAppChat(
                        phoneNumber = cleanRecipient,
                        message = action.message,
                        autoSend = isDirect,
                        recipientName = cleanRecipient
                    )
                    dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = "$cleanRecipient: ${action.message}", result = res.message, isSuccess = res.isSuccess))
                    Triple(res.isSuccess, res.message, res.message)
                } else {
                    if (!contactsManager.hasContactsPermission()) {
                        val errMsg = "Contacts permission needed to find \"$cleanRecipient\". Kripya Settings se Contacts permission allow karein."
                        dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = cleanRecipient, result = errMsg, isSuccess = false))
                        Triple(false, errMsg, errMsg)
                    } else {
                        val matches = contactsManager.searchContacts(cleanRecipient)
                        if (matches.isEmpty()) {
                            val res = deviceController.openWhatsAppChat(
                                phoneNumber = "",
                                message = action.message,
                                autoSend = false,
                                recipientName = cleanRecipient
                            )
                            val msg = "Contact \"$cleanRecipient\" phone me nahi mila. WhatsApp khol kar message prefill kar diya hai."
                            dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = cleanRecipient, result = msg, isSuccess = true))
                            Triple(true, msg, msg)
                        } else {
                            val selected = matches.first()
                            val res = deviceController.openWhatsAppChat(
                                phoneNumber = selected.normalizedNumber,
                                message = action.message,
                                autoSend = isDirect,
                                recipientName = selected.name
                            )
                            val ambiguityNote = if (matches.size > 1) " (Multiple matches, selected ${selected.name})" else ""
                            val finalMsg = "${res.message}$ambiguityNote"
                            dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = "${selected.name}: ${action.message}", result = finalMsg, isSuccess = res.isSuccess))
                            Triple(res.isSuccess, finalMsg, finalMsg)
                        }
                    }
                }
            }
            is ToolActionRequest.WhatsAppOpenChat -> {
                val cleanRecipient = action.recipient.trim()
                val isRawNumber = cleanRecipient.replace(Regex("[^0-9]"), "").length >= 7
                if (isRawNumber) {
                    val res = deviceController.openWhatsAppChat(cleanRecipient, null, false, cleanRecipient)
                    dao.insertActionLog(ActionLogItem(toolName = "WhatsAppOpenChat", parameters = cleanRecipient, result = res.message, isSuccess = res.isSuccess))
                    Triple(res.isSuccess, res.message, res.message)
                } else if (!contactsManager.hasContactsPermission()) {
                    val errMsg = "Contacts permission needed to find \"$cleanRecipient\". Kripya Settings se Contacts allow karein."
                    Triple(false, errMsg, errMsg)
                } else {
                    val matches = contactsManager.searchContacts(cleanRecipient)
                    if (matches.isEmpty()) {
                        val res = deviceController.searchWhatsApp(cleanRecipient)
                        Triple(res.isSuccess, res.message, res.message)
                    } else {
                        val selected = matches.first()
                        val res = deviceController.openWhatsAppChat(selected.normalizedNumber, null, false, selected.name)
                        dao.insertActionLog(ActionLogItem(toolName = "WhatsAppOpenChat", parameters = selected.name, result = res.message, isSuccess = res.isSuccess))
                        Triple(res.isSuccess, res.message, res.message)
                    }
                }
            }
            is ToolActionRequest.WhatsAppSearch -> {
                val res = deviceController.searchWhatsApp(action.query)
                dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSearch", parameters = action.query, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.ActiveAppContextAction -> {
                val res = deviceController.controlActiveApp(action.action, action.parameter)
                dao.insertActionLog(ActionLogItem(toolName = "ActiveAppContextAction", parameters = "${action.action} (${action.parameter ?: ""})", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
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
