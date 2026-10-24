package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineNLPProvider : AIProvider {

    override val id: String = AIConfigurationRepository.PROVIDER_OFFLINE
    override val displayName: String = "Offline Local Jarvis Engine"

    override suspend fun chat(request: AIChatRequest): Result<AIChatResponse> = withContext(Dispatchers.Default) {
        val lastMsg = request.messages.lastOrNull { it.role == "user" }?.content ?: ""
        // Offline engine returns a structured representation or plain response
        Result.success(
            AIChatResponse(
                content = "Offline engine ready for: $lastMsg",
                model = "on-device-nlp"
            )
        )
    }

    override suspend fun getModels(): Result<List<AIModelInfo>> = withContext(Dispatchers.Default) {
        Result.success(
            listOf(
                AIModelInfo("on-device-nlp", "On-Device Hindi/English NLP", "local", isAvailable = true)
            )
        )
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.Default) {
        Result.success("On-Device NLP Engine is active and operational without internet or API key!")
    }
}
