package com.alfredJenny.app.ui.screens.jenny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.JennyAIConfig
import com.alfredJenny.app.data.model.JennyAIConfigDto
import com.alfredJenny.app.data.model.OpenRouterModel
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JennyAIConfigUiState(
    val enabled: Boolean = false,
    val providerType: String = "openrouter",   // "openrouter" | "custom"
    val apiKey: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    // Model list
    val models: List<OpenRouterModel> = emptyList(),
    val filteredModels: List<OpenRouterModel> = emptyList(),
    val modelFilter: String = "",
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,
    // Test
    val isTesting: Boolean = false,
    val testReply: String? = null,
    val testProvider: String? = null,
    val testError: String? = null,
    // Save
    val isSaved: Boolean = false,
    // Info dialog
    val showKeyInfoDialog: Boolean = false,
)

@HiltViewModel
class JennyAIConfigViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JennyAIConfigUiState())
    val uiState: StateFlow<JennyAIConfigUiState> = _uiState

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val cfg = preferencesRepository.getJennyAiConfig()
            _uiState.update {
                it.copy(
                    enabled = cfg.enabled,
                    providerType = cfg.providerType,
                    apiKey = cfg.apiKey,
                    modelId = cfg.modelId,
                    baseUrl = cfg.baseUrl,
                )
            }
        }
    }

    fun onEnabledChange(v: Boolean)      = _uiState.update { it.copy(enabled = v, isSaved = false) }
    fun onProviderTypeChange(v: String)  = _uiState.update { it.copy(providerType = v, isSaved = false, models = emptyList(), filteredModels = emptyList()) }
    fun onApiKeyChange(v: String)        = _uiState.update { it.copy(apiKey = v, isSaved = false) }
    fun onModelIdChange(v: String)       = _uiState.update { it.copy(modelId = v, isSaved = false) }
    fun onBaseUrlChange(v: String)       = _uiState.update { it.copy(baseUrl = v, isSaved = false) }
    fun showKeyInfo()                    = _uiState.update { it.copy(showKeyInfoDialog = true) }
    fun dismissKeyInfo()                 = _uiState.update { it.copy(showKeyInfoDialog = false) }
    fun dismissTestResult()              = _uiState.update { it.copy(testReply = null, testError = null, testProvider = null) }

    fun onModelFilterChange(q: String) {
        _uiState.update { state ->
            val filtered = if (q.isBlank()) state.models
            else state.models.filter {
                it.id.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }
            state.copy(modelFilter = q, filteredModels = filtered)
        }
    }

    fun loadOpenRouterModels() {
        val key = _uiState.value.apiKey
        if (key.isBlank()) {
            _uiState.update { it.copy(modelsError = "Inserisci prima la API key OpenRouter") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true, modelsError = null) }
            chatRepository.getOpenRouterModels(key)
                .onSuccess { models ->
                    _uiState.update {
                        it.copy(
                            isLoadingModels = false,
                            models = models,
                            filteredModels = models,
                            modelFilter = "",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoadingModels = false, modelsError = err.message) }
                }
        }
    }

    fun testJenny() {
        val s = _uiState.value
        if (!s.enabled || s.apiKey.isBlank() || s.modelId.isBlank()) {
            _uiState.update { it.copy(testError = "Abilita il provider e inserisci API key e modello") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testReply = null, testError = null) }
            val cfg = JennyAIConfigDto(
                enabled = true,
                providerType = s.providerType,
                apiKey = s.apiKey,
                modelId = s.modelId,
                baseUrl = s.baseUrl,
            )
            chatRepository.testJennyProvider(cfg)
                .onSuccess { (reply, provider) ->
                    _uiState.update { it.copy(isTesting = false, testReply = reply, testProvider = provider) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isTesting = false, testError = err.message) }
                }
        }
    }

    fun save() {
        val s = _uiState.value
        viewModelScope.launch {
            preferencesRepository.saveJennyAiEnabled(s.enabled)
            preferencesRepository.saveJennyAiProviderType(s.providerType)
            preferencesRepository.saveJennyAiKey(s.apiKey)
            preferencesRepository.saveJennyAiModelId(s.modelId)
            preferencesRepository.saveJennyAiBaseUrl(s.baseUrl)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
