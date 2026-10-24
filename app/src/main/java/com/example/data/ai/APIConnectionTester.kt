package com.example.data.ai

class APIConnectionTester(
    private val oxyyProvider: OxyyAIProvider,
    private val geminiProvider: GeminiAIProvider,
    private val offlineProvider: OfflineNLPProvider,
    private val configRepo: AIConfigurationRepository
) {
    suspend fun testConnection(providerId: String = configRepo.activeProvider.value): Result<String> {
        return when (providerId) {
            AIConfigurationRepository.PROVIDER_GEMINI -> geminiProvider.testConnection()
            AIConfigurationRepository.PROVIDER_OFFLINE -> offlineProvider.testConnection()
            else -> oxyyProvider.testConnection()
        }
    }
}
