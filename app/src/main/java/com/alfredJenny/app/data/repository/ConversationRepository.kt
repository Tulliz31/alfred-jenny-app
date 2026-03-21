package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.local.ConversationDao
import com.alfredJenny.app.data.local.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only repository for conversation history.
 * API calls have moved to [ChatRepository].
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val dao: ConversationDao
) {
    fun getMessages(sessionId: String): Flow<List<ConversationEntity>> =
        dao.getMessagesForSession(sessionId)

    suspend fun saveMessage(sessionId: String, role: String, content: String) {
        dao.insertMessage(ConversationEntity(sessionId = sessionId, role = role, content = content))
    }

    suspend fun clearSession(sessionId: String) = dao.clearSession(sessionId)
}
