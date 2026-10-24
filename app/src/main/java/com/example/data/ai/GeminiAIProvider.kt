package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
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

class GeminiAIProvider : AIProvider {

    companion object {
        private const val TAG = "GeminiAIProvider"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override val id: String = AIConfigurationRepository.PROVIDER_GEMINI
    override val displayName: String = "Google Gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(request: AIChatRequest): Result<AIChatResponse> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key configured nahi hai.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()
            for (msg in request.messages) {
                val partObj = JSONObject().put("text", msg.content)
                val partsArray = JSONArray().put(partObj)
                val contentObj = JSONObject().apply {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
            }

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)
                if (!request.systemPrompt.isNullOrBlank()) {
                    val sysPart = JSONObject().put("text", request.systemPrompt)
                    val sysObj = JSONObject().put("parts", JSONArray().put(sysPart))
                    put("system_instruction", sysObj)
                }
            }

            val httpRequest = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(httpRequest).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Gemini API error [${response.code}]: $bodyString"))
            }

            val respJson = JSONObject(bodyString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            Result.success(AIChatResponse(content = text, model = "gemini-2.5-flash"))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getModels(): Result<List<AIModelInfo>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                AIModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", "google", 1000000),
                AIModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", "google", 2000000),
                AIModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "google", 1000000)
            )
        )
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not set in BuildConfig."))
        }
        val startTime = System.currentTimeMillis()
        val res = chat(AIChatRequest(listOf(AIChatMessage("user", "ping")), "gemini-2.5-flash", maxTokens = 5))
        val latency = System.currentTimeMillis() - startTime
        res.map { "Connected to Google Gemini!\nLatency: ${latency}ms" }
    }
}
