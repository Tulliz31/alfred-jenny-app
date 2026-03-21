package com.alfredJenny.app.domain.usecase

import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ChatMessage
import com.alfredJenny.app.data.repository.ConversationRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        userText: String,
        history: List<ChatMessage>,
        provider: AIProvider,
        apiKey: String
    ): Result<String> {
        repository.saveMessage(sessionId, "user", userText)
        val messages = history + ChatMessage(role = "user", content = userText)
        val result = repository.sendMessage(provider, apiKey, messages)
        result.onSuccess { reply ->
            repository.saveMessage(sessionId, "assistant", reply)
        }
        return result
    }
}
