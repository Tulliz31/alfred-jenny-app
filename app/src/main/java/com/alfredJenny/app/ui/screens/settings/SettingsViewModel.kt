package com.alfredJenny.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.data.remote.TokenStore
import com.alfredJenny.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun onProviderChange(provider: AIProvider)   { update { copy(aiProvider = provider) } }
    fun onApiKeyChange(key: String)              { update { copy(apiKey = key) } }
    fun onBaseUrlChange(url: String)             { update { copy(baseUrl = url) } }
    fun onElevenLabsKeyChange(key: String)       { update { copy(elevenLabsApiKey = key) } }
    fun onVoiceIdChange(id: String)              { update { copy(voiceId = id) } }
    fun onVoiceEnabledChange(enabled: Boolean)   { update { copy(voiceEnabled = enabled) } }

    private fun update(block: UserPreferences.() -> UserPreferences) {
        _uiState.update { it.copy(preferences = it.preferences.block(), isSaved = false) }
    }

    fun save() {
        viewModelScope.launch {
            val p = _uiState.value.preferences
            preferencesRepository.saveAiProvider(p.aiProvider)
            preferencesRepository.saveApiKey(p.apiKey)
            val effectiveUrl = p.baseUrl.trim().ifBlank { DEFAULT_BASE_URL }
            preferencesRepository.saveBaseUrl(effectiveUrl)
            tokenStore.baseUrl = effectiveUrl
            preferencesRepository.saveElevenLabsKey(p.elevenLabsApiKey)
            preferencesRepository.saveVoiceId(p.voiceId.ifBlank { "pNInz6obpgDQGcFmaJgB" })
            preferencesRepository.saveVoiceEnabled(p.voiceEnabled)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
