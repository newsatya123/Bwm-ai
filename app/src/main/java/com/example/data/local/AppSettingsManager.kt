package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssistantLogoTheme(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val description: String
) {
    NEON_CYAN(
        id = "cyan",
        displayName = "Neon Cyan (Default)",
        primaryColor = Color(0xFF00F2FE),
        secondaryColor = Color(0xFF7928CA),
        description = "Cyberpunk glowing cyan & electric violet"
    ),
    VIOLET_NEBULA(
        id = "violet",
        displayName = "Violet Nebula",
        primaryColor = Color(0xFFB5179E),
        secondaryColor = Color(0xFF7209B7),
        description = "Cosmic electric magenta & deep purple"
    ),
    EMERALD_MATRIX(
        id = "emerald",
        displayName = "Emerald Matrix",
        primaryColor = Color(0xFF00F5D4),
        secondaryColor = Color(0xFF00BB7F),
        description = "Luminescent emerald green & mint glow"
    ),
    SOLAR_FLARE(
        id = "solar",
        displayName = "Solar Flare",
        primaryColor = Color(0xFFFF9E00),
        secondaryColor = Color(0xFFFF0055),
        description = "Fiery glowing amber & crimson flame"
    ),
    QUANTUM_BLUE(
        id = "quantum",
        displayName = "Quantum Blue",
        primaryColor = Color(0xFF4CC9F0),
        secondaryColor = Color(0xFF4361EE),
        description = "Ice blue & royal sapphire aura"
    );

    companion object {
        fun fromId(id: String?): AssistantLogoTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NEON_CYAN
        }
    }
}

enum class SttProvider(
    val id: String,
    val displayName: String,
    val description: String
) {
    SYSTEM_DEFAULT(
        id = "system",
        displayName = "Android System Recognizer",
        description = "Standard OS speech recognition engine with offline package support"
    ),
    GOOGLE_VOICE(
        id = "google",
        displayName = "Google Speech Services",
        description = "High accuracy for Indian accents, Hindi & Hinglish vocabulary"
    ),
    OFFLINE_HYBRID(
        id = "offline",
        displayName = "On-Device Offline Engine",
        description = "Fastest response time, completely private on-device processing"
    );

    companion object {
        fun fromId(id: String?): SttProvider {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM_DEFAULT
        }
    }
}

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bwm_app_settings", Context.MODE_PRIVATE)

    private val _assistantName = MutableStateFlow(prefs.getString("assistant_name", "BWM AI") ?: "BWM AI")
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _logoTheme = MutableStateFlow(
        AssistantLogoTheme.fromId(prefs.getString("logo_theme", AssistantLogoTheme.NEON_CYAN.id))
    )
    val logoTheme: StateFlow<AssistantLogoTheme> = _logoTheme.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(prefs.getBoolean("continuous_listening", true))
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    private val _autoBackgroundListeningOnAppLaunch = MutableStateFlow(prefs.getBoolean("auto_bg_listening", true))
    val autoBackgroundListeningOnAppLaunch: StateFlow<Boolean> = _autoBackgroundListeningOnAppLaunch.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(prefs.getBoolean("wake_word_enabled", true))
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _micSensitivity = MutableStateFlow(prefs.getFloat("mic_sensitivity", 0.5f))
    val micSensitivity: StateFlow<Float> = _micSensitivity.asStateFlow()

    private val _sttProvider = MutableStateFlow(
        SttProvider.fromId(prefs.getString("stt_provider", SttProvider.SYSTEM_DEFAULT.id))
    )
    val sttProvider: StateFlow<SttProvider> = _sttProvider.asStateFlow()

    private val _isWhatsappDirectSend = MutableStateFlow(prefs.getBoolean("whatsapp_direct_send", false))
    val isWhatsappDirectSend: StateFlow<Boolean> = _isWhatsappDirectSend.asStateFlow()

    fun setAssistantName(name: String) {
        val clean = name.trim().ifBlank { "BWM AI" }
        prefs.edit().putString("assistant_name", clean).apply()
        _assistantName.value = clean
    }

    fun setLogoTheme(theme: AssistantLogoTheme) {
        prefs.edit().putString("logo_theme", theme.id).apply()
        _logoTheme.value = theme
    }

    fun setContinuousListening(enabled: Boolean) {
        prefs.edit().putBoolean("continuous_listening", enabled).apply()
        _isContinuousListening.value = enabled
    }

    fun setAutoBackgroundListening(enabled: Boolean) {
        prefs.edit().putBoolean("auto_bg_listening", enabled).apply()
        _autoBackgroundListeningOnAppLaunch.value = enabled
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
        _isWakeWordEnabled.value = enabled
    }

    fun setMicSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(0.1f, 1.0f)
        prefs.edit().putFloat("mic_sensitivity", clamped).apply()
        _micSensitivity.value = clamped
    }

    fun setSttProvider(provider: SttProvider) {
        prefs.edit().putString("stt_provider", provider.id).apply()
        _sttProvider.value = provider
    }

    fun setWhatsappDirectSend(enabled: Boolean) {
        prefs.edit().putBoolean("whatsapp_direct_send", enabled).apply()
        _isWhatsappDirectSend.value = enabled
    }
}
