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
import com.example.engine.tasks.TaskQueueManager
import com.example.engine.voice.ModularWakeWordEngine
import com.example.engine.voice.SpeechRecognitionState
import com.example.engine.voice.SpeechRecognizerManager
import com.example.engine.voice.TextToSpeechManager
import com.example.engine.voice.WakeWordDetector
import com.example.service.BwmAccessibilityService
import com.example.service.VoiceAssistantService
import android.net.Uri
import com.example.data.ai.AIConfigurationRepository
import com.example.data.ai.AIModelInfo
import com.example.data.ai.AIProviderManager
import com.example.data.ai.AIRequestManager
import com.example.data.ai.APIConnectionTester
import com.example.data.ai.GeminiAIProvider
import com.example.data.ai.ModelRepository
import com.example.data.ai.OfflineNLPProvider
import com.example.data.ai.OxyyAIProvider
import com.example.data.ai.SafeActionPlanner
import com.example.data.local.CustomCommandEntity
import com.example.engine.media.DeviceMediaManager
import com.example.engine.ocr.OcrManager
import com.example.engine.reading.PdfDocumentInfo
import com.example.engine.reading.PdfReadingManager
import com.example.engine.reading.ScreenReadingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

    val voiceGender: StateFlow<String> = settingsManager.voiceGender
    val speechRate: StateFlow<Float> = settingsManager.speechRate
    val speechPitch: StateFlow<Float> = settingsManager.speechPitch
    val spokenConfirmation: StateFlow<Boolean> = settingsManager.spokenConfirmation

    val contactsManager = ContactsManager(context)
    val appLauncher = AppLauncher(context)
    val deviceController = DeviceController(context)
    val ttsManager = TextToSpeechManager(context)
    private val geminiAgent = GeminiAssistantAgent()

    val taskQueueManager = TaskQueueManager(viewModelScope) { action, onConfirm ->
        val (_, _, spoken) = performToolAction(action)
        spoken
    }

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

    private val _pendingContactChoice = MutableStateFlow<ContactDisambiguationData?>(null)
    val pendingContactChoice: StateFlow<ContactDisambiguationData?> = _pendingContactChoice.asStateFlow()

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

    // Screen awareness, OCR & Document reading
    val ocrManager = OcrManager(context)
    val screenReadingManager = ScreenReadingManager(context, ttsManager, viewModelScope)
    val pdfReadingManager = PdfReadingManager(context, ocrManager)

    val isReadingActive: StateFlow<Boolean> = screenReadingManager.isReadingActive
    val isReadingPaused: StateFlow<Boolean> = screenReadingManager.isReadingPaused
    val currentDocInfo: StateFlow<PdfDocumentInfo?> = pdfReadingManager.currentDocInfo
    val currentPdfPage: StateFlow<Int> = pdfReadingManager.currentPage
    val isScreenReadingEnabled: StateFlow<Boolean> = settingsManager.isScreenReadingEnabled
    val whatsappAutoReadOutLoud: StateFlow<Boolean> = settingsManager.whatsappAutoReadOutLoud

    private val _requestOpenPdfPicker = MutableStateFlow(false)
    val requestOpenPdfPicker: StateFlow<Boolean> = _requestOpenPdfPicker.asStateFlow()

    private var activeProcessingJob: Job? = null

    // Custom commands flow
    val customCommands: StateFlow<List<CustomCommandEntity>> = dao.getAllCustomCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Provider Architecture (Oxyy AI + Gemini + Offline Hybrid)
    val aiConfigRepo = AIConfigurationRepository(context)
    val oxyyProvider = OxyyAIProvider(aiConfigRepo)
    val geminiProvider = GeminiAIProvider()
    val offlineProvider = OfflineNLPProvider()
    val aiProviderManager = AIProviderManager(oxyyProvider, geminiProvider, offlineProvider, aiConfigRepo)
    val modelRepository = ModelRepository(oxyyProvider, geminiProvider, aiConfigRepo)
    val safeActionPlanner = SafeActionPlanner()
    val aiConnectionTester = APIConnectionTester(oxyyProvider, geminiProvider, offlineProvider, aiConfigRepo)
    val aiRequestManager = AIRequestManager(context, aiProviderManager, aiConfigRepo, safeActionPlanner)

    val isAiBrainEnabled: StateFlow<Boolean> = aiConfigRepo.isAiBrainEnabled
    val activeAiProvider: StateFlow<String> = aiConfigRepo.activeProvider
    val aiExecutionMode: StateFlow<String> = aiConfigRepo.executionMode
    val oxyyApiKey: StateFlow<String> = aiConfigRepo.oxyyApiKey
    val oxyyBaseUrl: StateFlow<String> = aiConfigRepo.oxyyBaseUrl
    val oxyyModelId: StateFlow<String> = aiConfigRepo.oxyyModelId
    val aiTemperature: StateFlow<Float> = aiConfigRepo.temperature
    val aiMaxTokens: StateFlow<Int> = aiConfigRepo.maxTokens
    val aiTimeoutSeconds: StateFlow<Int> = aiConfigRepo.timeoutSeconds
    val aiFallbackProvider: StateFlow<String> = aiConfigRepo.fallbackProvider

    val cachedAiModels: StateFlow<List<AIModelInfo>> = modelRepository.cachedModels
    val isRefreshingModels: StateFlow<Boolean> = modelRepository.isLoading
    val modelRefreshError: StateFlow<String?> = modelRepository.lastRefreshError

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    // Device media manager for sharing photos, screenshots, and PDFs
    val mediaManager = DeviceMediaManager(context)
    var lastWhatsAppRecipient: String = ""

    init {
        geminiAgent.aiRequestManager = aiRequestManager
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

        ttsManager.preferredGender = settingsManager.voiceGender.value
        ttsManager.speechRate = settingsManager.speechRate.value
        ttsManager.speechPitch = settingsManager.speechPitch.value
        ttsManager.applyVoicePreferences()

        // Track TTS state to reset assistant or continue listening seamlessly
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _uiState.value == AssistantUiState.SPEAKING) {
                    val activePkg = BwmAccessibilityService.getActivePackage()
                    val isYouTubeActive = activePkg.contains("youtube", ignoreCase = true) || BwmAccessibilityService.isYouTubePlaying()

                    if (isYouTubeActive) {
                        // Special requirement: When YouTube video is playing/active, stop continuous listening and only wake up on wake word
                        _uiState.value = AssistantUiState.IDLE
                        _currentStatusMessage.value = "YouTube video active. Say 'Hey ${assistantName.value}' to speak"
                        startWakeWordEngineIfEnabled()
                    } else if (isContinuousListening.value) {
                        _currentStatusMessage.value = "Continuous listening active... Speak your next command"
                        delay(450)
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

    fun setVoiceGender(gender: String) {
        settingsManager.setVoiceGender(gender)
        ttsManager.preferredGender = gender
        ttsManager.applyVoicePreferences()
    }

    fun setSpeechRate(rate: Float) {
        settingsManager.setSpeechRate(rate)
        ttsManager.speechRate = rate
    }

    fun setSpeechPitch(pitch: Float) {
        settingsManager.setSpeechPitch(pitch)
        ttsManager.speechPitch = pitch
    }

    fun setSpokenConfirmation(enabled: Boolean) {
        settingsManager.setSpokenConfirmation(enabled)
    }

    fun previewVoice() {
        ttsManager.previewVoice("Sir, BWM Jarvis voice synthesis is operational and standing by.")
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

    fun onPdfPickerConsumed() {
        _requestOpenPdfPicker.value = false
    }

    fun openPdfFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val res = pdfReadingManager.openPdf(uri, fileName)
            res.fold(
                onSuccess = { info ->
                    val msg = "PDF '${info.title}' open ho gayi hai (${info.totalPages} pages). 'Pehla page padho' ya 'Summary do' bol sakte hain."
                    onAssistantFinished(msg, msg, "OpenPdf", info.title, true)
                },
                onFailure = { err ->
                    val msg = "PDF open karne me error: ${err.message}"
                    onAssistantFinished(msg, msg, "OpenPdf", err.message, false)
                }
            )
        }
    }

    fun setScreenReadingEnabled(enabled: Boolean) {
        settingsManager.setScreenReadingEnabled(enabled)
    }

    fun setWhatsappAutoReadOutLoud(enabled: Boolean) {
        settingsManager.setWhatsappAutoReadOutLoud(enabled)
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

            // Check custom commands first for immediate zero-latency trigger
            val enabledCustomCommands = withContext(Dispatchers.IO) {
                dao.getEnabledCustomCommands()
            }
            val matchedCustomCommand = enabledCustomCommands.firstOrNull { cmd ->
                query.equals(cmd.triggerPhrase.trim(), ignoreCase = true) ||
                query.contains(cmd.triggerPhrase.trim(), ignoreCase = true)
            }
            if (matchedCustomCommand != null) {
                val res = executeCustomCommandAction(matchedCustomCommand)
                onAssistantFinished(
                    displayText = res.second,
                    spokenText = res.second,
                    actionType = "CustomCommand",
                    actionDetail = matchedCustomCommand.triggerPhrase,
                    isSuccess = res.first
                )
                return@launch
            }

            // Contextual chat history
            val historyPairs = messages.value.takeLast(6).map { it.role to it.content }

            val agentResponse = withContext(Dispatchers.IO) {
                geminiAgent.processCommand(query, historyPairs)
            }

            val shouldSkipConfirmation = agentResponse.toolActions.all { action ->
                action is ToolActionRequest.WhatsAppSend && (action.directSend || isWhatsappDirectSend.value)
            }

            if (agentResponse.requiresConfirmation && !shouldSkipConfirmation && agentResponse.toolActions.isNotEmpty()) {
                _uiState.value = AssistantUiState.WAITING_FOR_CONFIRMATION
                val promptText = agentResponse.confirmationPrompt ?: "Sir, kya aap is action ke sath proceed karna chahte hain?"
                _currentStatusMessage.value = promptText
                if (spokenConfirmation.value) {
                    val spokenPrompt = if (agentResponse.toolActions.any { it is ToolActionRequest.WhatsAppSend }) {
                        "Sir, message bhejne se pehle confirmation chahiye."
                    } else {
                        promptText
                    }
                    ttsManager.speak(spokenPrompt)
                }
                _pendingConfirmation.value = ConfirmationDialogData(
                    title = "Confirm Action",
                    message = promptText,
                    onConfirm = {
                        _pendingConfirmation.value = null
                        executeActionsAndRespond(agentResponse.toolActions, agentResponse.responseText)
                    },
                    onCancel = {
                        _pendingConfirmation.value = null
                        _uiState.value = AssistantUiState.IDLE
                        val cancelMsg = "Sir, action cancel kar diya gaya hai."
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
            if (actions.size > 1) {
                taskQueueManager.enqueueTasks(actions)
            }
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
                if (!action.contactName.isNullOrBlank() && contactsManager.hasContactsPermission()) {
                    val matches = contactsManager.searchContacts(action.contactName)
                    if (matches.size > 1) {
                        val promptMsg = "Sir, ${action.contactName} naam ke ${matches.size} contacts mile hain. Kisko call karun?"
                        withContext(Dispatchers.Main) {
                            _uiState.value = AssistantUiState.WAITING_FOR_CONFIRMATION
                            _currentStatusMessage.value = promptMsg
                            ttsManager.speak(promptMsg)
                            _pendingContactChoice.value = ContactDisambiguationData(
                                title = "Select Contact to Call",
                                prompt = promptMsg,
                                contacts = matches,
                                onContactChosen = { chosen ->
                                    _pendingContactChoice.value = null
                                    viewModelScope.launch {
                                        val res = deviceController.openDialer(chosen.normalizedNumber)
                                        val label = "${chosen.name} (${chosen.normalizedNumber})"
                                        dao.insertActionLog(ActionLogItem(toolName = "DialPhone", parameters = label, result = res.message, isSuccess = res.isSuccess))
                                        onAssistantFinished(
                                            displayText = res.message,
                                            spokenText = res.message,
                                            actionType = "DialPhone",
                                            actionDetail = label,
                                            isSuccess = res.isSuccess
                                        )
                                    }
                                },
                                onDismiss = {
                                    _pendingContactChoice.value = null
                                    _uiState.value = AssistantUiState.IDLE
                                }
                            )
                        }
                        Triple(true, promptMsg, promptMsg)
                    } else {
                        val targetNumber = if (matches.isNotEmpty()) matches.first().normalizedNumber else action.phoneNumber
                        val res = deviceController.openDialer(targetNumber)
                        val label = "${action.contactName} ($targetNumber)"
                        dao.insertActionLog(ActionLogItem(toolName = "DialPhone", parameters = label, result = res.message, isSuccess = res.isSuccess))
                        Triple(res.isSuccess, res.message, res.message)
                    }
                } else {
                    val res = deviceController.openDialer(action.phoneNumber)
                    dao.insertActionLog(ActionLogItem(toolName = "DialPhone", parameters = action.phoneNumber, result = res.message, isSuccess = res.isSuccess))
                    Triple(res.isSuccess, res.message, res.message)
                }
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
                    "Sir, performed ${action.action} navigation"
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
                        val errMsg = "Sir, Contacts permission needed to find \"$cleanRecipient\". Kripya Settings se Contacts permission allow karein."
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
                            val msg = "Sir, contact \"$cleanRecipient\" phone me nahi mila. WhatsApp khol kar message prefill kar diya hai."
                            dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = cleanRecipient, result = msg, isSuccess = true))
                            Triple(true, msg, msg)
                        } else if (matches.size > 1) {
                            val promptMsg = "Sir, $cleanRecipient naam ke ${matches.size} contacts mile hain. Kise select karna hai?"
                            withContext(Dispatchers.Main) {
                                _uiState.value = AssistantUiState.WAITING_FOR_CONFIRMATION
                                _currentStatusMessage.value = promptMsg
                                ttsManager.speak(promptMsg)
                                _pendingContactChoice.value = ContactDisambiguationData(
                                    title = "Multiple Contacts Found",
                                    prompt = promptMsg,
                                    contacts = matches,
                                    onContactChosen = { chosen ->
                                        _pendingContactChoice.value = null
                                        viewModelScope.launch {
                                            val res = deviceController.openWhatsAppChat(
                                                phoneNumber = chosen.normalizedNumber,
                                                message = action.message,
                                                autoSend = isDirect,
                                                recipientName = chosen.name
                                            )
                                            dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = "${chosen.name}: ${action.message}", result = res.message, isSuccess = res.isSuccess))
                                            onAssistantFinished(
                                                displayText = res.message,
                                                spokenText = res.message,
                                                actionType = "WhatsAppSend",
                                                actionDetail = "${chosen.name}: ${action.message}",
                                                isSuccess = res.isSuccess
                                            )
                                        }
                                    },
                                    onDismiss = {
                                        _pendingContactChoice.value = null
                                        _uiState.value = AssistantUiState.IDLE
                                    }
                                )
                            }
                            Triple(true, promptMsg, promptMsg)
                        } else {
                            val selected = matches.first()
                            val res = deviceController.openWhatsAppChat(
                                phoneNumber = selected.normalizedNumber,
                                message = action.message,
                                autoSend = isDirect,
                                recipientName = selected.name
                            )
                            dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSend", parameters = "${selected.name}: ${action.message}", result = res.message, isSuccess = res.isSuccess))
                            Triple(res.isSuccess, res.message, res.message)
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
                    val errMsg = "Sir, Contacts permission needed to find \"$cleanRecipient\". Kripya Settings se Contacts allow karein."
                    Triple(false, errMsg, errMsg)
                } else {
                    val matches = contactsManager.searchContacts(cleanRecipient)
                    if (matches.isEmpty()) {
                        val res = deviceController.searchWhatsApp(cleanRecipient)
                        Triple(res.isSuccess, res.message, res.message)
                    } else if (matches.size > 1) {
                        val promptMsg = "Sir, $cleanRecipient naam ke ${matches.size} contacts mile hain. Kiska chat open karun?"
                        withContext(Dispatchers.Main) {
                            _uiState.value = AssistantUiState.WAITING_FOR_CONFIRMATION
                            _currentStatusMessage.value = promptMsg
                            ttsManager.speak(promptMsg)
                            _pendingContactChoice.value = ContactDisambiguationData(
                                title = "Select Contact Chat",
                                prompt = promptMsg,
                                contacts = matches,
                                onContactChosen = { chosen ->
                                    _pendingContactChoice.value = null
                                    viewModelScope.launch {
                                        val res = deviceController.openWhatsAppChat(chosen.normalizedNumber, null, false, chosen.name)
                                        dao.insertActionLog(ActionLogItem(toolName = "WhatsAppOpenChat", parameters = chosen.name, result = res.message, isSuccess = res.isSuccess))
                                        onAssistantFinished(
                                            displayText = res.message,
                                            spokenText = res.message,
                                            actionType = "WhatsAppOpenChat",
                                            actionDetail = chosen.name,
                                            isSuccess = res.isSuccess
                                        )
                                    }
                                },
                                onDismiss = {
                                    _pendingContactChoice.value = null
                                    _uiState.value = AssistantUiState.IDLE
                                }
                            )
                        }
                        Triple(true, promptMsg, promptMsg)
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
            is ToolActionRequest.PlayStoreSearch -> {
                val res = deviceController.openPlayStore(action.appName)
                dao.insertActionLog(ActionLogItem(toolName = "PlayStoreSearch", parameters = action.appName, result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.OpenRecentCalls -> {
                val res = deviceController.openRecentCalls()
                dao.insertActionLog(ActionLogItem(toolName = "OpenRecentCalls", parameters = "", result = res.message, isSuccess = res.isSuccess))
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.QueueControl -> {
                val msg = when (action.command.lowercase()) {
                    "pause" -> taskQueueManager.pauseQueue()
                    "resume" -> taskQueueManager.resumeQueue()
                    "cancel" -> taskQueueManager.cancelCurrentTask()
                    "skip" -> taskQueueManager.skipCurrentTask()
                    "status" -> taskQueueManager.getCurrentStatusSummary()
                    else -> "Queue command: ${action.command}"
                }
                Triple(true, msg, msg)
            }
            is ToolActionRequest.QueryActiveApp -> {
                val res = deviceController.getActiveAppSummary()
                Triple(res.isSuccess, res.message, res.message)
            }
            is ToolActionRequest.ReadScreen -> {
                if (!settingsManager.isScreenReadingEnabled.value) {
                    val msg = "Screen Reading mode Settings me disabled hai. Kripya Settings se use enable karein."
                    Triple(false, msg, msg)
                } else {
                    val msg = if (action.mode == "summary" || action.mode == "explain" || action.mode == "important_points") {
                        screenReadingManager.summarizeScreen(geminiAgent, action.query)
                    } else {
                        screenReadingManager.extractScreenText()
                    }
                    dao.insertActionLog(ActionLogItem(toolName = "ReadScreen", parameters = "mode=${action.mode}", result = msg, isSuccess = true))
                    Triple(true, msg, msg)
                }
            }
            is ToolActionRequest.ReadWhatsAppChat -> {
                if (!action.recipient.isNullOrBlank()) {
                    val cleanRecipient = action.recipient.trim()
                    if (contactsManager.hasContactsPermission()) {
                        val matches = contactsManager.searchContacts(cleanRecipient)
                        if (matches.isNotEmpty()) {
                            deviceController.openWhatsAppChat(matches.first().normalizedNumber, null, false, matches.first().name)
                            delay(1200)
                        }
                    }
                }
                val msg = if (action.query == "summary") {
                    screenReadingManager.summarizeWhatsAppChat(geminiAgent)
                } else {
                    screenReadingManager.readWhatsAppLastMessages(action.count)
                }
                dao.insertActionLog(ActionLogItem(toolName = "ReadWhatsAppChat", parameters = "count=${action.count}", result = msg, isSuccess = true))
                Triple(true, msg, msg)
            }
            is ToolActionRequest.SearchAndSummarizeWeb -> {
                deviceController.openGoogleSearch(action.query)
                val summary = if (action.openFirstResult) {
                    delay(1500)
                    deviceController.controlActiveApp("click_first_result")
                    delay(1500)
                    screenReadingManager.summarizeScreen(geminiAgent, action.query)
                } else {
                    val aiResp = geminiAgent.processCommand("User ne search kiya: '${action.query}'. Is topic ka ek simple aur concise 2-sentence Hindi/Hinglish summary do.")
                    aiResp.responseText
                }
                dao.insertActionLog(ActionLogItem(toolName = "SearchAndSummarizeWeb", parameters = action.query, result = summary, isSuccess = true))
                Triple(true, summary, summary)
            }
            is ToolActionRequest.OpenPdfDocument -> {
                withContext(Dispatchers.Main) {
                    _requestOpenPdfPicker.value = true
                }
                val msg = "PDF document choose karne ke liye file picker open kar diya hai."
                Triple(true, msg, msg)
            }
            is ToolActionRequest.ReadPdf -> {
                val doc = pdfReadingManager.currentDocInfo.value
                if (doc == null) {
                    withContext(Dispatchers.Main) {
                        _requestOpenPdfPicker.value = true
                    }
                    val msg = "Sir, pehle ek PDF choose karein jise padhna hai. Document picker open ho gaya hai."
                    Triple(false, msg, msg)
                } else {
                    val text = when (action.action) {
                        "summary" -> pdfReadingManager.summarizeDocument(geminiAgent)
                        "next" -> {
                            val res = pdfReadingManager.readNextPage()
                            res.getOrElse { "Error: ${it.message}" }
                        }
                        else -> {
                            val targetPage = (action.pageNumber ?: pdfReadingManager.currentPage.value) - 1
                            val res = pdfReadingManager.extractPageText(targetPage)
                            res.getOrElse { "Error: ${it.message}" }
                        }
                    }
                    dao.insertActionLog(ActionLogItem(toolName = "ReadPdf", parameters = "action=${action.action}, page=${action.pageNumber}", result = text, isSuccess = true))
                    Triple(true, text, text)
                }
            }
            is ToolActionRequest.ControlReading -> {
                val msg = when (action.command.lowercase()) {
                    "pause" -> screenReadingManager.pauseReading()
                    "resume" -> screenReadingManager.resumeReading()
                    "stop" -> screenReadingManager.stopReading()
                    "repeat" -> screenReadingManager.repeatLastChunk()
                    "next" -> screenReadingManager.nextChunk()
                    "faster" -> screenReadingManager.adjustSpeed(true)
                    "slower" -> screenReadingManager.adjustSpeed(false)
                    else -> "Reading command: ${action.command}"
                }
                Triple(true, msg, msg)
            }
            is ToolActionRequest.OcrImageOrScreen -> {
                val msg = screenReadingManager.extractScreenText()
                Triple(true, msg, msg)
            }
            is ToolActionRequest.WhatsAppSendMedia -> {
                val media = when (action.mediaType) {
                    "download_pdf" -> mediaManager.getLatestDownloadPdf()
                    "screenshot" -> mediaManager.getLatestScreenshot()
                    else -> mediaManager.getLatestPhoto()
                }
                if (media == null) {
                    val label = when (action.mediaType) {
                        "download_pdf" -> "PDF document"
                        "screenshot" -> "Screenshot"
                        else -> "photo"
                    }
                    val errMsg = "Sir, phone me koi $label nahi mila."
                    dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSendMedia", parameters = action.recipient, result = errMsg, isSuccess = false))
                    Triple(false, errMsg, errMsg)
                } else {
                    val targetName = if (action.recipient.equals("ACTIVE_CONVERSATION", true)) {
                        BwmAccessibilityService.getActiveWhatsAppContactName() ?: (if (lastWhatsAppRecipient.isNotBlank()) lastWhatsAppRecipient else "contact")
                    } else {
                        action.recipient
                    }
                    val success = mediaManager.shareMediaToWhatsApp(media, action.caption ?: "")
                    val msg = if (success) {
                        "WhatsApp par $targetName ke sath ${media.displayName} share karne ke liye send screen open kar diya hai."
                    } else {
                        "WhatsApp par media share karne me error aaya."
                    }
                    dao.insertActionLog(ActionLogItem(toolName = "WhatsAppSendMedia", parameters = "$targetName: ${media.displayName}", result = msg, isSuccess = success))
                    Triple(success, msg, msg)
                }
            }
            is ToolActionRequest.ExecuteCustomCommand -> {
                val cmd = withContext(Dispatchers.IO) {
                    dao.findCustomCommandByTrigger(action.triggerPhrase)
                }
                if (cmd != null) {
                    val res = executeCustomCommandAction(cmd)
                    Triple(res.first, res.second, res.second)
                } else {
                    Triple(false, "Custom command not found", "Custom command nahi mila")
                }
            }
        }
    }

    // --- AI BRAIN SETTINGS CONTROLLERS ---

    fun setAiBrainEnabled(enabled: Boolean) {
        aiConfigRepo.setAiBrainEnabled(enabled)
    }

    fun setActiveAiProvider(provider: String) {
        aiConfigRepo.setActiveProvider(provider)
    }

    fun setAiExecutionMode(mode: String) {
        aiConfigRepo.setExecutionMode(mode)
    }

    fun setOxyyApiKey(key: String) {
        aiConfigRepo.setOxyyApiKey(key)
    }

    fun setOxyyBaseUrl(url: String) {
        aiConfigRepo.setOxyyBaseUrl(url)
    }

    fun setOxyyModelId(model: String) {
        aiConfigRepo.setOxyyModelId(model)
    }

    fun setAiTemperature(temp: Float) {
        aiConfigRepo.setTemperature(temp)
    }

    fun setAiMaxTokens(tokens: Int) {
        aiConfigRepo.setMaxTokens(tokens)
    }

    fun setAiTimeoutSeconds(timeout: Int) {
        aiConfigRepo.setTimeoutSeconds(timeout)
    }

    fun setAiFallbackProvider(provider: String) {
        aiConfigRepo.setFallbackProvider(provider)
    }

    fun clearOxyyConfiguration() {
        aiConfigRepo.clearOxyyConfiguration()
        _connectionTestResult.value = null
    }

    fun refreshAiModels() {
        viewModelScope.launch {
            modelRepository.refreshModels()
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = "Testing connection to ${activeAiProvider.value}..."
            val res = aiConnectionTester.testConnection()
            _isTestingConnection.value = false
            res.fold(
                onSuccess = { msg -> _connectionTestResult.value = "✓ $msg" },
                onFailure = { err -> _connectionTestResult.value = "✗ Error: ${err.message}" }
            )
        }
    }

    // --- CUSTOM COMMANDS CONTROLLER ---

    fun addCustomCommand(
        trigger: String,
        alternatives: String = "",
        actionType: String,
        parameter: String = "",
        message: String = "",
        requiresConfirmation: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = CustomCommandEntity(
                triggerPhrase = trigger.trim(),
                alternativePhrases = alternatives.trim(),
                actionType = actionType.trim(),
                actionParameter = parameter.trim(),
                messagePayload = message.trim(),
                requiresConfirmation = requiresConfirmation,
                isEnabled = true
            )
            dao.insertCustomCommand(cmd)
        }
    }

    fun updateCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCustomCommand(command)
        }
    }

    fun deleteCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCustomCommand(command)
        }
    }

    fun toggleCustomCommand(command: CustomCommandEntity, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCustomCommand(command.copy(isEnabled = isEnabled))
        }
    }

    fun testCustomCommand(command: CustomCommandEntity) {
        handleUserInput(command.triggerPhrase)
    }

    fun exportCustomCommandsJson(): String {
        val list = customCommands.value
        val array = JSONArray()
        for (cmd in list) {
            val obj = JSONObject().apply {
                put("trigger", cmd.triggerPhrase)
                put("alternatives", cmd.alternativePhrases)
                put("actionType", cmd.actionType)
                put("parameter", cmd.actionParameter)
                put("message", cmd.messagePayload)
                put("requiresConfirmation", cmd.requiresConfirmation)
                put("isEnabled", cmd.isEnabled)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importCustomCommandsJson(jsonStr: String): Int {
        return try {
            val array = JSONArray(jsonStr)
            var count = 0
            viewModelScope.launch(Dispatchers.IO) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val trigger = obj.optString("trigger", "")
                    if (trigger.isNotBlank()) {
                        val cmd = CustomCommandEntity(
                            triggerPhrase = trigger,
                            alternativePhrases = obj.optString("alternatives", ""),
                            actionType = obj.optString("actionType", "whatsapp_send"),
                            actionParameter = obj.optString("parameter", ""),
                            messagePayload = obj.optString("message", ""),
                            requiresConfirmation = obj.optBoolean("requiresConfirmation", false),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                        dao.insertCustomCommand(cmd)
                        count++
                    }
                }
            }
            count
        } catch (_: Exception) {
            -1
        }
    }

    private suspend fun executeCustomCommandAction(cmd: CustomCommandEntity): Pair<Boolean, String> {
        return when (cmd.actionType) {
            "whatsapp_send" -> {
                val res = deviceController.openWhatsAppChat(cmd.actionParameter, cmd.messagePayload, false, cmd.actionParameter)
                Pair(res.isSuccess, res.message)
            }
            "launch_app" -> {
                val res = appLauncher.launchAppByName(cmd.actionParameter)
                Pair(res.isSuccess, res.message)
            }
            "youtube_search" -> {
                val res = deviceController.openYouTube(cmd.actionParameter, autoPlayFirst = true)
                Pair(res.isSuccess, res.message)
            }
            "device_action" -> {
                when (cmd.actionParameter.lowercase()) {
                    "flashlight_on" -> Pair(deviceController.toggleFlashlight(true).isSuccess, "Torch on kar diya")
                    "flashlight_off" -> Pair(deviceController.toggleFlashlight(false).isSuccess, "Torch off kar diya")
                    "battery" -> Pair(true, deviceController.getBatteryStatus().message)
                    "volume_up" -> Pair(true, deviceController.adjustVolume("up").message)
                    "volume_down" -> Pair(true, deviceController.adjustVolume("down").message)
                    else -> Pair(false, "Unknown device action")
                }
            }
            "routine" -> {
                deviceController.adjustVolume("mute")
                Pair(true, "Routine '${cmd.triggerPhrase}' activated.")
            }
            else -> Pair(false, "Unsupported command type")
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
        screenReadingManager.stopReading()
        pdfReadingManager.closeCurrentPdf()
    }
}
