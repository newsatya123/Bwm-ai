package com.example.engine.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

import android.content.ComponentName
import com.example.data.local.SttProvider

sealed class SpeechRecognitionState {
    object Idle : SpeechRecognitionState()
    object Ready : SpeechRecognitionState()
    object Listening : SpeechRecognitionState()
    data class PartialResult(val text: String) : SpeechRecognitionState()
    data class FinalResult(val text: String) : SpeechRecognitionState()
    data class Error(val message: String, val errorCode: Int) : SpeechRecognitionState()
}

class SpeechRecognizerManager(
    private val context: Context,
    private val onResultReceived: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _recognitionState = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    var currentLanguageCode: String = "en-IN"
    var isContinuousMode: Boolean = false
    var sttProvider: SttProvider = SttProvider.SYSTEM_DEFAULT

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        stopListening()

        if (!isRecognitionAvailable()) {
            _recognitionState.value = SpeechRecognitionState.Error(
                "Speech recognition service is not available on this device. You can type commands below.",
                -1
            )
            return
        }

        try {
            speechRecognizer = when (sttProvider) {
                SttProvider.GOOGLE_VOICE -> {
                    try {
                        val googleComp = ComponentName(
                            "com.google.android.googlequicksearchbox",
                            "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
                        )
                        SpeechRecognizer.createSpeechRecognizer(context, googleComp)
                    } catch (_: Exception) {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                }
                else -> {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            }.apply {
                setRecognitionListener(createListener())
            }

            var hasDispatchedResult = false

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)

                if (sttProvider == SttProvider.OFFLINE_HYBRID) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra("android.speech.extra.PREFER_OFFLINE", true)
                }
            }

            speechRecognizer?.startListening(intent)
            _recognitionState.value = SpeechRecognitionState.Ready
        } catch (e: Exception) {
            _recognitionState.value = SpeechRecognitionState.Error("Failed to initialize microphone: ${e.message}", -1)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _audioRmsDb.value = 0f
        if (_recognitionState.value is SpeechRecognitionState.Listening || _recognitionState.value is SpeechRecognitionState.Ready) {
            _recognitionState.value = SpeechRecognitionState.Idle
        }
    }

    fun destroy() {
        stopListening()
    }

    private var hasDispatchedResult = false

    private fun createListener(): RecognitionListener {
        hasDispatchedResult = false
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _recognitionState.value = SpeechRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                _recognitionState.value = SpeechRecognitionState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _audioRmsDb.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _audioRmsDb.value = 0f
            }

            override fun onError(error: Int) {
                _audioRmsDb.value = 0f
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client-side recognition issue"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error occurred"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Speech recognition error ($error)"
                }
                android.util.Log.d("SpeechRecognizer", "Recognition error ($error): $errorMessage")
                _recognitionState.value = SpeechRecognitionState.Error(errorMessage, error)
            }

            override fun onResults(results: Bundle?) {
                _audioRmsDb.value = 0f
                if (hasDispatchedResult) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var recognizedText = matches?.firstOrNull()?.trim() ?: ""

                val wakeCheck = ModularWakeWordEngine.checkWakeWord(recognizedText)
                if (wakeCheck.isDetected && !wakeCheck.remainingCommand.isNullOrBlank()) {
                    recognizedText = wakeCheck.remainingCommand
                }

                if (recognizedText.isNotBlank()) {
                    hasDispatchedResult = true
                    _recognitionState.value = SpeechRecognitionState.FinalResult(recognizedText)
                    onResultReceived(recognizedText)
                } else {
                    _recognitionState.value = SpeechRecognitionState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _recognitionState.value = SpeechRecognitionState.PartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
