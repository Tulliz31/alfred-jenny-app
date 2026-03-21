package com.alfredJenny.app.domain.usecase

import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(sessionId: String): Flow<List<ConversationEntity>> =
        chatRepository.getMessages(sessionId)
}
