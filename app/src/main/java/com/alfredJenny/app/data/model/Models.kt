package com.alfredJenny.app.data.model

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic (Claude)"),
    GEMINI("Google Gemini"),
    OLLAMA("Ollama (Local)")
}

enum class MessageRole { USER, ASSISTANT, SYSTEM }

/** Local app preferences stored in DataStore. */
data class UserPreferences(
    val aiProvider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val selectedModel: String = "",
    val baseUrl: String = "",           // empty → use DEFAULT_BASE_URL
    val jwtToken: String = "",
    val userRole: String = ""
)
