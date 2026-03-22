package com.alfredJenny.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.model.StreamEvent
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.data.repository.CalendarRepository
import com.alfredJenny.app.data.repository.MemoRepository
import com.alfredJenny.app.ui.components.AlfredAvatarState
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.domain.usecase.GetConversationHistoryUseCase
import com.alfredJenny.app.services.ReminderWorker
import com.alfredJenny.app.services.SpeechInputService
import com.alfredJenny.app.services.VoicePlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.alfredJenny.app.permissions.PermissionNeeded
import com.alfredJenny.app.permissions.PermissionUtils
import javax.inject.Inject

data class HomeUiState(
    // Chat
    val messages: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val streamingContent: String = "",          // in-progress streamed reply
    val isLoading: Boolean = false,
    val error: String? = null,
    // Companions
    val companions: List<CompanionDto> = emptyList(),
    val selectedCompanionId: String = "alfred",
    val isAdmin: Boolean = false,
    // Voice
    val voiceMode: VoiceMode = VoiceMode.OUTDOOR,
    val voiceEnabled: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialSpeechText: String = "",
    val voiceError: String? = null,
    val avatarState: AlfredAvatarState = AlfredAvatarState.IDLE,
    // Provider
    val activeProvider: String = "",
    val fallbackNotice: String? = null,         // brief message when fallback fires
    val isRefreshing: Boolean = false,
    // Smart home command feedback
    val commandFeedback: String? = null,
    // Memo/calendar feedback
    val memoFeedback: String? = null,
    // Pending calendar event requiring confirmation
    val pendingCalendarEvent: StreamEvent.EventRequested? = null,
    // Calendar read results
    val calendarReadResult: String? = null,
    val permissionNeeded: PermissionNeeded = PermissionNeeded.NONE,
    val permissionDenied: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val voicePlaybackService: VoicePlaybackService,
    private val speechInputService: SpeechInputService,
    private val memoRepository: MemoRepository,
    private val calendarRepository: CalendarRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val sessionId: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var prefs: UserPreferences = UserPreferences()

    private var pendingEventForPermission: StreamEvent.EventRequested? = null
    private var pendingReminderForPermission: Triple<String, String, String>? = null
    private var pendingCalendarReadForPermission: String? = null

    init {
        viewModelScope.launch {
            getConversationHistoryUseCase(sessionId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        loadCompanions()
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { p ->
                prefs = p
                _uiState.update { it.copy(voiceEnabled = p.voiceEnabled) }
            }
        }
        viewModelScope.launch {
            voicePlaybackService.isPlaying.collect { playing ->
                _uiState.update {
                    it.copy(isSpeaking = playing,
                            avatarState = deriveAvatarState(it.isListening, it.isLoading, playing, it.streamingContent.isNotBlank()))
                }
            }
        }
        viewModelScope.launch {
            speechInputService.state.collect { s ->
                _uiState.update {
                    it.copy(isListening = s.isListening, partialSpeechText = s.partialText,
                            avatarState = deriveAvatarState(s.isListening, it.isLoading, it.isSpeaking, it.streamingContent.isNotBlank()))
                }
            }
        }
        speechInputService.onFinalResult    = { text -> onVoiceResult(text) }
        speechInputService.onCommandDetected = { text -> onVoiceResult(text) }
    }

    // ── Companions ────────────────────────────────────────────────────────────

    private fun loadCompanions() {
        viewModelScope.launch {
            chatRepository.getCompanions()
                .onSuccess { list ->
                    _uiState.update { it.copy(companions = list, isAdmin = authRepository.isAdmin()) }
                }
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            chatRepository.getCompanions()
                .onSuccess { list ->
                    _uiState.update { it.copy(companions = list, isAdmin = authRepository.isAdmin(), isRefreshing = false) }
                }
                .onFailure { _uiState.update { it.copy(isRefreshing = false) } }
        }
    }

    fun onCompanionSelected(id: String) {
        if (id == _uiState.value.selectedCompanionId) return
        _uiState.update { it.copy(selectedCompanionId = id) }
        viewModelScope.launch { chatRepository.clearSession(sessionId) }
    }

    // ── Text chat ─────────────────────────────────────────────────────────────

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage(overrideText: String? = null) {
        val state = _uiState.value
        val text = (overrideText ?: state.inputText).trim()
        val companionId = state.selectedCompanionId
        if (text.isBlank() || state.isLoading) return

        _uiState.update {
            it.copy(inputText = "", isLoading = true, streamingContent = "",
                    error = null, fallbackNotice = null,
                    avatarState = AlfredAvatarState.THINKING)
        }

        viewModelScope.launch {
            chatRepository.streamMessage(
                sessionId = sessionId,
                companionId = companionId,
                personalityLevel = prefs.jennyPersonalityLevel,
                memoryEnabled = prefs.memoryEnabled,
                memorySummaryInterval = prefs.memorySummaryInterval,
                maxContextMessages = prefs.maxContextMessages,
                userText = text,
            ).collect { event ->
                when (event) {
                    is StreamEvent.Chunk -> {
                        val newContent = _uiState.value.streamingContent + event.text
                        _uiState.update {
                            it.copy(
                                streamingContent = newContent,
                                avatarState = deriveAvatarState(it.isListening, true, it.isSpeaking, true)
                            )
                        }
                    }
                    is StreamEvent.ProviderAnnounced -> {
                        _uiState.update { it.copy(activeProvider = event.providerId) }
                    }
                    is StreamEvent.FallbackUsed -> {
                        _uiState.update {
                            it.copy(activeProvider = event.providerId,
                                    fallbackNotice = "Fallback a ${event.providerId}")
                        }
                    }
                    is StreamEvent.Done -> {
                        val fullText = event.fullText
                        chatRepository.saveStreamedReply(
                            sessionId = sessionId,
                            replyText = fullText,
                            memoryEnabled = prefs.memoryEnabled,
                            memorySummaryInterval = prefs.memorySummaryInterval,
                        )
                        _uiState.update {
                            it.copy(isLoading = false, streamingContent = "",
                                    activeProvider = event.providerId,
                                    avatarState = if (it.voiceEnabled) AlfredAvatarState.TALKING else AlfredAvatarState.IDLE)
                        }
                        if (prefs.voiceEnabled) speakReply(fullText)
                    }
                    is StreamEvent.CommandExecuted -> {
                        val icon = actionIcon(event.action)
                        _uiState.update { it.copy(commandFeedback = "$icon ${event.deviceName}") }
                    }
                    is StreamEvent.CommandFailed -> {
                        _uiState.update { it.copy(commandFeedback = "❌ ${event.deviceName}: ${event.error}") }
                    }
                    is StreamEvent.MemoSaved -> {
                        viewModelScope.launch {
                            memoRepository.saveMemo(
                                title = event.title,
                                content = event.content,
                                companion = _uiState.value.selectedCompanionId,
                            )
                            _uiState.update { it.copy(memoFeedback = "📝 Nota salvata: ${event.title}") }
                        }
                    }
                    is StreamEvent.EventRequested -> {
                        if (prefs.calendarConfirmBeforeAdd) {
                            _uiState.update { it.copy(pendingCalendarEvent = event) }
                        } else {
                            insertCalendarEvent(event)
                        }
                    }
                    is StreamEvent.ReminderScheduled -> {
                        scheduleReminder(event.text, event.date, event.time)
                        _uiState.update { it.copy(memoFeedback = "⏰ Promemoria impostato: ${event.text}") }
                    }
                    is StreamEvent.CalendarRead -> {
                        if (!PermissionUtils.areCalendarGranted(context)) {
                            pendingCalendarReadForPermission = event.period
                            _uiState.update { it.copy(permissionNeeded = PermissionNeeded.CALENDAR) }
                        } else {
                            viewModelScope.launch {
                                val calId = prefs.defaultCalendarId
                                if (calId < 0L) {
                                    _uiState.update { it.copy(calendarReadResult = "Nessun calendario selezionato nelle impostazioni.") }
                                    return@launch
                                }
                                val (startMs, endMs) = periodToRange(event.period)
                                val events = calendarRepository.getEvents(calId, startMs, endMs)
                                val text = calendarRepository.formatEventsForDisplay(events)
                                _uiState.update { it.copy(calendarReadResult = text) }
                            }
                        }
                    }
                    is StreamEvent.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, streamingContent = "",
                                    error = event.message, avatarState = AlfredAvatarState.IDLE)
                        }
                    }
                }
            }
        }
    }

    fun dismissError()              = _uiState.update { it.copy(error = null, voiceError = null) }
    fun dismissFallbackNotice()     = _uiState.update { it.copy(fallbackNotice = null) }
    fun dismissCommandFeedback()    = _uiState.update { it.copy(commandFeedback = null) }
    fun dismissMemoFeedback()       = _uiState.update { it.copy(memoFeedback = null) }
    fun dismissCalendarReadResult() = _uiState.update { it.copy(calendarReadResult = null) }

    fun confirmCalendarEvent() {
        val ev = _uiState.value.pendingCalendarEvent ?: return
        _uiState.update { it.copy(pendingCalendarEvent = null) }
        viewModelScope.launch { insertCalendarEvent(ev) }
    }
    fun dismissCalendarEvent() = _uiState.update { it.copy(pendingCalendarEvent = null) }

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionNeeded = PermissionNeeded.NONE) }
        if (granted) {
            pendingEventForPermission?.let { ev ->
                pendingEventForPermission = null
                viewModelScope.launch { insertCalendarEvent(ev) }
            }
            pendingCalendarReadForPermission?.let { period ->
                pendingCalendarReadForPermission = null
                viewModelScope.launch {
                    val calId = prefs.defaultCalendarId
                    if (calId >= 0L) {
                        val (startMs, endMs) = periodToRange(period)
                        val events = calendarRepository.getEvents(calId, startMs, endMs)
                        _uiState.update { it.copy(calendarReadResult = calendarRepository.formatEventsForDisplay(events)) }
                    }
                }
            }
        } else {
            pendingEventForPermission = null
            pendingCalendarReadForPermission = null
            _uiState.update { it.copy(permissionDenied = "Permesso Calendario negato — vai in Impostazioni") }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionNeeded = PermissionNeeded.NONE) }
        if (granted) {
            pendingReminderForPermission?.let { (text, date, time) ->
                pendingReminderForPermission = null
                scheduleReminder(text, date, time)
            }
        } else {
            pendingReminderForPermission = null
            _uiState.update { it.copy(permissionDenied = "Permesso Notifiche negato — vai in Impostazioni") }
        }
    }

    fun dismissPermissionDenied() = _uiState.update { it.copy(permissionDenied = null) }

    private suspend fun insertCalendarEvent(ev: StreamEvent.EventRequested) {
        if (!PermissionUtils.areCalendarGranted(context)) {
            pendingEventForPermission = ev
            _uiState.update { it.copy(permissionNeeded = PermissionNeeded.CALENDAR) }
            return
        }
        val calId = prefs.defaultCalendarId
        if (calId < 0L) {
            _uiState.update { it.copy(memoFeedback = "⚠️ Seleziona un calendario nelle impostazioni") }
            return
        }
        val startMs = calendarRepository.parseEventTimeMs(ev.date, ev.startTime)
        val endMs   = calendarRepository.parseEventTimeMs(ev.date, ev.endTime)
        calendarRepository.insertEvent(calId, ev.title, ev.description, startMs, endMs)
        _uiState.update { it.copy(memoFeedback = "📅 Evento aggiunto: ${ev.title}") }
    }

    private fun scheduleReminder(text: String, date: String, time: String) {
        if (!PermissionUtils.areNotificationsGranted(context)) {
            pendingReminderForPermission = Triple(text, date, time)
            _uiState.update { it.copy(permissionNeeded = PermissionNeeded.NOTIFICATION) }
            return
        }
        val triggerMs = calendarRepository.parseEventTimeMs(date, time)
        val delayMs = triggerMs - System.currentTimeMillis()
        if (delayMs <= 0) return
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TITLE, "Promemoria")
            .putString(ReminderWorker.KEY_TEXT, text)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun periodToRange(period: String): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val start = cal.timeInMillis
        return when (period) {
            "domani" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                val s = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                Pair(s, cal.timeInMillis)
            }
            "settimana" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                Pair(start, cal.timeInMillis)
            }
            else -> { // oggi
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                Pair(start, cal.timeInMillis)
            }
        }
    }

    // ── Voice mode ────────────────────────────────────────────────────────────

    fun toggleVoiceMode() {
        val next = if (_uiState.value.voiceMode == VoiceMode.OUTDOOR) VoiceMode.CASA else VoiceMode.OUTDOOR
        _uiState.update { it.copy(voiceMode = next) }
        if (next == VoiceMode.CASA) speechInputService.startCasaListening()
        else speechInputService.stopListening()
    }

    fun startOutdoorListening() {
        if (_uiState.value.voiceMode != VoiceMode.OUTDOOR) return
        speechInputService.startOutdoorListening()
    }

    fun stopOutdoorListening() {
        if (_uiState.value.voiceMode != VoiceMode.OUTDOOR) return
        speechInputService.stopListening()
    }

    fun toggleVoice() {
        val enabled = !_uiState.value.voiceEnabled
        _uiState.update { it.copy(voiceEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.saveVoiceEnabled(enabled) }
        if (!enabled) voicePlaybackService.stopPlayback()
    }

    fun stopSpeaking() = voicePlaybackService.stopPlayback()

    private fun onVoiceResult(text: String) {
        _uiState.update { it.copy(inputText = text) }
        sendMessage(overrideText = text)
    }

    private fun speakReply(text: String) {
        viewModelScope.launch {
            voicePlaybackService.speak(
                text    = text,
                voiceId = prefs.voiceId,
                apiKey  = prefs.elevenLabsApiKey
            ).onFailure { err ->
                _uiState.update { it.copy(voiceError = "TTS: ${err.message}") }
            }
        }
    }

    companion object {
        fun actionIcon(action: String): String = when {
            action.contains("turn_on") || action == "on"    -> "💡"
            action.contains("turn_off") || action == "off"  -> "🔌"
            action == "brightness"                          -> "🔆"
            action == "colour"                              -> "🎨"
            action == "temperature"                         -> "🌡️"
            else                                            -> "✅"
        }
    }

    private fun deriveAvatarState(
        isListening: Boolean,
        isLoading: Boolean,
        isSpeaking: Boolean,
        isStreaming: Boolean
    ): AlfredAvatarState = when {
        isSpeaking   -> AlfredAvatarState.TALKING
        isStreaming  -> AlfredAvatarState.TALKING
        isLoading    -> AlfredAvatarState.THINKING
        isListening  -> AlfredAvatarState.LISTENING
        else         -> AlfredAvatarState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        speechInputService.release()
        voicePlaybackService.stopPlayback()
    }
}
