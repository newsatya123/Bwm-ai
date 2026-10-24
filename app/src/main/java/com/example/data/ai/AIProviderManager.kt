package com.example.data.ai

class AIProviderManager(
    val oxyyProvider: OxyyAIProvider,
    val geminiProvider: GeminiAIProvider,
    val offlineProvider: OfflineNLPProvider,
    val configRepo: AIConfigurationRepository
) {
    fun getActiveProvider(): AIProvider {
        return when (configRepo.activeProvider.value) {
            AIConfigurationRepository.PROVIDER_GEMINI -> geminiProvider
            AIConfigurationRepository.PROVIDER_OFFLINE -> offlineProvider
            else -> oxyyProvider
        }
    }

    fun getFallbackProvider(): AIProvider {
        return when (configRepo.fallbackProvider.value) {
            AIConfigurationRepository.PROVIDER_GEMINI -> geminiProvider
            AIConfigurationRepository.PROVIDER_OXYY -> oxyyProvider
            else -> offlineProvider
        }
    }

    fun getAllProviders(): List<AIProvider> {
        return listOf(oxyyProvider, geminiProvider, offlineProvider)
    }
}
