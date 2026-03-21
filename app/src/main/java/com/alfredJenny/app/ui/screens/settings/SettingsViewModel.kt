package com.alfredJenny.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.UserPreferences
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
    private val preferencesRepository: PreferencesRepository
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

    fun onProviderChange(provider: AIProvider) {
        _uiState.update { it.copy(preferences = it.preferences.copy(aiProvider = provider), isSaved = false) }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(preferences = it.preferences.copy(apiKey = key), isSaved = false) }
    }

    fun save() {
        viewModelScope.launch {
            val prefs = _uiState.value.preferences
            preferencesRepository.saveAiProvider(prefs.aiProvider)
            preferencesRepository.saveApiKey(prefs.apiKey)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
