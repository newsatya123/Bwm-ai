package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppSettingsManager
import com.example.data.remote.GeminiAssistantAgent
import com.example.data.remote.ToolActionRequest
import com.example.engine.actions.AppLauncher
import com.example.engine.actions.DeviceController
import com.example.engine.voice.ModularWakeWordEngine
import com.example.engine.voice.SpeechRecognizerManager
import com.example.engine.voice.TextToSpeechManager
import com.example.engine.voice.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceAssistantService : Service() {

    companion object {
        const val CHANNEL_ID = "bwm_voice_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_START_LISTENING = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "com.example.service.ACTION_TOGGLE_PAUSE"
        const val ACTION_TRIGGER_VOICE = "com.example.service.ACTION_TRIGGER_VOICE"
        const val ACTION_TOGGLE_TORCH = "com.example.service.ACTION_TOGGLE_TORCH"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isListeningPaused = MutableStateFlow(false)
        val isListeningPaused: StateFlow<Boolean> = _isListeningPaused.asStateFlow()

        private val _lastExecutedStatus = MutableStateFlow("Background Listening Active (Say 'Hey BWM')")
        val lastExecutedStatus: StateFlow<String> = _lastExecutedStatus.asStateFlow()

        fun start(context: Context) = startService(context)
        fun stop(context: Context) = stopService(context)

        fun startService(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun togglePause(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeWordEngine: WakeWordDetector? = null
    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var deviceController: DeviceController? = null
    private var appLauncher: AppLauncher? = null
    private var ttsManager: TextToSpeechManager? = null
    private var settingsManager: AppSettingsManager? = null
    private val agent = GeminiAssistantAgent()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        deviceController = DeviceController(this)
        appLauncher = AppLauncher(this)
        ttsManager = TextToSpeechManager(this)
        settingsManager = AppSettingsManager(this)
        wakeWordEngine = ModularWakeWordEngine(this)

        speechRecognizerManager = SpeechRecognizerManager(this) { spokenCommand ->
            handleBackgroundCommand(spokenCommand)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                _isListeningPaused.value = !_isListeningPaused.value
                if (_isListeningPaused.value) {
                    wakeWordEngine?.stop()
                    speechRecognizerManager?.stopListening()
                    _lastExecutedStatus.value = "Listening paused"
                } else {
                    startBackgroundListening()
                    _lastExecutedStatus.value = "Background listening resumed"
                }
                updateNotification()
                return START_STICKY
            }
            ACTION_TRIGGER_VOICE -> {
                // User clicked "Speak" button in notification while inside another app like YouTube
                triggerImmediateVoiceCapture()
                return START_STICKY
            }
            ACTION_TOGGLE_TORCH -> {
                val isOn = DeviceController.isFlashlightOn
                deviceController?.toggleFlashlight(!isOn)
                _lastExecutedStatus.value = if (!isOn) "Flashlight turned ON" else "Flashlight turned OFF"
                updateNotification()
                return START_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithNotification()
                _isServiceRunning.value = true
                _isListeningPaused.value = false
                startBackgroundListening()
            }
        }
        return START_STICKY
    }

    private fun startBackgroundListening() {
        if (_isListeningPaused.value) return

        val customName = settingsManager?.assistantName?.value ?: "BWM AI"
        wakeWordEngine?.setCustomAssistantName(customName)
        wakeWordEngine?.start { attachedCommand ->
            if (!attachedCommand.isNullOrBlank()) {
                handleBackgroundCommand(attachedCommand)
            } else {
                // Instantly capture command silently without any beep or audio disruption
                triggerImmediateVoiceCapture()
            }
        }
    }

    private fun triggerImmediateVoiceCapture() {
        wakeWordEngine?.stop()
        speechRecognizerManager?.startListening()
        _lastExecutedStatus.value = "Listening for your voice command..."
        updateNotification()
    }

    private fun handleBackgroundCommand(command: String) {
        val query = command.trim()
        if (query.isBlank()) {
            startBackgroundListening()
            return
        }

        serviceScope.launch {
            _lastExecutedStatus.value = "Processing: \"$query\"..."
            updateNotification()

            val response = agent.processCommand(query)

            if (response.toolActions.isNotEmpty()) {
                val results = mutableListOf<String>()
                for (action in response.toolActions) {
                    val resultText = executeBackgroundAction(action)
                    results.add(resultText)
                    kotlinx.coroutines.delay(400)
                }
                val spoken = results.joinToString(", aur ")
                _lastExecutedStatus.value = spoken
                updateNotification()
                ttsManager?.speak(spoken)
            } else {
                _lastExecutedStatus.value = response.responseText
                updateNotification()
                ttsManager?.speak(response.responseText)
            }

            // Resume silent background listening loop after speaking finishes
            kotlinx.coroutines.delay(1200)
            if (!_isListeningPaused.value && _isServiceRunning.value) {
                startBackgroundListening()
            }
        }
    }

    private fun executeBackgroundAction(action: ToolActionRequest): String {
        return when (action) {
            is ToolActionRequest.ToggleFlashlight -> {
                val res = deviceController?.toggleFlashlight(action.enable)
                res?.message ?: "Flashlight updated"
            }
            is ToolActionRequest.AdjustVolume -> {
                val res = deviceController?.adjustVolume(action.direction, action.levelPercent)
                res?.message ?: "Volume adjusted"
            }
            is ToolActionRequest.GetBatteryStatus -> {
                val res = deviceController?.getBatteryStatus()
                res?.message ?: "Battery status checked"
            }
            is ToolActionRequest.LaunchApp -> {
                val res = appLauncher?.launchAppByName(action.appName)
                res?.message ?: "Opening ${action.appName}"
            }
            is ToolActionRequest.YouTubeSearch -> {
                val res = deviceController?.openYouTube(action.query, action.autoPlayFirst)
                res?.message ?: "Searching YouTube for ${action.query}"
            }
            is ToolActionRequest.YouTubeControl -> {
                val res = deviceController?.controlYouTube(action.action)
                res?.message ?: "YouTube control ${action.action}"
            }
            is ToolActionRequest.SetTimer -> {
                val res = deviceController?.setTimer(action.seconds, action.label)
                res?.message ?: "Timer set"
            }
            is ToolActionRequest.SetAlarm -> {
                val res = deviceController?.setAlarm(action.hour, action.minute, action.message)
                res?.message ?: "Alarm set"
            }
            is ToolActionRequest.DeviceNavigation -> {
                val success = BwmAccessibilityService.performGlobal(action.action)
                if (success) "Performed ${action.action}" else "Accessibility navigation"
            }
            else -> "Command executed"
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundType
            )
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerVoiceIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_TRIGGER_VOICE
        }
        val triggerVoicePending = PendingIntent.getService(
            this,
            3,
            triggerVoiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleTorchIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        val toggleTorchPending = PendingIntent.getService(
            this,
            4,
            toggleTorchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val assistantName = settingsManager?.assistantName?.value ?: "BWM AI"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$assistantName — Active Assistant")
            .setContentText(_lastExecutedStatus.value)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "🎤 Speak", triggerVoicePending)
            .addAction(0, "⚡ Torch", toggleTorchPending)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        wakeWordEngine?.stop()
        speechRecognizerManager?.stopListening()
        ttsManager?.stop()
        _isServiceRunning.value = false
        _isListeningPaused.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordEngine?.stop()
        speechRecognizerManager?.destroy()
        ttsManager?.shutdown()
        serviceScope.cancel()
        _isServiceRunning.value = false
        _isListeningPaused.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
