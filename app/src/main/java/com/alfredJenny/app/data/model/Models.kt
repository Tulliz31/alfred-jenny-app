package com.alfredJenny.app.data.model

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic (Claude)"),
    GEMINI("Google Gemini"),
    OLLAMA("Ollama (Local)")
}

enum class MessageRole { USER, ASSISTANT, SYSTEM }

enum class VoiceMode { CASA, OUTDOOR }

/** Local app preferences stored in DataStore. */
data class UserPreferences(
    val aiProvider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val selectedModel: String = "",
    val baseUrl: String = "",
    val jwtToken: String = "",
    val userRole: String = "",
    val username: String = "",
    // ElevenLabs TTS
    val elevenLabsApiKey: String = "",
    val voiceId: String = "pNInz6obpgDQGcFmaJgB",
    val voiceEnabled: Boolean = false,
    // Jenny config (managed via Servizio section)
    val jennyEnabled: Boolean = false,
    val jennyVoiceId: String = "EXAVITQu4vr4xnSDxMaL",
    val jennyPersonalityLevel: Int = 3,
    // Memory system
    val memoryEnabled: Boolean = true,
    val memorySummaryInterval: Int = 20,   // summarize every N messages
    val maxContextMessages: Int = 50,      // max messages to send in context window
    // Advanced
    val httpTimeoutSeconds: Int = 60,
    val retryCount: Int = 2,
    val debugMode: Boolean = false,
    val providerFallbackEnabled: Boolean = true,
    // Smart Home
    val smartHomeEnabled: Boolean = false,
    // Onboarding
    val onboardingCompleted: Boolean = false,
    // Jenny outfit (CASUAL / SERATA / BIKINI)
    val jennyOutfit: String = "CASUAL",
)

/** Provider info for local display (mirrors backend ProviderInfo). */
data class ProviderInfo(
    val id: String,
    val name: String,
    val description: String,
    val defaultModel: String,
    val active: Boolean,
    val pricePerKInput: Float,
    val pricePerKOutput: Float,
    val avgLatencyMs: Int,
)

/** Local representation of a Tuya smart home device. */
data class SmartHomeDevice(
    val id: String,
    val name: String,
    val category: String,
    val productName: String = "",
    val online: Boolean = false,
    val isOn: Boolean = false,
    val brightness: Int? = null,      // 10–1000, null if not supported
    val temperature: Float? = null,   // current temp in °C, null if not applicable
)

/** A streaming event from ChatRepository.streamMessage(). */
sealed class StreamEvent {
    data class Chunk(val text: String) : StreamEvent()
    data class ProviderAnnounced(val providerId: String) : StreamEvent()
    data class FallbackUsed(val providerId: String) : StreamEvent()
    data class Done(val fullText: String, val providerId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
