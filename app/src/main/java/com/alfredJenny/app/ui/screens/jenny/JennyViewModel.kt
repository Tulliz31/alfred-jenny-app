package com.alfredJenny.app.ui.screens.jenny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.StreamEvent
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.model.VoiceMode
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.services.SpeechInputService
import com.alfredJenny.app.services.VoicePlaybackService
import com.alfredJenny.app.ui.components.AlfredAvatarState
import com.alfredJenny.app.ui.components.EmotionDetector
import com.alfredJenny.app.ui.components.EyeState
import com.alfredJenny.app.ui.components.JennyOutfit
import com.alfredJenny.app.ui.components.OutfitManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private var prefs: UserPreferences = UserPreferences()

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

        _uiState.update {
            it.copy(inputText = "", isLoading = true, streamingContent = "",
                    error = null, fallbackNotice = null,
                    avatarState = AlfredAvatarState.THINKING)
        }

        var ttsSentTriggered = false

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
                        if (!ttsSentTriggered && _uiState.value.voiceEnabled) {
                            val end = newContent.indexOfFirst { c -> c in ".!?;" }
                            if (end > 15) {
                                ttsSentTriggered = true
                                speakReply(newContent.substring(0, end + 1))
                                _uiState.update { it.copy(avatarState = AlfredAvatarState.TALKING) }
                            }
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

    fun setOutfit(outfit: JennyOutfit) {
        _uiState.update { it.copy(outfit = outfit) }
        viewModelScope.launch { preferencesRepository.saveJennyOutfit(outfit.name) }
    }

    fun dismissError()         = _uiState.update { it.copy(error = null, voiceError = null) }
    fun dismissFallbackNotice()= _uiState.update { it.copy(fallbackNotice = null) }

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
