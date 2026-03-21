package com.alfredJenny.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.ChatMessage
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.domain.usecase.GetConversationHistoryUseCase
import com.alfredJenny.app.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val messages: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val preferencesRepository: PreferencesRepository
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
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            val history = _uiState.value.messages.map {
                ChatMessage(role = it.role, content = it.content)
            }
            val result = sendMessageUseCase(
                sessionId = sessionId,
                userText = text,
                history = history,
                provider = prefs.aiProvider,
                apiKey = prefs.apiKey
            )
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
