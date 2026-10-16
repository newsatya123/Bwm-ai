package com.example.engine.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var speechRate: Float = 1.0f
    var speechPitch: Float = 1.0f
    var isTtsEnabled: Boolean = true

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            // Try setting Hindi or Indian English or default
            val hindiResult = tts?.setLanguage(Locale("hi", "IN"))
            if (hindiResult == TextToSpeech.LANG_MISSING_DATA || hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
        } else {
            isInitialized = false
        }
    }

    fun setLanguage(locale: Locale) {
        if (isInitialized) {
            tts?.setLanguage(locale)
        }
    }

    fun speak(text: String, onCompletion: (() -> Unit)? = null) {
        if (!isTtsEnabled || !isInitialized) {
            onCompletion?.invoke()
            return
        }

        // Clean text of markdown or URLs for better speech
        val cleanedText = text
            .replace(Regex("https?://\\S+"), "web link")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        if (cleanedText.isBlank()) return

        stop()

        val utteranceId = "BWM_TTS_${System.currentTimeMillis()}"
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
