package com.alfredJenny.app.domain.usecase

import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: ConversationRepository
) {
    operator fun invoke(sessionId: String): Flow<List<ConversationEntity>> =
        repository.getMessages(sessionId)
}
