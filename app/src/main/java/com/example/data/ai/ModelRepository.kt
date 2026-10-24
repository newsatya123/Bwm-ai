package com.example.data.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModelRepository(
    private val oxyyProvider: OxyyAIProvider,
    private val geminiProvider: GeminiAIProvider,
    private val configRepo: AIConfigurationRepository
) {
    private val _cachedModels = MutableStateFlow<List<AIModelInfo>>(
        listOf(
            AIModelInfo("oxyy-default", "Oxyy Default Chat", "oxyy"),
            AIModelInfo("gpt-4o-mini", "GPT-4o Mini (via Oxyy)", "openai"),
            AIModelInfo("claude-3-haiku", "Claude 3 Haiku (via Oxyy)", "anthropic"),
            AIModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", "google")
        )
    )
    val cachedModels: StateFlow<List<AIModelInfo>> = _cachedModels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastRefreshError = MutableStateFlow<String?>(null)
    val lastRefreshError: StateFlow<String?> = _lastRefreshError.asStateFlow()

    suspend fun refreshModels(providerId: String = configRepo.activeProvider.value): Result<List<AIModelInfo>> {
        _isLoading.value = true
        _lastRefreshError.value = null

        val provider: AIProvider = when (providerId) {
            AIConfigurationRepository.PROVIDER_GEMINI -> geminiProvider
            else -> oxyyProvider
        }

        val result = provider.getModels()
        _isLoading.value = false

        result.fold(
            onSuccess = { models ->
                _cachedModels.value = models
                _lastRefreshError.value = null
            },
            onFailure = { err ->
                _lastRefreshError.value = err.message
            }
        )
        return result
    }

    fun selectModel(modelId: String) {
        configRepo.setOxyyModelId(modelId)
    }
}
