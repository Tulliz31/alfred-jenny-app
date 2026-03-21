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
    val baseUrl: String = "",               // empty → use DEFAULT_BASE_URL
    val jwtToken: String = "",
    val userRole: String = "",
    val username: String = "",
    // ElevenLabs TTS
    val elevenLabsApiKey: String = "",
    val voiceId: String = "pNInz6obpgDQGcFmaJgB",   // Adam
    val voiceEnabled: Boolean = false,
    // Jenny config (persisted, managed via Servizio section)
    val jennyEnabled: Boolean = false,
    val jennyVoiceId: String = "EXAVITQu4vr4xnSDxMaL",  // Bella
    val jennyPersonalityLevel: Int = 3
)
