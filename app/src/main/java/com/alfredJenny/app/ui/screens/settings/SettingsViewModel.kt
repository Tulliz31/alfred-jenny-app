package com.alfredJenny.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.ActivityLogEntry
import com.alfredJenny.app.data.model.ProviderInfo
import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.data.model.UserEntry
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.ui.components.JennyOutfit
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
    // Tuya connection test
    val isTuyaTesting: Boolean = false,
    val tuyaTestResult: String? = null,
    val tuyaTestError: String? = null,
    // Change password
    val showChangePasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val changePasswordError: String? = null,
    val changePasswordSuccess: Boolean = false,
    val isChangingPassword: Boolean = false,
    // Admin: user management
    val users: List<UserEntry> = emptyList(),
    val isLoadingUsers: Boolean = false,
    val usersError: String? = null,
    val showCreateUserDialog: Boolean = false,
    val showEditUserDialog: Boolean = false,
    val editingUser: UserEntry? = null,
    val newUserName: String = "",
    val newUserPassword: String = "",
    val newUserRole: String = "user",
    val userActionError: String? = null,
    val userActionSuccess: String? = null,
    // Admin: activity log
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val isLoadingLog: Boolean = false,
    val logError: String? = null,
    val showActivityLog: Boolean = false,
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

    fun onJennyEnabledChange(enabled: Boolean)      { update { copy(jennyEnabled = enabled) } }
    fun onJennyVoiceIdChange(id: String)            { update { copy(jennyVoiceId = id) } }
    fun onJennyPersonalityLevelChange(level: Int)   { update { copy(jennyPersonalityLevel = level) } }
    fun onJennyAutoOutfitChange(enabled: Boolean)   { update { copy(jennyAutoOutfit = enabled) } }

    fun forceJennyOutfit(outfit: JennyOutfit) {
        update { copy(jennyOutfit = outfit.name) }
        viewModelScope.launch { preferencesRepository.saveJennyOutfit(outfit.name) }
    }

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
    fun onSmartHomeAiControlChange(enabled: Boolean)  { update { copy(smartHomeAiControl = enabled) } }
    fun onTuyaClientIdChange(id: String)              { update { copy(tuyaClientId = id) } }
    fun onTuyaClientSecretChange(s: String)           { update { copy(tuyaClientSecret = s) } }
    fun onTuyaUserIdChange(id: String)                { update { copy(tuyaUserId = id) } }
    fun onTuyaRegionChange(r: String)                 { update { copy(tuyaRegion = r) } }

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

    fun testTuyaConnection() {
        val p = _uiState.value.preferences
        viewModelScope.launch {
            _uiState.update { it.copy(isTuyaTesting = true, tuyaTestResult = null, tuyaTestError = null) }
            smartHomeRepository.updateTuyaConfig(
                clientId = p.tuyaClientId,
                clientSecret = p.tuyaClientSecret,
                userUid = p.tuyaUserId,
                region = p.tuyaRegion,
            ).onFailure { err ->
                _uiState.update { it.copy(isTuyaTesting = false, tuyaTestError = "Errore: ${err.message}") }
                return@launch
            }
            smartHomeRepository.syncDevices()
                .onSuccess { (devices, _) ->
                    _uiState.update {
                        it.copy(
                            isTuyaTesting = false,
                            tuyaTestResult = "Connessione riuscita! ${devices.size} dispositivi trovati.",
                            discoveredDevices = devices,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isTuyaTesting = false, tuyaTestError = "Errore: ${err.message}") }
                }
        }
    }

    fun dismissTuyaTestResult() {
        _uiState.update { it.copy(tuyaTestResult = null, tuyaTestError = null) }
    }

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

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun toggleLightTheme(enabled: Boolean) {
        update { copy(lightTheme = enabled) }
        viewModelScope.launch { preferencesRepository.saveLightTheme(enabled) }
    }

    // ── Change password ───────────────────────────────────────────────────────

    fun showChangePassword() = _uiState.update {
        it.copy(showChangePasswordDialog = true, currentPassword = "", newPassword = "",
            confirmPassword = "", changePasswordError = null, changePasswordSuccess = false)
    }

    fun dismissChangePassword() = _uiState.update {
        it.copy(showChangePasswordDialog = false, changePasswordError = null)
    }

    fun onCurrentPasswordChange(v: String) = _uiState.update { it.copy(currentPassword = v, changePasswordError = null) }
    fun onNewPasswordChange(v: String)     = _uiState.update { it.copy(newPassword = v, changePasswordError = null) }
    fun onConfirmPasswordChange(v: String) = _uiState.update { it.copy(confirmPassword = v, changePasswordError = null) }

    fun submitChangePassword() {
        val s = _uiState.value
        if (s.newPassword != s.confirmPassword) {
            _uiState.update { it.copy(changePasswordError = "Le password non coincidono") }
            return
        }
        if (s.newPassword.length < 4) {
            _uiState.update { it.copy(changePasswordError = "Password troppo corta (min 4 caratteri)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null) }
            authRepository.changeMyPassword(s.currentPassword, s.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isChangingPassword = false, changePasswordSuccess = true,
                        showChangePasswordDialog = false) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isChangingPassword = false,
                        changePasswordError = err.message ?: "Errore cambio password") }
                }
        }
    }

    fun dismissChangePasswordSuccess() = _uiState.update { it.copy(changePasswordSuccess = false) }

    // ── Admin: user management ────────────────────────────────────────────────

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUsers = true, usersError = null) }
            authRepository.listUsers()
                .onSuccess { list -> _uiState.update { it.copy(users = list, isLoadingUsers = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoadingUsers = false, usersError = err.message) } }
        }
    }

    fun showCreateUser() = _uiState.update {
        it.copy(showCreateUserDialog = true, newUserName = "", newUserPassword = "",
            newUserRole = "user", userActionError = null)
    }

    fun dismissCreateUser() = _uiState.update { it.copy(showCreateUserDialog = false, userActionError = null) }

    fun onNewUserNameChange(v: String)     = _uiState.update { it.copy(newUserName = v, userActionError = null) }
    fun onNewUserPasswordChange(v: String) = _uiState.update { it.copy(newUserPassword = v, userActionError = null) }
    fun onNewUserRoleChange(v: String)     = _uiState.update { it.copy(newUserRole = v) }

    fun submitCreateUser() {
        val s = _uiState.value
        if (s.newUserName.isBlank() || s.newUserPassword.isBlank()) {
            _uiState.update { it.copy(userActionError = "Username e password obbligatori") }
            return
        }
        viewModelScope.launch {
            authRepository.createUser(s.newUserName, s.newUserPassword, s.newUserRole)
                .onSuccess { _ ->
                    _uiState.update { it.copy(showCreateUserDialog = false, userActionSuccess = "Utente creato") }
                    loadUsers()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(userActionError = err.message ?: "Errore creazione utente") }
                }
        }
    }

    fun showEditUser(user: UserEntry) = _uiState.update {
        it.copy(showEditUserDialog = true, editingUser = user,
            newUserPassword = "", newUserRole = user.role, userActionError = null)
    }

    fun dismissEditUser() = _uiState.update { it.copy(showEditUserDialog = false, editingUser = null, userActionError = null) }

    fun submitEditUser() {
        val s = _uiState.value
        val user = s.editingUser ?: return
        viewModelScope.launch {
            authRepository.updateUser(
                username = user.username,
                newPassword = s.newUserPassword.ifBlank { null },
                newRole = s.newUserRole.ifBlank { null }
            ).onSuccess { _ ->
                _uiState.update { it.copy(showEditUserDialog = false, editingUser = null,
                    userActionSuccess = "Utente aggiornato") }
                loadUsers()
            }.onFailure { err ->
                _uiState.update { it.copy(userActionError = err.message ?: "Errore aggiornamento utente") }
            }
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            authRepository.deleteUser(username)
                .onSuccess {
                    _uiState.update { it.copy(userActionSuccess = "Utente eliminato") }
                    loadUsers()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(userActionError = err.message ?: "Errore eliminazione") }
                }
        }
    }

    fun dismissUserActionFeedback() = _uiState.update { it.copy(userActionError = null, userActionSuccess = null) }

    // ── Admin: activity log ───────────────────────────────────────────────────

    fun toggleActivityLog() {
        val showing = !_uiState.value.showActivityLog
        _uiState.update { it.copy(showActivityLog = showing) }
        if (showing) loadActivityLog()
    }

    fun loadActivityLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLog = true, logError = null) }
            authRepository.getActivityLog()
                .onSuccess { log -> _uiState.update { it.copy(activityLog = log, isLoadingLog = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoadingLog = false, logError = err.message) } }
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
            preferencesRepository.saveSmartHomeAiControl(p.smartHomeAiControl)
            preferencesRepository.saveTuyaClientId(p.tuyaClientId)
            preferencesRepository.saveTuyaClientSecret(p.tuyaClientSecret)
            preferencesRepository.saveTuyaUserId(p.tuyaUserId)
            preferencesRepository.saveTuyaRegion(p.tuyaRegion)
            preferencesRepository.saveJennyAutoOutfit(p.jennyAutoOutfit)
            preferencesRepository.saveLightTheme(p.lightTheme)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
