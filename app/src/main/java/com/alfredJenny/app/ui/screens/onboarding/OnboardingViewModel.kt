package com.alfredJenny.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.remote.TokenStore
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    // Step 0 – Backend URL
    val backendUrl: String = "",
    val isTesting: Boolean = false,
    val urlError: String? = null,
    val urlOk: Boolean = false,
    // Step 1 – Login
    val username: String = "",
    val password: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    // Step 2 – API key
    val apiKey: String = "",
    val isSaving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val apiService: ApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    // ── Step 0: URL ───────────────────────────────────────────────────────────

    fun onUrlChange(url: String) {
        _state.update { it.copy(backendUrl = url, urlError = null, urlOk = false) }
    }

    fun testConnection() {
        val url = _state.value.backendUrl.trim().trimEnd('/')
        if (url.isBlank()) {
            _state.update { it.copy(urlError = "Inserisci l'URL del backend") }
            return
        }
        _state.update { it.copy(isTesting = true, urlError = null, urlOk = false) }
        viewModelScope.launch {
            tokenStore.baseUrl = url
            runCatching { apiService.health() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        preferencesRepository.saveBaseUrl(url)
                        _state.update { it.copy(isTesting = false, urlOk = true) }
                    } else {
                        _state.update { it.copy(isTesting = false, urlError = "Server raggiunto ma risposta inattesa (${resp.code()})") }
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isTesting = false, urlError = "Impossibile raggiungere il server: ${err.message}") }
                }
        }
    }

    fun goToLogin() {
        _state.update { it.copy(step = 1, loginError = null) }
    }

    // ── Step 1: Login ─────────────────────────────────────────────────────────

    fun onUsernameChange(v: String) { _state.update { it.copy(username = v, loginError = null) } }
    fun onPasswordChange(v: String) { _state.update { it.copy(password = v, loginError = null) } }

    fun login(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(loginError = "Username e password richiesti") }
            return
        }
        _state.update { it.copy(isLoggingIn = true, loginError = null) }
        viewModelScope.launch {
            authRepository.login(s.username, s.password)
                .onSuccess {
                    _state.update { it.copy(isLoggingIn = false, step = 2) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoggingIn = false, loginError = err.message ?: "Login fallito") }
                }
        }
    }

    // ── Step 2: API key ───────────────────────────────────────────────────────

    fun onApiKeyChange(v: String) { _state.update { it.copy(apiKey = v) } }

    fun finish(onComplete: () -> Unit) {
        val key = _state.value.apiKey.trim()
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (key.isNotBlank()) preferencesRepository.saveApiKey(key)
            preferencesRepository.saveOnboardingCompleted(true)
            _state.update { it.copy(isSaving = false) }
            onComplete()
        }
    }

    fun skipApiKey(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.saveOnboardingCompleted(true)
            onComplete()
        }
    }
}
