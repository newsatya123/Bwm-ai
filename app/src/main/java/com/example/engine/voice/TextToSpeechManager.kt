package com.example.engine.voice

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsVoiceOption(
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isMale: Boolean,
    val isNetwork: Boolean
)

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    var speechRate: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            if (isInitialized) tts?.setSpeechRate(field)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            if (isInitialized) tts?.setPitch(field)
        }

    var isTtsEnabled: Boolean = true
    var preferredGender: String = "male" // "male", "female", "default"
    var selectedVoiceName: String? = null

    private var activeCompletionCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            // Try default Indian / English locales
            val hindiResult = tts?.setLanguage(Locale("hi", "IN"))
            if (hindiResult == TextToSpeech.LANG_MISSING_DATA || hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("en", "IN"))
            }

            discoverVoices()
            applyVoicePreferences()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    activeCompletionCallback?.invoke()
                    activeCompletionCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    activeCompletionCallback?.invoke()
                    activeCompletionCallback = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    activeCompletionCallback?.invoke()
                    activeCompletionCallback = null
                }
            })
        } else {
            isInitialized = false
        }
    }

    private fun discoverVoices() {
        if (!isInitialized) return
        try {
            val voices = tts?.voices ?: return
            val list = mutableListOf<TtsVoiceOption>()

            for (v in voices) {
                val name = v.name.lowercase()
                val isMale = name.contains("male") || name.contains("ahp") || name.contains("man") || name.contains("-m-")
                val isNetwork = v.isNetworkConnectionRequired
                val display = "${v.locale.displayLanguage} (${v.locale.country}) ${if (isMale) "Male" else "Female"}"
                list.add(
                    TtsVoiceOption(
                        name = v.name,
                        displayName = display,
                        locale = v.locale,
                        isMale = isMale,
                        isNetwork = isNetwork
                    )
                )
            }
            _availableVoices.value = list
        } catch (_: Exception) {}
    }

    fun applyVoicePreferences() {
        if (!isInitialized) return
        try {
            val voices = tts?.voices ?: return

            if (!selectedVoiceName.isNullOrBlank()) {
                val matched = voices.firstOrNull { it.name.equals(selectedVoiceName, ignoreCase = true) }
                if (matched != null) {
                    tts?.voice = matched
                    return
                }
            }

            if (preferredGender.equals("male", ignoreCase = true)) {
                // Seek a male voice for en-IN or hi-IN or US
                val maleVoice = voices.firstOrNull { v ->
                    val n = v.name.lowercase()
                    (n.contains("male") || n.contains("ahp") || n.contains("sfg")) &&
                            (v.locale.language == "hi" || v.locale.language == "en")
                } ?: voices.firstOrNull { v ->
                    v.name.lowercase().contains("male")
                }

                if (maleVoice != null) {
                    tts?.voice = maleVoice
                } else {
                    // Jarvis male acoustic tone fallback: slightly lower pitch (0.88f) provides crisp resonant male timbre
                    tts?.setPitch(0.88f)
                }
            } else if (preferredGender.equals("female", ignoreCase = true)) {
                val femaleVoice = voices.firstOrNull { v ->
                    val n = v.name.lowercase()
                    (n.contains("female") || n.contains("cfc")) &&
                            (v.locale.language == "hi" || v.locale.language == "en")
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                } else {
                    tts?.setPitch(1.1f)
                }
            }
        } catch (_: Exception) {}
    }

    fun setLanguage(locale: Locale) {
        if (isInitialized) {
            tts?.setLanguage(locale)
            applyVoicePreferences()
        }
    }

    fun speak(text: String, onCompletion: (() -> Unit)? = null) {
        if (!isTtsEnabled || !isInitialized) {
            onCompletion?.invoke()
            return
        }

        val cleanedText = text
            .replace(Regex("https?://\\S+"), "web link")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        if (cleanedText.isBlank()) {
            onCompletion?.invoke()
            return
        }

        stop()
        activeCompletionCallback = onCompletion

        val utteranceId = "BWM_JARVIS_${System.currentTimeMillis()}"
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun previewVoice(sampleText: String = "Sir, BWM Jarvis voice synthesis is operational and standing by.") {
        speak(sampleText)
    }

    fun stop() {
        if (isInitialized) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
        _isSpeaking.value = false
        activeCompletionCallback = null
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
