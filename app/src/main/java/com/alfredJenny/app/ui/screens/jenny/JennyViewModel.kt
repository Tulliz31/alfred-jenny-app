package com.alfredJenny.app.ui.screens.jenny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.services.SpeechInputService
import com.alfredJenny.app.services.VoicePlaybackService
import com.alfredJenny.app.ui.components.AlfredAvatarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Fixed session ID for Jenny — enables targeted conversation clearing
const val JENNY_SESSION_ID = "jenny_companion"

data class JennyUiState(
    val messages: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val voiceMode: VoiceMode = VoiceMode.OUTDOOR,
    val voiceEnabled: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialSpeechText: String = "",
    val voiceError: String? = null,
    val personalityLevel: Int = 3,
    val avatarState: AlfredAvatarState = AlfredAvatarState.IDLE
)

@HiltViewModel
class JennyViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val voicePlaybackService: VoicePlaybackService,
    private val speechInputService: SpeechInputService
) : ViewModel() {

    private val _uiState = MutableStateFlow(JennyUiState())
    val uiState: StateFlow<JennyUiState> = _uiState

    init {
        // Load message history
        viewModelScope.launch {
            chatRepository.getMessages(JENNY_SESSION_ID).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        // Sync prefs
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(voiceEnabled = prefs.voiceEnabled, personalityLevel = prefs.jennyPersonalityLevel) }
            }
        }
        // Voice playback state
        viewModelScope.launch {
            voicePlaybackService.isPlaying.collect { playing ->
                _uiState.update { it.copy(isSpeaking = playing, avatarState = deriveAvatarState(it.isListening, it.isLoading, playing)) }
            }
        }
        // Speech recognition state
        viewModelScope.launch {
            speechInputService.state.collect { s ->
                _uiState.update { it.copy(isListening = s.isListening, partialSpeechText = s.partialText, avatarState = deriveAvatarState(s.isListening, it.isLoading, it.isSpeaking)) }
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

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null, avatarState = AlfredAvatarState.THINKING) }

        viewModelScope.launch {
            chatRepository.sendMessage(
                sessionId = JENNY_SESSION_ID,
                companionId = "jenny",
                userText = text,
                personalityLevel = _uiState.value.personalityLevel
            ).onSuccess { reply ->
                _uiState.update { it.copy(isLoading = false, avatarState = if (it.voiceEnabled) AlfredAvatarState.TALKING else AlfredAvatarState.IDLE) }
                if (_uiState.value.voiceEnabled) speakReply(reply)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message, avatarState = AlfredAvatarState.IDLE) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null, voiceError = null) }

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
            val prefs = preferencesRepository.userPreferences.first()
            voicePlaybackService.speak(
                text    = text,
                voiceId = prefs.jennyVoiceId.ifBlank { "EXAVITQu4vr4xnSDxMaL" },
                apiKey  = prefs.elevenLabsApiKey
            ).onFailure { err ->
                _uiState.update { it.copy(voiceError = "TTS: ${err.message}") }
            }
        }
    }

    private fun deriveAvatarState(isListening: Boolean, isLoading: Boolean, isSpeaking: Boolean): AlfredAvatarState = when {
        isSpeaking  -> AlfredAvatarState.TALKING
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
