package com.example.engine.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.regex.Pattern

data class WakeWordDetectionResult(
    val isDetected: Boolean,
    val matchedPhrase: String? = null,
    val remainingCommand: String? = null
)

interface WakeWordDetector {
    val isListening: StateFlow<Boolean>
    val engineName: String
    fun start(onWakeWordDetected: (String?) -> Unit): Boolean
    fun stop()
    fun isSupported(): Boolean
    fun simulateWakeWord(attachedCommand: String? = null)
}

class ModularWakeWordEngine(private val context: Context) : WakeWordDetector {

    companion object {
        private const val TAG = "WakeWordEngine"

        private val WAKE_WORD_PATTERN = Pattern.compile(
            "\\b(hey|he|hay|hi|hello|ok|okay|sun|suno|हे|अरे|सुनो)\\s+(bwm|bmw|b\\s*w\\s*m|b\\.w\\.m\\.|बी\\s*डब्ल्यू\\s*एम|b\\s*v\\s*m)\\b",
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        )

        fun checkWakeWord(text: String, customAssistantName: String? = null): WakeWordDetectionResult {
            val trimmed = text.trim()

            // 1. Check custom assistant name if provided
            if (!customAssistantName.isNullOrBlank()) {
                val cleanCustom = customAssistantName.trim().lowercase(Locale.ROOT)
                val prefixes = listOf("hey", "hi", "hello", "ok", "okay", "suno", "sun")
                val lower = trimmed.lowercase(Locale.ROOT)

                for (p in prefixes) {
                    val trigger = "$p $cleanCustom"
                    if (lower.startsWith(trigger)) {
                        val remaining = lower.removePrefix(trigger).trim().removePrefix(",").trim()
                        return WakeWordDetectionResult(
                            isDetected = true,
                            matchedPhrase = trigger,
                            remainingCommand = if (remaining.isNotBlank()) remaining else null
                        )
                    }
                }
            }

            val matcher = WAKE_WORD_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                val matched = matcher.group()
                val endIndex = matcher.end()
                val remaining = if (endIndex < trimmed.length) {
                    trimmed.substring(endIndex).trim().removePrefix(",").trim()
                } else null
                return WakeWordDetectionResult(
                    isDetected = true,
                    matchedPhrase = matched,
                    remainingCommand = if (!remaining.isNullOrBlank()) remaining else null
                )
            }

            val lower = trimmed.lowercase(Locale.ROOT)
            val directKeywords = listOf("hey bwm", "hi bwm", "ok bwm", "hello bwm", "hey bmw", "ok bmw", "suno bwm")
            for (keyword in directKeywords) {
                if (lower.startsWith(keyword)) {
                    val remaining = lower.removePrefix(keyword).trim().removePrefix(",").trim()
                    return WakeWordDetectionResult(
                        isDetected = true,
                        matchedPhrase = keyword,
                        remainingCommand = if (remaining.isNotBlank()) remaining else null
                    )
                }
            }

            return WakeWordDetectionResult(isDetected = false)
        }
    }

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    override val engineName: String = "BWM On-Device Voice Trigger ('Hey BWM')"

    private var speechRecognizer: SpeechRecognizer? = null
    private var onDetectedCallback: ((String?) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isShouldBeListening = false

    override fun isSupported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun start(onWakeWordDetected: (String?) -> Unit): Boolean {
        if (!isSupported()) return false
        onDetectedCallback = onWakeWordDetected
        isShouldBeListening = true
        _isListening.value = true

        mainHandler.post {
            startInternalRecognizer()
        }
        return true
    }

    override fun stop() {
        isShouldBeListening = false
        _isListening.value = false
        onDetectedCallback = null
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            destroyRecognizer()
        }
    }

    override fun simulateWakeWord(attachedCommand: String?) {
        onDetectedCallback?.invoke(attachedCommand)
    }

    private fun startInternalRecognizer() {
        if (!isShouldBeListening) return

        try {
            destroyRecognizer()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-US"))
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Wake word recognizer listening started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word recognizer: ${e.message}")
            scheduleRestart(2000)
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun scheduleRestart(delayMs: Long = 800) {
        if (!isShouldBeListening) return
        mainHandler.postDelayed({
            if (isShouldBeListening) {
                startInternalRecognizer()
            }
        }, delayMs)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.d(TAG, "Wake word recognizer error code: $error, restarting loop")
                scheduleRestart(500)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var wakeDetected = false
                var attachedCommand: String? = null

                if (!matches.isNullOrEmpty()) {
                    for (text in matches) {
                        val check = checkWakeWord(text)
                        if (check.isDetected) {
                            wakeDetected = true
                            attachedCommand = check.remainingCommand
                            break
                        }
                    }
                }

                if (wakeDetected) {
                    Log.i(TAG, "Wake word 'Hey BWM' detected! Command: $attachedCommand")
                    onDetectedCallback?.invoke(attachedCommand)
                    scheduleRestart(3500)
                } else {
                    scheduleRestart(300)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    for (text in matches) {
                        val check = checkWakeWord(text)
                        if (check.isDetected) {
                            Log.i(TAG, "Wake word detected in partial result: $text")
                            destroyRecognizer()
                            onDetectedCallback?.invoke(check.remainingCommand)
                            scheduleRestart(3500)
                            return
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
