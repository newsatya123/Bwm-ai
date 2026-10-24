package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIConfigurationRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bwm_ai_config", Context.MODE_PRIVATE)

    companion object {
        const val PROVIDER_OXYY = "oxyy"
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OFFLINE = "offline"

        const val MODE_AUTO = "auto"
        const val MODE_ONLINE_ONLY = "online"
        const val MODE_OFFLINE_ONLY = "offline"

        const val DEFAULT_OXYY_BASE_URL = "https://api.oxyy.ai/v1"
        const val DEFAULT_OXYY_MODEL = "oxyy-default"
    }

    private val _isAiBrainEnabled = MutableStateFlow(prefs.getBoolean("ai_brain_enabled", true))
    val isAiBrainEnabled: StateFlow<Boolean> = _isAiBrainEnabled.asStateFlow()

    private val _activeProvider = MutableStateFlow(prefs.getString("active_provider", PROVIDER_OXYY) ?: PROVIDER_OXYY)
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _executionMode = MutableStateFlow(prefs.getString("execution_mode", MODE_AUTO) ?: MODE_AUTO)
    val executionMode: StateFlow<String> = _executionMode.asStateFlow()

    private val _oxyyApiKey = MutableStateFlow(prefs.getString("oxyy_api_key", "") ?: "")
    val oxyyApiKey: StateFlow<String> = _oxyyApiKey.asStateFlow()

    private val _oxyyBaseUrl = MutableStateFlow(prefs.getString("oxyy_base_url", DEFAULT_OXYY_BASE_URL) ?: DEFAULT_OXYY_BASE_URL)
    val oxyyBaseUrl: StateFlow<String> = _oxyyBaseUrl.asStateFlow()

    private val _oxyyModelId = MutableStateFlow(prefs.getString("oxyy_model_id", DEFAULT_OXYY_MODEL) ?: DEFAULT_OXYY_MODEL)
    val oxyyModelId: StateFlow<String> = _oxyyModelId.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat("temperature", 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(prefs.getInt("max_tokens", 1024))
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _timeoutSeconds = MutableStateFlow(prefs.getInt("timeout_seconds", 30))
    val timeoutSeconds: StateFlow<Int> = _timeoutSeconds.asStateFlow()

    private val _fallbackProvider = MutableStateFlow(prefs.getString("fallback_provider", PROVIDER_OFFLINE) ?: PROVIDER_OFFLINE)
    val fallbackProvider: StateFlow<String> = _fallbackProvider.asStateFlow()

    fun setAiBrainEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_brain_enabled", enabled).apply()
        _isAiBrainEnabled.value = enabled
    }

    fun setActiveProvider(providerId: String) {
        prefs.edit().putString("active_provider", providerId).apply()
        _activeProvider.value = providerId
    }

    fun setExecutionMode(mode: String) {
        prefs.edit().putString("execution_mode", mode).apply()
        _executionMode.value = mode
    }

    fun setOxyyApiKey(apiKey: String) {
        val clean = apiKey.trim()
        prefs.edit().putString("oxyy_api_key", clean).apply()
        _oxyyApiKey.value = clean
    }

    fun setOxyyBaseUrl(baseUrl: String) {
        var clean = baseUrl.trim()
        if (clean.isBlank()) clean = DEFAULT_OXYY_BASE_URL
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length - 1)
        prefs.edit().putString("oxyy_base_url", clean).apply()
        _oxyyBaseUrl.value = clean
    }

    fun setOxyyModelId(modelId: String) {
        val clean = modelId.trim().ifBlank { DEFAULT_OXYY_MODEL }
        prefs.edit().putString("oxyy_model_id", clean).apply()
        _oxyyModelId.value = clean
    }

    fun setTemperature(temp: Float) {
        val bounded = temp.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat("temperature", bounded).apply()
        _temperature.value = bounded
    }

    fun setMaxTokens(tokens: Int) {
        val bounded = tokens.coerceIn(64, 4096)
        prefs.edit().putInt("max_tokens", bounded).apply()
        _maxTokens.value = bounded
    }

    fun setTimeoutSeconds(seconds: Int) {
        val bounded = seconds.coerceIn(5, 120)
        prefs.edit().putInt("timeout_seconds", bounded).apply()
        _timeoutSeconds.value = bounded
    }

    fun setFallbackProvider(providerId: String) {
        prefs.edit().putString("fallback_provider", providerId).apply()
        _fallbackProvider.value = providerId
    }

    fun clearOxyyConfiguration() {
        prefs.edit()
            .remove("oxyy_api_key")
            .putString("oxyy_base_url", DEFAULT_OXYY_BASE_URL)
            .putString("oxyy_model_id", DEFAULT_OXYY_MODEL)
            .apply()
        _oxyyApiKey.value = ""
        _oxyyBaseUrl.value = DEFAULT_OXYY_BASE_URL
        _oxyyModelId.value = DEFAULT_OXYY_MODEL
    }
}
