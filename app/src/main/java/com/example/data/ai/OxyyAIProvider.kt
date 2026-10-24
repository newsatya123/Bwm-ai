package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OxyyAIProvider(
    private val configRepo: AIConfigurationRepository
) : AIProvider {

    companion object {
        private const val TAG = "OxyyAIProvider"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override val id: String = AIConfigurationRepository.PROVIDER_OXYY
    override val displayName: String = "Oxyy AI"

    private fun getClient(): OkHttpClient {
        val timeout = configRepo.timeoutSeconds.value.toLong().coerceAtLeast(5L)
        return OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun chat(request: AIChatRequest): Result<AIChatResponse> = withContext(Dispatchers.IO) {
        val apiKey = configRepo.oxyyApiKey.value.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Oxyy API key configured nahi hai. Kripya Settings > AI Brain me jakar valid Oxyy API key enter karein.")
            )
        }

        val baseUrl = configRepo.oxyyBaseUrl.value.trim().removeSuffix("/")
        val endpoint = "$baseUrl/chat/completions"

        try {
            val rootJson = JSONObject()
            val modelId = if (request.model.isNotBlank()) request.model else configRepo.oxyyModelId.value
            rootJson.put("model", modelId)

            val messagesArray = JSONArray()

            // System prompt if present
            if (!request.systemPrompt.isNullOrBlank()) {
                val sysMsg = JSONObject().apply {
                    put("role", "system")
                    put("content", request.systemPrompt)
                }
                messagesArray.put(sysMsg)
            }

            for (msg in request.messages) {
                val obj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(obj)
            }
            rootJson.put("messages", messagesArray)

            val temp = request.temperature ?: configRepo.temperature.value
            rootJson.put("temperature", temp.toDouble())

            val maxTokens = request.maxTokens ?: configRepo.maxTokens.value
            if (maxTokens > 0) {
                rootJson.put("max_tokens", maxTokens)
            }

            val requestBody = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "BWM-Jarvis-Android/1.6.0")
                .post(requestBody)
                .build()

            Log.d(TAG, "Sending request to Oxyy AI: model=$modelId, endpoint=$endpoint")
            val response = getClient().newCall(httpRequest).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(code, bodyString)
                Log.e(TAG, "Oxyy AI Error [$code]: $errorMsg")
                return@withContext Result.failure(IOException(errorMsg))
            }

            val respJson = JSONObject(bodyString)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(IOException("Oxyy AI response me koi choices nahi mile."))
            }

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
            val content = messageObj?.optString("content") ?: ""

            val usageObj = respJson.optJSONObject("usage")
            val promptTokens = usageObj?.optInt("prompt_tokens", 0) ?: 0
            val completionTokens = usageObj?.optInt("completion_tokens", 0) ?: 0
            val totalTokens = usageObj?.optInt("total_tokens", 0) ?: (promptTokens + completionTokens)

            Result.success(
                AIChatResponse(
                    content = content,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens,
                    model = respJson.optString("model", modelId)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Oxyy chat call failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getModels(): Result<List<AIModelInfo>> = withContext(Dispatchers.IO) {
        val apiKey = configRepo.oxyyApiKey.value.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Oxyy API key missing hai. Kripya pehle API key enter karein.")
            )
        }

        val baseUrl = configRepo.oxyyBaseUrl.value.trim().removeSuffix("/")
        val endpoint = "$baseUrl/models"

        try {
            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("User-Agent", "BWM-Jarvis-Android/1.6.0")
                .get()
                .build()

            val response = getClient().newCall(httpRequest).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(code, bodyString)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val models = mutableListOf<AIModelInfo>()
            val root = JSONObject(bodyString)
            val dataArray = root.optJSONArray("data") ?: JSONArray()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val modelId = item.optString("id", "")
                if (modelId.isNotBlank()) {
                    val ownedBy = item.optString("owned_by", "oxyy")
                    val contextWindow = item.optInt("context_window", -1).takeIf { it > 0 }
                    models.add(
                        AIModelInfo(
                            id = modelId,
                            name = modelId,
                            ownedBy = ownedBy,
                            contextWindow = contextWindow,
                            isAvailable = true
                        )
                    )
                }
            }

            if (models.isEmpty()) {
                // If endpoint returns empty list, supply standard default models
                models.add(AIModelInfo("oxyy-default", "Oxyy Default Chat", "oxyy"))
                models.add(AIModelInfo("gpt-4o-mini", "GPT-4o Mini (via Oxyy)", "openai"))
                models.add(AIModelInfo("claude-3-haiku", "Claude 3 Haiku (via Oxyy)", "anthropic"))
            }

            Result.success(models)
        } catch (e: Exception) {
            Log.e(TAG, "Oxyy getModels failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = configRepo.oxyyApiKey.value.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API key blank hai. Pehle Oxyy API key enter karein.")
            )
        }

        val startTime = System.currentTimeMillis()
        val testRequest = AIChatRequest(
            messages = listOf(AIChatMessage("user", "ping")),
            model = configRepo.oxyyModelId.value,
            maxTokens = 10,
            temperature = 0.0f
        )

        val chatResult = chat(testRequest)
        val latencyMs = System.currentTimeMillis() - startTime

        chatResult.fold(
            onSuccess = { resp ->
                val modelUsed = resp.model.ifBlank { configRepo.oxyyModelId.value }
                Result.success("Connected successfully to Oxyy AI!\nModel: $modelUsed\nLatency: ${latencyMs}ms\nResponse: \"${resp.content.take(30)}\"")
            },
            onFailure = { err ->
                // If chat failed, also attempt GET /models to check if key is valid but model id differs
                val modelsResult = getModels()
                modelsResult.fold(
                    onSuccess = { models ->
                        Result.success("Connected to Oxyy API! (${models.size} models available)\nLatency: ${latencyMs}ms\nNote: Chat with '${configRepo.oxyyModelId.value}' gave error: ${err.message}. Model list se valid model select karein.")
                    },
                    onFailure = {
                        Result.failure(err)
                    }
                )
            }
        )
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val errObj = json.optJSONObject("error")
            val message = errObj?.optString("message") ?: json.optString("message")
            if (!message.isNullOrBlank()) {
                when (code) {
                    401 -> "Invalid API Key [401]: $message. Kripya correct Oxyy API key enter karein."
                    402 -> "Insufficient Balance [402]: $message. Oxyy account balance recharge karein."
                    404 -> "Model Not Found [404]: $message. Kripya valid model ID select karein."
                    429 -> "Rate Limit Exceeded [429]: $message. Kuch der baad retry karein."
                    500, 502, 503, 504 -> "Oxyy Server Error [$code]: $message. Server temporary unavailable hai."
                    else -> "Oxyy API Error [$code]: $message"
                }
            } else {
                defaultMessageForCode(code)
            }
        } catch (_: Exception) {
            defaultMessageForCode(code)
        }
    }

    private fun defaultMessageForCode(code: Int): String {
        return when (code) {
            401 -> "Invalid API Key [401]: Authentication failed. Kripya Oxyy API key check karein."
            402 -> "Payment Required [402]: Oxyy account balance insufficient hai."
            403 -> "Forbidden [403]: Oxyy API access denied. Permissions check karein."
            404 -> "Not Found [404]: Oxyy endpoint ya model mil nahi raha."
            429 -> "Rate Limit [429]: Too many requests. Kripya thoda wait karein."
            500, 502, 503, 504 -> "Oxyy Server Error [$code]: Oxyy servers temporary unavailable hain."
            else -> "HTTP error code: $code"
        }
    }
}
