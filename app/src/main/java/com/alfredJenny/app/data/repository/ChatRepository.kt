package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.local.ConversationDao
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.local.ConversationSummaryDao
import com.alfredJenny.app.data.local.ConversationSummaryEntity
import com.alfredJenny.app.data.model.*
import com.alfredJenny.app.data.remote.ApiService
import com.alfredJenny.app.data.remote.TokenStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: ConversationDao,
    private val summaryDao: ConversationSummaryDao,
    private val okHttpClient: OkHttpClient,
    private val tokenStore: TokenStore,
    private val moshi: Moshi
) {
    // ── Local history ─────────────────────────────────────────────────────────

    fun getMessages(sessionId: String): Flow<List<ConversationEntity>> =
        dao.getMessagesForSession(sessionId)

    suspend fun clearSession(sessionId: String) {
        dao.clearSession(sessionId)
        summaryDao.clearSummariesForSession(sessionId)
    }

    // ── Non-streaming send ────────────────────────────────────────────────────

    suspend fun sendMessage(
        sessionId: String,
        companionId: String,
        userText: String,
        personalityLevel: Int = 3,
        memoryEnabled: Boolean = true,
        memorySummaryInterval: Int = 20,
        maxContextMessages: Int = 50,
        fallbackEnabled: Boolean = true,
    ): Result<Pair<String, String>> {   // Pair<reply, providerUsed>
        dao.insertMessage(ConversationEntity(sessionId = sessionId, role = "user", content = userText))

        return runCatching {
            val summaryContext = if (memoryEnabled) {
                summaryDao.getLatestSummary(sessionId)?.summaryText ?: ""
            } else ""

            val allMessages = dao.getMessagesOnce(sessionId)
            val context = if (allMessages.size > maxContextMessages) {
                allMessages.takeLast(maxContextMessages)
            } else allMessages

            val response = apiService.chat(
                BackendChatRequestDto(
                    companionId = companionId,
                    messages = context.map { BackendChatMessageDto(role = it.role, content = it.content) },
                    personalityLevel = personalityLevel,
                    sessionId = sessionId,
                    summaryContext = summaryContext,
                    providerOverride = if (!fallbackEnabled) tokenStore.baseUrl.let { "" } else ""
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                val assistantMsg = ConversationEntity(sessionId = sessionId, role = "assistant", content = body.reply)
                dao.insertMessage(assistantMsg)

                // Trigger memory summary if threshold reached
                if (memoryEnabled) {
                    val count = dao.getMessagesOnce(sessionId).size
                    if (count > 0 && count % memorySummaryInterval == 0) {
                        requestSummaryInBackground(sessionId, assistantMsg.id)
                    }
                }
                Pair(body.reply, body.provider)
            } else {
                error(parseApiError(response.errorBody()?.string(), response.code()))
            }
        }
    }

    // ── Streaming send ────────────────────────────────────────────────────────

    fun streamMessage(
        sessionId: String,
        companionId: String,
        userText: String,
        personalityLevel: Int = 3,
        memoryEnabled: Boolean = true,
        memorySummaryInterval: Int = 20,
        maxContextMessages: Int = 50,
    ): Flow<StreamEvent> = callbackFlow {
        // Save user message
        dao.insertMessage(ConversationEntity(sessionId = sessionId, role = "user", content = userText))

        val summaryContext = if (memoryEnabled) {
            summaryDao.getLatestSummary(sessionId)?.summaryText ?: ""
        } else ""

        val allMessages = dao.getMessagesOnce(sessionId)
        val context = if (allMessages.size > maxContextMessages) {
            allMessages.takeLast(maxContextMessages)
        } else allMessages

        val requestDto = BackendChatRequestDto(
            companionId = companionId,
            messages = context.map { BackendChatMessageDto(role = it.role, content = it.content) },
            personalityLevel = personalityLevel,
            sessionId = sessionId,
            summaryContext = summaryContext,
        )

        val jsonBody = moshi.adapter(BackendChatRequestDto::class.java).toJson(requestDto)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val baseUrl = tokenStore.baseUrl.trimEnd('/')
        val request = Request.Builder()
            .url("$baseUrl/chat/stream")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val fullResponse = StringBuilder()
        var currentProvider = ""

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    when {
                        json.has("error") -> {
                            trySend(StreamEvent.Error(json.getString("error")))
                            close()
                        }
                        json.has("done") && json.getBoolean("done") -> {
                            trySend(StreamEvent.Done(fullResponse.toString(), currentProvider))
                            close()
                        }
                        json.has("fallback") -> {
                            currentProvider = json.getString("fallback")
                            trySend(StreamEvent.FallbackUsed(currentProvider))
                        }
                        json.has("provider") -> {
                            currentProvider = json.getString("provider")
                            trySend(StreamEvent.ProviderAnnounced(currentProvider))
                        }
                        json.has("cmd_ok") -> {
                            val parts = json.getString("cmd_ok").split(":", limit = 2)
                            val name = parts.getOrElse(0) { "dispositivo" }
                            val action = parts.getOrElse(1) { "comando" }
                            trySend(StreamEvent.CommandExecuted(name, action))
                        }
                        json.has("cmd_err") -> {
                            val parts = json.getString("cmd_err").split(":", limit = 2)
                            val name = parts.getOrElse(0) { "dispositivo" }
                            val err = parts.getOrElse(1) { "errore" }
                            trySend(StreamEvent.CommandFailed(name, err))
                        }
                        json.has("c") -> {
                            val chunk = json.getString("c")
                            if (chunk.isNotEmpty()) {
                                fullResponse.append(chunk)
                                trySend(StreamEvent.Chunk(chunk))
                            }
                        }
                    }
                } catch (_: Exception) { /* skip malformed events */ }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val msg = t?.message ?: "SSE error ${response?.code}"
                trySend(StreamEvent.Error(msg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                // May be called after [DONE] — safe to call close() again
                close()
            }
        }

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    // ── Save streamed reply to DB ─────────────────────────────────────────────

    suspend fun saveStreamedReply(
        sessionId: String,
        replyText: String,
        memoryEnabled: Boolean = true,
        memorySummaryInterval: Int = 20,
    ) {
        val msg = ConversationEntity(sessionId = sessionId, role = "assistant", content = replyText)
        dao.insertMessage(msg)

        if (memoryEnabled) {
            val count = dao.getMessagesOnce(sessionId).size
            if (count > 0 && count % memorySummaryInterval == 0) {
                requestSummaryInBackground(sessionId, msg.id)
            }
        }
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    private suspend fun requestSummaryInBackground(sessionId: String, lastMessageId: Long) {
        try {
            val messages = dao.getMessagesOnce(sessionId).map {
                BackendChatMessageDto(role = it.role, content = it.content)
            }
            val response = apiService.summarize(SummarizeRequestDto(messages = messages))
            if (response.isSuccessful) {
                val summary = response.body()!!.summary
                summaryDao.insertSummary(
                    ConversationSummaryEntity(
                        sessionId = sessionId,
                        summaryText = summary,
                        coveringUpToMessageId = lastMessageId
                    )
                )
            }
        } catch (_: Exception) { /* summary failure is non-fatal */ }
    }

    // ── Providers ─────────────────────────────────────────────────────────────

    suspend fun getProviders(): Result<List<ProviderInfo>> = runCatching {
        val response = apiService.getProviders()
        if (response.isSuccessful) {
            response.body()!!.map { dto ->
                ProviderInfo(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    defaultModel = dto.defaultModel,
                    active = dto.active,
                    pricePerKInput = dto.pricePerKInput,
                    pricePerKOutput = dto.pricePerKOutput,
                    avgLatencyMs = dto.avgLatencyMs
                )
            }
        } else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun setActiveProvider(providerId: String): Result<ProviderInfo> = runCatching {
        val response = apiService.setActiveProvider(SetProviderRequestDto(provider = providerId))
        if (response.isSuccessful) {
            val dto = response.body()!!
            ProviderInfo(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                defaultModel = dto.defaultModel,
                active = dto.active,
                pricePerKInput = dto.pricePerKInput,
                pricePerKOutput = dto.pricePerKOutput,
                avgLatencyMs = dto.avgLatencyMs
            )
        } else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun getCompanions(): Result<List<CompanionDto>> = runCatching {
        val response = apiService.getCompanions()
        if (response.isSuccessful) response.body()!!
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseApiError(body: String?, code: Int): String {
        if (body.isNullOrBlank()) return "Errore $code"
        return try {
            """"detail"\s*:\s*"([^"]+)"""".toRegex().find(body)
                ?.groupValues?.getOrNull(1) ?: "Errore $code"
        } catch (_: Exception) { "Errore $code" }
    }
}
