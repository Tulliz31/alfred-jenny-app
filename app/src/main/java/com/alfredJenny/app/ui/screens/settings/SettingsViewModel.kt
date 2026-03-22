package com.alfredJenny.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ProviderInfo
import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.data.remote.TokenStore
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isSaved: Boolean = false,
    // Provider management
    val providers: List<ProviderInfo> = emptyList(),
    val isLoadingProviders: Boolean = false,
    val providerError: String? = null,
    val isSettingProvider: Boolean = false,
    // Service section unlock
    val showPasswordField: Boolean = false,
    val passwordInput: String = "",
    val passwordError: String? = null,
    val isVerifyingPassword: Boolean = false,
    val serviceSectionUnlocked: Boolean = false,
    // Jenny management
    val showClearJennyDialog: Boolean = false,
    val jennyConversationsCleared: Boolean = false,
    // Smart Home
    val isDiscoveringDevices: Boolean = false,
    val discoveredDevices: List<SmartHomeDevice> = emptyList(),
    val discoveryError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val tokenStore: TokenStore,
    private val smartHomeRepository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        loadProviders()
    }

    // ── Standard settings ─────────────────────────────────────────────────────

    fun onProviderChange(provider: AIProvider)   { update { copy(aiProvider = provider) } }
    fun onApiKeyChange(key: String)              { update { copy(apiKey = key) } }
    fun onBaseUrlChange(url: String)             { update { copy(baseUrl = url) } }
    fun onElevenLabsKeyChange(key: String)       { update { copy(elevenLabsApiKey = key) } }
    fun onVoiceIdChange(id: String)              { update { copy(voiceId = id) } }
    fun onVoiceEnabledChange(enabled: Boolean)   { update { copy(voiceEnabled = enabled) } }

    // ── Jenny / Servizio ──────────────────────────────────────────────────────

    fun onJennyEnabledChange(enabled: Boolean)   { update { copy(jennyEnabled = enabled) } }
    fun onJennyVoiceIdChange(id: String)         { update { copy(jennyVoiceId = id) } }
    fun onJennyPersonalityLevelChange(level: Int){ update { copy(jennyPersonalityLevel = level) } }

    // ── Memory ────────────────────────────────────────────────────────────────

    fun onMemoryEnabledChange(enabled: Boolean)        { update { copy(memoryEnabled = enabled) } }
    fun onMemorySummaryIntervalChange(n: Int)          { update { copy(memorySummaryInterval = n) } }
    fun onMaxContextMessagesChange(n: Int)             { update { copy(maxContextMessages = n) } }

    // ── Advanced ──────────────────────────────────────────────────────────────

    fun onHttpTimeoutChange(secs: Int)                 { update { copy(httpTimeoutSeconds = secs) } }
    fun onRetryCountChange(n: Int)                     { update { copy(retryCount = n) } }
    fun onDebugModeChange(enabled: Boolean)            { update { copy(debugMode = enabled) } }
    fun onFallbackEnabledChange(enabled: Boolean)      { update { copy(providerFallbackEnabled = enabled) } }
    fun onSmartHomeEnabledChange(enabled: Boolean)     { update { copy(smartHomeEnabled = enabled) } }

    // ── Provider management (backend) ─────────────────────────────────────────

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProviders = true, providerError = null) }
            chatRepository.getProviders()
                .onSuccess { list -> _uiState.update { it.copy(providers = list, isLoadingProviders = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoadingProviders = false, providerError = err.message) } }
        }
    }

    fun setActiveProvider(providerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingProvider = true, providerError = null) }
            chatRepository.setActiveProvider(providerId)
                .onSuccess { _ ->
                    // Refresh the list to show updated active flag
                    chatRepository.getProviders()
                        .onSuccess { list -> _uiState.update { it.copy(providers = list, isSettingProvider = false) } }
                        .onFailure { _uiState.update { it.copy(isSettingProvider = false) } }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSettingProvider = false, providerError = err.message) }
                }
        }
    }

    fun dismissProviderError() = _uiState.update { it.copy(providerError = null) }

    // ── Service section unlock ────────────────────────────────────────────────

    fun onTripleTap() {
        if (_uiState.value.serviceSectionUnlocked) return
        _uiState.update { it.copy(showPasswordField = true, passwordError = null, passwordInput = "") }
    }

    fun onPasswordChange(pwd: String) {
        _uiState.update { it.copy(passwordInput = pwd, passwordError = null) }
    }

    fun unlockServiceSection() {
        val pwd = _uiState.value.passwordInput
        if (pwd.isBlank()) return
        _uiState.update { it.copy(isVerifyingPassword = true, passwordError = null) }
        viewModelScope.launch {
            val ok = authRepository.verifyAdminPassword(pwd)
            _uiState.update {
                if (ok) it.copy(isVerifyingPassword = false, serviceSectionUnlocked = true,
                                showPasswordField = false, passwordInput = "", passwordError = null)
                else it.copy(isVerifyingPassword = false,
                             passwordError = "Password errata o account non admin")
            }
        }
    }

    fun dismissPasswordField() {
        _uiState.update { it.copy(showPasswordField = false, passwordInput = "", passwordError = null) }
    }

    // ── Jenny conversation management ─────────────────────────────────────────

    fun requestClearJennyConversations() {
        _uiState.update { it.copy(showClearJennyDialog = true) }
    }

    fun dismissClearJennyDialog() {
        _uiState.update { it.copy(showClearJennyDialog = false) }
    }

    fun confirmClearJennyConversations() {
        viewModelScope.launch {
            chatRepository.clearSession("jenny_companion")
            _uiState.update { it.copy(showClearJennyDialog = false, jennyConversationsCleared = true) }
        }
    }

    fun dismissClearedNotice() {
        _uiState.update { it.copy(jennyConversationsCleared = false) }
    }

    // ── Smart Home ────────────────────────────────────────────────────────────

    fun discoverDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringDevices = true, discoveryError = null) }
            smartHomeRepository.discoverDevices()
                .onSuccess { devices ->
                    _uiState.update { it.copy(isDiscoveringDevices = false, discoveredDevices = devices) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isDiscoveringDevices = false, discoveryError = err.message) }
                }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

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
            preferencesRepository.saveJennyEnabled(p.jennyEnabled)
            preferencesRepository.saveJennyVoiceId(p.jennyVoiceId.ifBlank { "EXAVITQu4vr4xnSDxMaL" })
            preferencesRepository.saveJennyPersonalityLevel(p.jennyPersonalityLevel)
            preferencesRepository.saveMemoryEnabled(p.memoryEnabled)
            preferencesRepository.saveMemorySummaryInterval(p.memorySummaryInterval)
            preferencesRepository.saveMaxContextMessages(p.maxContextMessages)
            preferencesRepository.saveHttpTimeout(p.httpTimeoutSeconds)
            preferencesRepository.saveRetryCount(p.retryCount)
            preferencesRepository.saveDebugMode(p.debugMode)
            preferencesRepository.saveFallbackEnabled(p.providerFallbackEnabled)
            preferencesRepository.saveSmartHomeEnabled(p.smartHomeEnabled)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
