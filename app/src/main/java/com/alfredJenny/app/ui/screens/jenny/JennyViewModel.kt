package com.alfredJenny.app.ui.screens.jenny

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.StreamEvent
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.CalendarRepository
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.MemoRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.services.ReminderWorker
import com.alfredJenny.app.services.SpeechInputService
import com.alfredJenny.app.services.VoicePlaybackService
import com.alfredJenny.app.ui.components.AlfredAvatarState
import com.alfredJenny.app.ui.components.EmotionDetector
import com.alfredJenny.app.ui.components.EyeState
import com.alfredJenny.app.ui.components.JennyOutfit
import com.alfredJenny.app.ui.components.OutfitDetector
import com.alfredJenny.app.ui.components.OutfitManager
import com.alfredJenny.app.permissions.PermissionNeeded
import com.alfredJenny.app.permissions.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val JENNY_SESSION_ID = "jenny_companion"

data class JennyUiState(
    val messages: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val streamingContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val voiceMode: VoiceMode = VoiceMode.OUTDOOR,
    val voiceEnabled: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialSpeechText: String = "",
    val voiceError: String? = null,
    val personalityLevel: Int = 3,
    val avatarState: AlfredAvatarState = AlfredAvatarState.IDLE,
    val activeProvider: String = "",
    val fallbackNotice: String? = null,
    val audioAmplitude: Float = 0f,
    val outfit: JennyOutfit = JennyOutfit.CASUAL,
    val eyeEmotion: EyeState = EyeState.OPEN,
    val outfitToast: String? = null,
    val memoFeedback: String? = null,
    val pendingCalendarEvent: StreamEvent.EventRequested? = null,
    val calendarReadResult: String? = null,
    val permissionNeeded: PermissionNeeded = PermissionNeeded.NONE,
    val permissionDenied: String? = null,
)

