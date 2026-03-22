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
    val memorySummaryInterval: Int = 20,
    val maxContextMessages: Int = 50,
    // Advanced
    val httpTimeoutSeconds: Int = 60,
    val retryCount: Int = 2,
    val debugMode: Boolean = false,
    val providerFallbackEnabled: Boolean = true,
    // Smart Home
    val smartHomeEnabled: Boolean = false,
    val smartHomeAiControl: Boolean = true,  // allow Alfred/Jenny to control devices
    // Tuya credentials (sent to backend at runtime)
    val tuyaClientId: String = "",
    val tuyaClientSecret: String = "",
    val tuyaUserId: String = "",
    val tuyaRegion: String = "EU",
    // Onboarding
    val onboardingCompleted: Boolean = false,
    // Jenny outfit (CASUAL / SERATA / BIKINI)
    val jennyOutfit: String = "CASUAL",
    val jennyAutoOutfit: Boolean = true,
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

/** Local representation of an enriched smart home device. */
data class SmartHomeDevice(
    val id: String,
    val name: String,
    val nameCustom: String = "",
    val category: String = "",
    val type: String = "switch",   // light | switch | thermostat | fan | camera | curtain | ac
    val source: String = "tuya",
    val productName: String = "",
    val online: Boolean = false,
    val isOn: Boolean = false,
    val brightness: Int? = null,       // 0-100 percentage
    val temperature: Float? = null,    // current temp in °C
    val capabilities: List<String> = emptyList(),
    val visible: Boolean = true,
) {
    /** Display name: custom name if set, otherwise the Tuya name. */
    val displayName: String get() = nameCustom.ifBlank { name }

    val canDim: Boolean get() = "brightness" in capabilities
    val canChangeColor: Boolean get() = "colour" in capabilities
    val hasTemperature: Boolean get() = "temperature" in capabilities
}

/** A streaming event from ChatRepository.streamMessage(). */
sealed class StreamEvent {
    data class Chunk(val text: String) : StreamEvent()
    data class ProviderAnnounced(val providerId: String) : StreamEvent()
    data class FallbackUsed(val providerId: String) : StreamEvent()
    data class Done(val fullText: String, val providerId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    /** A smart home command was successfully executed by the AI. */
    data class CommandExecuted(val deviceName: String, val action: String) : StreamEvent()
    /** A smart home command attempted by the AI failed. */
    data class CommandFailed(val deviceName: String, val error: String) : StreamEvent()
}
