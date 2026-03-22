package com.alfredJenny.app.ui.screens.settings

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.ElevenLabsVoice
import com.alfredJenny.app.data.model.VoiceSubscription
import com.alfredJenny.app.data.model.VoicePreviewRequestDto
import com.alfredJenny.app.data.remote.ApiService
import com.alfredJenny.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class VoiceBrowserUiState(
    val companionId: String = "alfred",      // "alfred" or "jenny"
    val voices: List<ElevenLabsVoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterGender: String = "",           // "" = all, "male", "female"
    val filterAccent: String = "",           // "" = all, e.g. "american", "british", "italian"
    val playingVoiceId: String? = null,
    val previewError: String? = null,
    val currentVoiceId: String = "",
    val subscription: VoiceSubscription? = null,
)

@HiltViewModel
class VoiceBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val apiService: ApiService,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val companionId: String = savedStateHandle["companionId"] ?: "alfred"

    private val _uiState = MutableStateFlow(VoiceBrowserUiState(companionId = companionId))
    val uiState: StateFlow<VoiceBrowserUiState> = _uiState

    // Preview cache: voiceId -> audio bytes
    private val previewCache = mutableMapOf<String, ByteArray>()
    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            val currentVoiceId = if (companionId == "jenny") prefs.jennyVoiceId else prefs.voiceId
            _uiState.update { it.copy(currentVoiceId = currentVoiceId) }
            loadVoices(prefs.elevenLabsApiKey.ifBlank { null })
            loadSubscription(prefs.elevenLabsApiKey.ifBlank { null })
        }
    }

    private fun loadVoices(apiKey: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val resp = apiService.getVoices(apiKey)
                if (resp.isSuccessful) {
                    val voices = resp.body()!!.voices.map {
                        ElevenLabsVoice(it.voiceId, it.name, it.category, it.description,
                            it.previewUrl, it.accent, it.gender, it.age, it.useCase)
                    }
                    _uiState.update { it.copy(voices = voices, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Errore ${resp.code()}") }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    private fun loadSubscription(apiKey: String?) {
        viewModelScope.launch {
            runCatching {
                val resp = apiService.getVoiceSubscription(apiKey)
                if (resp.isSuccessful) {
                    resp.body()!!.let { dto ->
                        _uiState.update { it.copy(
                            subscription = VoiceSubscription(dto.tier, dto.characterCount, dto.characterLimit)
                        ) }
                    }
                }
            }
        }
    }

    fun onSearchChange(q: String)      = _uiState.update { it.copy(searchQuery = q) }
    fun onGenderFilter(g: String)      = _uiState.update { it.copy(filterGender = g) }
    fun onAccentFilter(a: String)      = _uiState.update { it.copy(filterAccent = a) }

    fun filteredVoices(): List<ElevenLabsVoice> {
        val s = _uiState.value
        return s.voices.filter { v ->
            (s.searchQuery.isBlank() || v.name.contains(s.searchQuery, ignoreCase = true)) &&
            (s.filterGender.isBlank() || v.gender.equals(s.filterGender, ignoreCase = true)) &&
            (s.filterAccent.isBlank() || v.accent.equals(s.filterAccent, ignoreCase = true))
        }
    }

    fun playPreview(voiceId: String) {
        stopPreview()
        viewModelScope.launch {
            _uiState.update { it.copy(playingVoiceId = voiceId, previewError = null) }
            val cached = previewCache[voiceId]
            val audioBytes: ByteArray = if (cached != null) {
                cached
            } else {
                val prefs = preferencesRepository.userPreferences.first()
                val apiKey = prefs.elevenLabsApiKey.ifBlank { null }
                runCatching {
                    val resp = apiService.previewVoice(voiceId, VoicePreviewRequestDto(apiKey))
                    if (resp.isSuccessful) {
                        Base64.decode(resp.body()!!.audioBase64, Base64.DEFAULT).also {
                            previewCache[voiceId] = it
                        }
                    } else {
                        _uiState.update { it.copy(playingVoiceId = null, previewError = "Errore anteprima ${resp.code()}") }
                        return@launch
                    }
                }.getOrElse { err ->
                    _uiState.update { it.copy(playingVoiceId = null, previewError = err.message) }
                    return@launch
                }
            }

            withContext(Dispatchers.IO) {
                val file = File.createTempFile("preview_", ".mp3", context.cacheDir)
                file.writeBytes(audioBytes)
                tempFile = file
            }

            withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile!!.absolutePath)
                    prepare()
                    setOnCompletionListener { _uiState.update { it.copy(playingVoiceId = null) } }
                    setOnErrorListener { _, _, _ -> _uiState.update { it.copy(playingVoiceId = null) }; false }
                    start()
                }
            }
        }
    }

    fun stopPreview() {
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
        tempFile?.delete()
        tempFile = null
        _uiState.update { it.copy(playingVoiceId = null) }
    }

    fun selectVoice(voiceId: String) {
        stopPreview()
        viewModelScope.launch {
            if (companionId == "jenny") {
                preferencesRepository.saveJennyVoiceId(voiceId)
            } else {
                preferencesRepository.saveVoiceId(voiceId)
            }
            _uiState.update { it.copy(currentVoiceId = voiceId) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