@HiltViewModel
class JennyViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val voicePlaybackService: VoicePlaybackService,
    private val speechInputService: SpeechInputService,
    private val memoRepository: MemoRepository,
    private val calendarRepository: CalendarRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JennyUiState())
    val uiState: StateFlow<JennyUiState> = _uiState

    private var prefs: UserPreferences = UserPreferences()

    private var pendingEventForPermission: StreamEvent.EventRequested? = null
    private var pendingReminderForPermission: Triple<String, String, String>? = null
    private var pendingCalendarReadForPermission: String? = null

    init {
        viewModelScope.launch {
            chatRepository.getMessages(JENNY_SESSION_ID).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { p ->
                prefs = p
                val savedOutfit = runCatching { JennyOutfit.valueOf(p.jennyOutfit) }
                    .getOrDefault(OutfitManager.autoOutfit())
                _uiState.update {
                    it.copy(
                        voiceEnabled = p.voiceEnabled,
                        personalityLevel = p.jennyPersonalityLevel,
                        outfit = savedOutfit,
                    )
                }
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
            voicePlaybackService.audioAmplitude.collect { amp ->
                _uiState.update { it.copy(audioAmplitude = amp) }
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

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage(overrideText: String? = null) {
        val state = _uiState.value
        val text = (overrideText ?: state.inputText).trim()
        if (text.isBlank() || state.isLoading) return

        // Outfit auto-detection on user text
        if (prefs.jennyAutoOutfit) {
            OutfitDetector.detect(text)?.let { change ->
                if (change.outfit != _uiState.value.outfit) {
                    setOutfit(change.outfit)
                    _uiState.update { it.copy(outfitToast = change.phrase) }
                    viewModelScope.launch { delay(3000); _uiState.update { it.copy(outfitToast = null) } }
                }
            }
        }

        _uiState.update {
            it.copy(inputText = "", isLoading = true, streamingContent = "",
                    error = null, fallbackNotice = null,
                    avatarState = AlfredAvatarState.THINKING)
        }

        viewModelScope.launch {
            chatRepository.streamMessage(
                sessionId = JENNY_SESSION_ID,
                companionId = "jenny",
                userText = text,
                personalityLevel = prefs.jennyPersonalityLevel,
                memoryEnabled = prefs.memoryEnabled,
                memorySummaryInterval = prefs.memorySummaryInterval,
                maxContextMessages = prefs.maxContextMessages,
            ).collect { event ->
                when (event) {
                    is StreamEvent.Chunk -> {
                        val newContent = _uiState.value.streamingContent + event.text
                        _uiState.update {
                            it.copy(streamingContent = newContent,
                                    avatarState = deriveAvatarState(it.isListening, true, it.isSpeaking, true))
                        }
                    }
                    is StreamEvent.ProviderAnnounced -> {
                        _uiState.update { it.copy(activeProvider = event.providerId) }
                    }
                    is StreamEvent.FallbackUsed -> {
                        _uiState.update { it.copy(activeProvider = event.providerId,
                                fallbackNotice = "Fallback a ${event.providerId}") }
                    }
                    is StreamEvent.Done -> {
                        chatRepository.saveStreamedReply(
                            sessionId = JENNY_SESSION_ID,
                            replyText = event.fullText,
                            memoryEnabled = prefs.memoryEnabled,
                            memorySummaryInterval = prefs.memorySummaryInterval,
                        )
                        val emotion = EmotionDetector.detect(event.fullText)
                        _uiState.update {
                            it.copy(isLoading = false, streamingContent = "",
                                    activeProvider = event.providerId,
                                    avatarState = if (it.voiceEnabled) AlfredAvatarState.TALKING else AlfredAvatarState.IDLE,
                                    eyeEmotion = emotion)
                        }
                        // Emotion expires after 5 s
                        viewModelScope.launch {
                            delay(5000)
                            _uiState.update { it.copy(eyeEmotion = EyeState.OPEN) }
                        }
                        if (prefs.voiceEnabled) speakReply(event.fullText)
                        // Outfit auto-detection from AI reply (skip user-text triggered changes)
                        if (prefs.jennyAutoOutfit) {
                            OutfitDetector.detect(userText = "", aiText = event.fullText)?.let { change ->
                                if (change.outfit != _uiState.value.outfit) {
                                    setOutfit(change.outfit)
                                    _uiState.update { it.copy(outfitToast = change.phrase) }
                                    viewModelScope.launch {
                                        delay(3000)
                                        _uiState.update { it.copy(outfitToast = null) }
                                    }
                                }
                            }
                        }
                    }
                    is StreamEvent.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, streamingContent = "",
                                    error = event.message, avatarState = AlfredAvatarState.IDLE)
                        }
                    }
                    is StreamEvent.CommandExecuted -> { /* smart home commands not used in Jenny */ }
                    is StreamEvent.CommandFailed   -> { /* smart home commands not used in Jenny */ }
                    is StreamEvent.MemoSaved -> {
                        viewModelScope.launch {
                            memoRepository.saveMemo(event.title, event.content, "jenny")
                            _uiState.update { it.copy(memoFeedback = "📝 Nota salvata: ${event.title}") }
                        }
                    }
                    is StreamEvent.EventRequested -> {
                        if (prefs.calendarConfirmBeforeAdd) {
                            _uiState.update { it.copy(pendingCalendarEvent = event) }
                        } else {
                            viewModelScope.launch { insertCalendarEvent(event) }
                        }
                    }
                    is StreamEvent.ReminderScheduled -> {
                        scheduleReminder(event.text, event.date, event.time)
                        _uiState.update { it.copy(memoFeedback = "⏰ Promemoria: ${event.text}") }
                    }
                    is StreamEvent.CalendarRead -> {
                        if (!PermissionUtils.areCalendarGranted(context)) {
                            pendingCalendarReadForPermission = event.period
                            _uiState.update { it.copy(permissionNeeded = PermissionNeeded.CALENDAR) }
                        } else {
                            viewModelScope.launch {
                                val calId = prefs.defaultCalendarId
                                if (calId < 0L) {
                                    chatRepository.saveAssistantMessage(JENNY_SESSION_ID, "⚠️ Nessun calendario selezionato nelle impostazioni.")
                                    return@launch
                                }
                                val (startMs, endMs) = periodToRange(event.period)
                                val events = calendarRepository.getEvents(calId, startMs, endMs)
                                chatRepository.saveAssistantMessage(JENNY_SESSION_ID, calendarRepository.formatEventsForDisplay(events))
                            }
                        }
                    }
                }
            }
        }
    }

    fun setOutfit(outfit: JennyOutfit) {
        _uiState.update { it.copy(outfit = outfit) }
        viewModelScope.launch { preferencesRepository.saveJennyOutfit(outfit.name) }
    }

    fun dismissError()              = _uiState.update { it.copy(error = null, voiceError = null) }
    fun dismissFallbackNotice()     = _uiState.update { it.copy(fallbackNotice = null) }
    fun dismissOutfitToast()        = _uiState.update { it.copy(outfitToast = null) }
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
                        chatRepository.saveAssistantMessage(JENNY_SESSION_ID, calendarRepository.formatEventsForDisplay(events))
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
            else -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                Pair(start, cal.timeInMillis)
            }
        }
    }

    fun toggleVoice() {
        val enabled = !_uiState.value.voiceEnabled
        _uiState.update { it.copy(voiceEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.saveVoiceEnabled(enabled) }
        if (!enabled) voicePlaybackService.stopPlayback()
    }

    fun stopSpeaking() = voicePlaybackService.stopPlayback()

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

    private fun onVoiceResult(text: String) {
        _uiState.update { it.copy(inputText = text) }
        sendMessage(overrideText = text)
    }

    private fun speakReply(text: String) {
        viewModelScope.launch {
            voicePlaybackService.speak(
                text    = text,
                voiceId = prefs.jennyVoiceId.ifBlank { "EXAVITQu4vr4xnSDxMaL" },
                apiKey  = prefs.elevenLabsApiKey
            ).onFailure { err ->
                _uiState.update { it.copy(voiceError = "TTS: ${err.message}") }
            }
        }
    }

    private fun deriveAvatarState(
        isListening: Boolean,
        isLoading: Boolean,
        isSpeaking: Boolean,
        isStreaming: Boolean,
    ): AlfredAvatarState = when {
        isSpeaking  -> AlfredAvatarState.TALKING
        isStreaming  -> AlfredAvatarState.TALKING
        isLoading   -> AlfredAvatarState.THINKING
        isListening -> AlfredAvatarState.LISTENING
        else        -> AlfredAvatarState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        speechInputService.release()
        voicePlaybackService.stopPlayback()
    }
}
