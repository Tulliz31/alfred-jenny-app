package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.local.ConversationDao
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.BackendChatMessageDto
import com.alfredJenny.app.data.model.BackendChatRequestDto
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: ConversationDao
) {
    // ── Local history ─────────────────────────────────────────────────────────

    fun getMessages(sessionId: String): Flow<List<ConversationEntity>> =
        dao.getMessagesForSession(sessionId)

    suspend fun clearSession(sessionId: String) = dao.clearSession(sessionId)

    // ── Remote ────────────────────────────────────────────────────────────────

    suspend fun sendMessage(
        sessionId: String,
        companionId: String,
        userText: String,
        personalityLevel: Int = 3
    ): Result<String> {
        dao.insertMessage(ConversationEntity(sessionId = sessionId, role = "user", content = userText))

        return runCatching {
            val history = dao.getMessagesOnce(sessionId).map {
                BackendChatMessageDto(role = it.role, content = it.content)
            }
            val response = apiService.chat(
                BackendChatRequestDto(
                    companionId = companionId,
                    messages = history,
                    personalityLevel = personalityLevel
                )
            )
            if (response.isSuccessful) {
                val reply = response.body()!!.reply
                dao.insertMessage(ConversationEntity(sessionId = sessionId, role = "assistant", content = reply))
                reply
            } else {
                error(parseApiError(response.errorBody()?.string(), response.code()))
            }
        }.also { result ->
            if (result.isFailure) dao.clearSession(sessionId)
        }
    }

    suspend fun getCompanions(): Result<List<CompanionDto>> = runCatching {
        val response = apiService.getCompanions()
        if (response.isSuccessful) response.body()!!
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    private fun parseApiError(body: String?, code: Int): String {
        if (body.isNullOrBlank()) return "Errore $code"
        return try {
            """"detail"\s*:\s*"([^"]+)"""".toRegex().find(body)
                ?.groupValues?.getOrNull(1) ?: "Errore $code"
        } catch (_: Exception) { "Errore $code" }
    }
}
