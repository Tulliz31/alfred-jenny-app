package com.alfredJenny.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.model.StreamEvent
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.ui.components.AlfredAvatarState
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.domain.usecase.GetConversationHistoryUseCase
import com.alfredJenny.app.services.SpeechInputService
import com.alfredJenny.app.services.VoicePlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val voicePlaybackService: VoicePlaybackService,
    private val speechInputService: SpeechInputService
) : ViewModel() {

    val sessionId: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var prefs: UserPreferences = UserPreferences()

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

    fun dismissError()           = _uiState.update { it.copy(error = null, voiceError = null) }
    fun dismissFallbackNotice()  = _uiState.update { it.copy(fallbackNotice = null) }
    fun dismissCommandFeedback() = _uiState.update { it.copy(commandFeedback = null) }

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
