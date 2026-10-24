package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.remote.AgentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIRequestManager(
    private val context: Context,
    private val providerManager: AIProviderManager,
    private val configRepo: AIConfigurationRepository,
    private val actionPlanner: SafeActionPlanner
) {
    companion object {
        private const val TAG = "AIRequestManager"

        val SYSTEM_PROMPT = """
            You are BWM AI (Jarvis), an autonomous native Android Personal Phone Assistant.
            You understand Hindi, Hinglish, and English fluently.
            Keep spoken conversational responses concise, clear, and natural (1-2 sentences).

            When the user requests an Android device action, respond with a JSON action plan block formatted as:
            {
              "thought": "brief explanation",
              "response": "short spoken response in Hindi/Hinglish/English",
              "intent": "<intent_name>",
              "target_app": "<optional package/app>",
              "recipient": "<contact name or number>",
              "message": "<message text>",
              "app_name": "<app name>",
              "direction": "up|down|mute|max",
              "enable": true|false,
              "seconds": <number>,
              "query": "<search query>",
              "requires_confirmation": true|false,
              "confidence": 0.95
            }

            Supported intents:
            - send_message (recipient, message)
            - send_media (recipient, media_type: "latest_photo"|"screenshot"|"download_pdf", caption)
            - launch_app (app_name)
            - toggle_flashlight (enable: true|false)
            - adjust_volume (direction: "up"|"down"|"mute"|"max", level_percent)
            - get_battery_status
            - set_timer (seconds, label)
            - set_alarm (hour, minute, message)
            - dial_phone (contact_name, phone_number)
            - youtube_search (query, auto_play: true)
            - youtube_control (action: "play"|"pause"|"next")
            - save_note (title, content)
            - read_screen (mode: "read"|"summary"|"important_points", query)
            - read_whatsapp_chat (recipient, count)
            - search_and_summarize_web (query, open_first_result: true|false)
            - open_pdf_document (file_name)
            - read_pdf (page_number, action: "read"|"next"|"summary")
            - control_reading (command: "pause"|"resume"|"stop"|"repeat"|"next"|"faster"|"slower")

            If the user asks a general question or knowledge query, answer conversationally without JSON or with {"response": "..."}.
        """.trimIndent()
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun processCommand(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): AgentResponse = withContext(Dispatchers.IO) {
        val executionMode = configRepo.executionMode.value

        // Check if Offline Only is enforced
        if (executionMode == AIConfigurationRepository.MODE_OFFLINE_ONLY || !configRepo.isAiBrainEnabled.value) {
            Log.d(TAG, "Offline mode active. Processing locally.")
            return@withContext nullResponseForOffline(prompt)
        }

        // Automatic Mode: Try local matching first for instant response
        if (executionMode == AIConfigurationRepository.MODE_AUTO) {
            if (actionPlanner.canHandleLocally(prompt)) {
                Log.d(TAG, "Prompt can be handled locally with 100% confidence: '$prompt'")
                return@withContext nullResponseForOffline(prompt)
            }
        }

        // Check network connectivity before sending cloud request
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Internet unavailable, routing to offline intent engine.")
            return@withContext nullResponseForOffline(prompt)
        }

        val primaryProvider = providerManager.getActiveProvider()

        // Prepare request
        val messages = mutableListOf<AIChatMessage>()
        for ((role, text) in history.takeLast(6)) {
            messages.add(AIChatMessage(if (role.equals("user", true)) "user" else "assistant", text))
        }
        messages.add(AIChatMessage("user", prompt))

        val chatRequest = AIChatRequest(
            messages = messages,
            model = configRepo.oxyyModelId.value,
            temperature = configRepo.temperature.value,
            maxTokens = configRepo.maxTokens.value,
            systemPrompt = SYSTEM_PROMPT
        )

        Log.d(TAG, "Dispatching request to primary provider: ${primaryProvider.displayName}")
        val result = primaryProvider.chat(chatRequest)

        if (result.isSuccess) {
            val response = result.getOrThrow()
            return@withContext actionPlanner.parseModelResponse(response.content, prompt)
        } else {
            val err = result.exceptionOrNull()
            Log.w(TAG, "Primary provider failed: ${err?.message}. Attempting fallback.")

            val fallbackProvider = providerManager.getFallbackProvider()
            if (fallbackProvider.id != primaryProvider.id && fallbackProvider.id != AIConfigurationRepository.PROVIDER_OFFLINE) {
                val fallbackResult = fallbackProvider.chat(chatRequest)
                if (fallbackResult.isSuccess) {
                    val fallbackResp = fallbackResult.getOrThrow()
                    return@withContext actionPlanner.parseModelResponse(fallbackResp.content, prompt)
                }
            }

            // If fallback also failed or is offline, return offline fallback
            Log.i(TAG, "Cloud providers failed, defaulting to offline engine: ${err?.message}")
            return@withContext nullResponseForOffline(prompt)
        }
    }

    private fun nullResponseForOffline(prompt: String): AgentResponse {
        // Returns null tool action so caller invokes the local offline NLP engine seamlessly
        return AgentResponse(
            responseText = "",
            toolActions = emptyList(),
            requiresConfirmation = false
        )
    }
}
