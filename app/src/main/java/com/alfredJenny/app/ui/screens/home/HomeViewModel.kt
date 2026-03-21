package com.alfredJenny.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.AuthRepository
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
    val voiceError: String? = null
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

    init {
        // Message history
        viewModelScope.launch {
            getConversationHistoryUseCase(sessionId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        // Companions + admin flag
        loadCompanions()
        // Sync voice prefs
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(voiceEnabled = prefs.voiceEnabled) }
            }
        }
        // Mirror isPlaying from playback service
        viewModelScope.launch {
            voicePlaybackService.isPlaying.collect { playing ->
                _uiState.update { it.copy(isSpeaking = playing) }
            }
        }
        // Mirror speech recognition state
        viewModelScope.launch {
            speechInputService.state.collect { s ->
                _uiState.update { it.copy(isListening = s.isListening, partialSpeechText = s.partialText) }
            }
        }
        // Wire STT callbacks
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

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            chatRepository.sendMessage(sessionId, companionId, text)
                .onSuccess { reply ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (_uiState.value.voiceEnabled) speakReply(reply)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null, voiceError = null) }

    // ── Voice mode toggle ─────────────────────────────────────────────────────

    fun toggleVoiceMode() {
        val next = if (_uiState.value.voiceMode == VoiceMode.OUTDOOR) VoiceMode.CASA else VoiceMode.OUTDOOR
        _uiState.update { it.copy(voiceMode = next) }
        if (next == VoiceMode.CASA) {
            speechInputService.startCasaListening()
        } else {
            speechInputService.stopListening()
        }
    }

    // ── Outdoor push-to-talk ──────────────────────────────────────────────────

    fun startOutdoorListening() {
        if (_uiState.value.voiceMode != VoiceMode.OUTDOOR) return
        speechInputService.startOutdoorListening()
    }

    fun stopOutdoorListening() {
        if (_uiState.value.voiceMode != VoiceMode.OUTDOOR) return
        speechInputService.stopListening()
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

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
            val prefs = preferencesRepository.userPreferences.first()
            voicePlaybackService.speak(
                text    = text,
                voiceId = prefs.voiceId,
                apiKey  = prefs.elevenLabsApiKey
            ).onFailure { err ->
                _uiState.update { it.copy(voiceError = "TTS: ${err.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechInputService.release()
        voicePlaybackService.stopPlayback()
    }
}
