package com.alfredJenny.app.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.CompanionAIConfig
import com.alfredJenny.app.data.model.OpenRouterModel
import com.alfredJenny.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIProviderConfigUiState(
    val companionId: String = "alfred",
    val isLoading: Boolean = false,
    val loadError: String? = null,
    // Config fields
    val enabled: Boolean = false,
    val providerType: String = "openai",   // openai | anthropic | gemini | openrouter | custom
    val apiKey: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val useGlobal: Boolean = false,        // jenny only: true = inherit Alfred's config
    // OpenRouter model browser
    val models: List<OpenRouterModel> = emptyList(),
    val filteredModels: List<OpenRouterModel> = emptyList(),
    val modelFilter: String = "",
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,
    val showModelBrowser: Boolean = false,
    // Test
    val isTesting: Boolean = false,
    val testReply: String? = null,
    val testProvider: String? = null,
    val testError: String? = null,
    // Save
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class AIProviderConfigViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val companionId: String = savedStateHandle["companionId"] ?: "alfred"

    private val _uiState = MutableStateFlow(AIProviderConfigUiState(companionId = companionId))
    val uiState: StateFlow<AIProviderConfigUiState> = _uiState

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            chatRepository.getCompanionConfig(companionId)
                .onSuccess { config ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            enabled = config.enabled,
                            providerType = config.providerType,
                            apiKey = config.apiKey,
                            modelId = config.modelId,
                            baseUrl = config.baseUrl ?: "",
                            useGlobal = config.useGlobal,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, loadError = err.message) }
                }
        }
    }

    // ── Field mutations ───────────────────────────────────────────────────────

    fun onEnabledChange(v: Boolean) = _uiState.update { it.copy(enabled = v, isSaved = false) }
    fun onUseGlobalChange(v: Boolean) = _uiState.update { it.copy(useGlobal = v, isSaved = false) }
    fun onProviderTypeChange(v: String) = _uiState.update {
        it.copy(providerType = v, isSaved = false, models = emptyList(), filteredModels = emptyList())
    }
    fun onApiKeyChange(v: String) = _uiState.update { it.copy(apiKey = v, isSaved = false) }
    fun onModelIdChange(v: String) = _uiState.update { it.copy(modelId = v, isSaved = false) }
    fun onBaseUrlChange(v: String) = _uiState.update { it.copy(baseUrl = v, isSaved = false) }

    // ── Model browser ─────────────────────────────────────────────────────────

    fun showModelBrowser() = _uiState.update { it.copy(showModelBrowser = true) }
    fun dismissModelBrowser() = _uiState.update { it.copy(showModelBrowser = false, modelFilter = "") }

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
            chatRepository.getOpenRouterModelsFromProviders(key)
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

    // ── Test ──────────────────────────────────────────────────────────────────

    fun testConfig() {
        val s = _uiState.value
        if (!s.enabled) {
            _uiState.update { it.copy(testError = "Abilita prima il provider") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testReply = null, testError = null) }
            chatRepository.testProviderConfig(buildConfig(s))
                .onSuccess { (reply, provider) ->
                    _uiState.update { it.copy(isTesting = false, testReply = reply, testProvider = provider) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isTesting = false, testError = err.message) }
                }
        }
    }

    fun dismissTestResult() = _uiState.update { it.copy(testReply = null, testError = null, testProvider = null) }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            chatRepository.setCompanionConfig(companionId, buildConfig(_uiState.value))
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { err -> _uiState.update { it.copy(isSaving = false, saveError = err.message) } }
        }
    }

    fun dismissSaveError() = _uiState.update { it.copy(saveError = null) }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildConfig(s: AIProviderConfigUiState) = CompanionAIConfig(
        companionId = companionId,
        providerType = s.providerType,
        apiKey = s.apiKey,
        modelId = s.modelId,
        baseUrl = s.baseUrl.ifBlank { null },
        enabled = s.enabled,
        useGlobal = s.useGlobal,
    )
}
