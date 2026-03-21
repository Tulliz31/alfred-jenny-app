package com.alfredJenny.app.domain.usecase

import com.alfredJenny.app.data.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        companionId: String,
        userText: String
    ): Result<String> = chatRepository.sendMessage(sessionId, companionId, userText)
}
