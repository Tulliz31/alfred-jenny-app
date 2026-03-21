package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.local.ConversationDao
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ChatMessage
import com.alfredJenny.app.data.model.ChatRequest
import com.alfredJenny.app.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val dao: ConversationDao,
    private val apiService: ApiService
) {
    fun getMessages(sessionId: String): Flow<List<ConversationEntity>> =
        dao.getMessagesForSession(sessionId)

    suspend fun saveMessage(sessionId: String, role: String, content: String) {
        dao.insertMessage(ConversationEntity(sessionId = sessionId, role = role, content = content))
    }

    suspend fun sendMessage(
        provider: AIProvider,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> {
        val (url, model) = when (provider) {
            AIProvider.OPENAI -> "https://api.openai.com/v1/chat/completions" to "gpt-4o-mini"
            AIProvider.ANTHROPIC -> "https://api.anthropic.com/v1/messages" to "claude-3-haiku-20240307"
            AIProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/chat/completions" to "gemini-pro"
            AIProvider.OLLAMA -> "http://localhost:11434/v1/chat/completions" to "llama3"
        }
        return runCatching {
            val response = apiService.sendMessage(
                url = url,
                authorization = "Bearer $apiKey",
                request = ChatRequest(model = model, messages = messages)
            )
            response.body()?.choices?.firstOrNull()?.message?.content
                ?: error("Empty response from API")
        }
    }

    suspend fun clearSession(sessionId: String) = dao.clearSession(sessionId)
}
