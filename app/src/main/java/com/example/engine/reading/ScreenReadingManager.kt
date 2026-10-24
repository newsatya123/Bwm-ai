package com.example.engine.reading

import android.content.Context
import android.util.Log
import com.example.data.remote.GeminiAssistantAgent
import com.example.engine.voice.TextToSpeechManager
import com.example.service.BwmAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenReadingManager(
    private val context: Context,
    private val ttsManager: TextToSpeechManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ScreenReadingManager"
    }

    private val _isReadingActive = MutableStateFlow(false)
    val isReadingActive: StateFlow<Boolean> = _isReadingActive.asStateFlow()

    private val _isReadingPaused = MutableStateFlow(false)
    val isReadingPaused: StateFlow<Boolean> = _isReadingPaused.asStateFlow()

    private val _currentReadingText = MutableStateFlow("")
    val currentReadingText: StateFlow<String> = _currentReadingText.asStateFlow()

    private var readingChunks: List<String> = emptyList()
    private var currentChunkIndex = 0
    private var readingJob: Job? = null

    // --- UNIVERSAL READING ENGINE CONTROLS ---

    fun startReading(text: String, onFinished: (() -> Unit)? = null) {
        stopReading()
        if (text.isBlank()) return

        val cleanText = text.trim()
        _currentReadingText.value = cleanText
        // Split text into readable sentences / paragraphs (around 80-150 chars per chunk)
        readingChunks = splitIntoReadingChunks(cleanText)
        currentChunkIndex = 0
        _isReadingActive.value = true
        _isReadingPaused.value = false

        readingJob = scope.launch(Dispatchers.Main) {
            while (isActive && currentChunkIndex < readingChunks.size) {
                if (_isReadingPaused.value) {
                    delay(300)
                    continue
                }

                val chunk = readingChunks[currentChunkIndex]
                Log.d(TAG, "Reading chunk [${currentChunkIndex + 1}/${readingChunks.size}]: $chunk")
                ttsManager.speak(chunk)

                // Wait until TTS completes this chunk
                while (isActive && ttsManager.isSpeaking.value && !_isReadingPaused.value) {
                    delay(150)
                }

                if (!_isReadingPaused.value) {
                    currentChunkIndex++
                    delay(400) // Natural conversational pause between sentences
                }
            }

            _isReadingActive.value = false
            _isReadingPaused.value = false
            onFinished?.invoke()
        }
    }

    fun pauseReading(): String {
        if (!_isReadingActive.value) return "Reading abhi chal nahi rahi hai."
        _isReadingPaused.value = true
        ttsManager.stop()
        return "Reading pause kar di gayi hai. Aap 'continue karo' bol kar dobara sun sakte hain."
    }

    fun resumeReading(): String {
        if (!_isReadingActive.value) return "Koi active reading session nahi hai."
        if (!_isReadingPaused.value) return "Reading already chal rahi hai."
        _isReadingPaused.value = false
        return "Reading continue ki ja rahi hai."
    }

    fun repeatLastChunk(): String {
        if (readingChunks.isEmpty()) return "Repeat karne ke liye koi text nahi hai."
        ttsManager.stop()
        if (currentChunkIndex > 0) currentChunkIndex--
        _isReadingPaused.value = false
        val chunk = readingChunks.getOrNull(currentChunkIndex) ?: ""
        ttsManager.speak(chunk)
        return "Dobara padh raha hoon: $chunk"
    }

    fun nextChunk(): String {
        if (readingChunks.isEmpty()) return "Koi text available nahi hai."
        ttsManager.stop()
        if (currentChunkIndex < readingChunks.size - 1) {
            currentChunkIndex++
            _isReadingPaused.value = false
            val chunk = readingChunks[currentChunkIndex]
            ttsManager.speak(chunk)
            return "Agla paragraph: $chunk"
        }
        return "Aap already aakhri paragraph par hain."
    }

    fun stopReading(): String {
        readingJob?.cancel()
        readingJob = null
        _isReadingActive.value = false
        _isReadingPaused.value = false
        _currentReadingText.value = ""
        ttsManager.stop()
        return "Reading band kar di gayi hai."
    }

    fun adjustSpeed(faster: Boolean): String {
        val current = ttsManager.speechRate
        val newRate = if (faster) (current + 0.2f).coerceAtMost(2.0f) else (current - 0.2f).coerceAtLeast(0.5f)
        ttsManager.speechRate = newRate
        ttsManager.applyVoicePreferences()
        val desc = if (faster) "fast" else "slow"
        return "Reading speed $desc kar di gayi hai (${"%.1f".format(newRate)}x)."
    }

    // --- SCREEN AWARENESS & APP READING ---

    fun extractScreenText(): String {
        val content = BwmAccessibilityService.extractActiveScreenContent()
        if (content == null || content.textNodes.isEmpty()) {
            return "Sir, active screen par readable text nahi mila ya Accessibility Service enabled nahi hai."
        }
        val topTexts = content.textNodes.take(15).joinToString(". ")
        return "Screen par likha hai: $topTexts"
    }

    suspend fun summarizeScreen(agent: GeminiAssistantAgent, query: String? = null): String = withContext(Dispatchers.IO) {
        val content = BwmAccessibilityService.extractActiveScreenContent()
        if (content == null || content.textNodes.isEmpty()) {
            return@withContext "Sir, screen par readable text nahi mila. Kripya ensure karein ki content visible ho."
        }
        val prompt = if (query.isNullOrBlank()) {
            "Sir, active app '${content.title}' ki screen par ye text hai:\n\n${content.rawSummaryText}\n\nIs page ka 2-3 concise Hindi/Hinglish sentences me clear summary do."
        } else {
            "Sir, active app '${content.title}' ki screen par ye text hai:\n\n${content.rawSummaryText}\n\nUser question: '$query'. Is question ka screen content ke basis par concise Hindi/Hinglish me answer do."
        }
        val response = agent.processCommand(prompt)
        response.responseText
    }

    // --- WHATSAPP CONVERSATION READING ---

    fun readWhatsAppLastMessages(count: Int = 1): String {
        val messages = BwmAccessibilityService.extractVisibleWhatsAppMessages(count)
        if (messages.isEmpty()) {
            return "Sir, WhatsApp chat me koi visible message nahi mila. Kripya chat open rakhein."
        }
        if (count == 1) {
            val last = messages.last()
            return "Sir, ${last.sender} ka last message hai: \"${last.message}\""
        }
        val sb = StringBuilder("Sir, last ${messages.size} messages hain:\n")
        messages.forEachIndexed { idx, m ->
            sb.append("${idx + 1}. ${m.sender}: \"${m.message}\"\n")
        }
        return sb.toString().trim()
    }

    suspend fun summarizeWhatsAppChat(agent: GeminiAssistantAgent): String = withContext(Dispatchers.IO) {
        val messages = BwmAccessibilityService.extractVisibleWhatsAppMessages(10)
        if (messages.isEmpty()) {
            return@withContext "Sir, WhatsApp chat me visible messages nahi mile jinhe summarize kiya ja sake."
        }
        val chatLog = messages.joinToString("\n") { "${it.sender}: ${it.message}" }
        val prompt = "Neeche WhatsApp chat ke visible messages diye gaye hain:\n\n$chatLog\n\nIs chat conversation ka 1-2 line me concise Hindi/Hinglish summary do."
        val response = agent.processCommand(prompt)
        response.responseText
    }

    private fun splitIntoReadingChunks(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?\\n])\\s+"))
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        for (s in sentences) {
            val trimmed = s.trim()
            if (trimmed.isBlank()) continue
            if (current.length + trimmed.length > 120 && current.isNotBlank()) {
                chunks.add(current.toString().trim())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(trimmed)
        }
        if (current.isNotBlank()) {
            chunks.add(current.toString().trim())
        }
        return if (chunks.isNotEmpty()) chunks else listOf(text)
    }
}
