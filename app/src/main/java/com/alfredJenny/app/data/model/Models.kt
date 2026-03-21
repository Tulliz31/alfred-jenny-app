package com.alfredJenny.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic (Claude)"),
    GEMINI("Google Gemini"),
    OLLAMA("Ollama (Local)")
}

enum class MessageRole { USER, ASSISTANT, SYSTEM }

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessage>,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<Choice>?
)

@JsonClass(generateAdapter = true)
data class Choice(
    @Json(name = "message") val message: ChatMessage?
)

data class UserPreferences(
    val aiProvider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val selectedModel: String = ""
)
