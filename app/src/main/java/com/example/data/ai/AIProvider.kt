package com.example.data.ai

data class AIChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class AIChatRequest(
    val messages: List<AIChatMessage>,
    val model: String,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String? = null
)

data class AIChatResponse(
    val content: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val model: String = ""
)

data class AIModelInfo(
    val id: String,
    val name: String,
    val ownedBy: String = "",
    val contextWindow: Int? = null,
    val isAvailable: Boolean = true
)

interface AIProvider {
    val id: String
    val displayName: String

    suspend fun chat(request: AIChatRequest): Result<AIChatResponse>
    suspend fun getModels(): Result<List<AIModelInfo>>
    suspend fun testConnection(): Result<String>
}
