package com.alfredJenny.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.data.repository.ChatRepository
import com.alfredJenny.app.domain.usecase.GetConversationHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val messages: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val companions: List<CompanionDto> = emptyList(),
    val selectedCompanionId: String = "alfred",
    val isAdmin: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase
) : ViewModel() {

    val sessionId: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            getConversationHistoryUseCase(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        loadCompanions()
    }

    private fun loadCompanions() {
        viewModelScope.launch {
            chatRepository.getCompanions()
                .onSuccess { companions ->
                    _uiState.update {
                        it.copy(
                            companions = companions,
                            isAdmin = authRepository.isAdmin()
                        )
                    }
                }
                .onFailure { err ->
                    // Non-blocking: companions list just stays empty, show subtle error
                    _uiState.update { it.copy(error = err.message) }
                }
        }
    }

    fun onCompanionSelected(companionId: String) {
        if (companionId == _uiState.value.selectedCompanionId) return
        _uiState.update { it.copy(selectedCompanionId = companionId) }
        // Each companion gets its own session — clear and restart
        viewModelScope.launch {
            chatRepository.clearSession(sessionId)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val companionId = state.selectedCompanionId
        if (text.isBlank() || state.isLoading) return

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            chatRepository.sendMessage(sessionId, companionId, text)
                .onSuccess {
                    _uiState.update { s -> s.copy(isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update { s -> s.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
