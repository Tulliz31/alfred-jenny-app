package com.alfredJenny.app.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.local.MemoEntity
import com.alfredJenny.app.data.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val memos: List<MemoEntity> = emptyList(),
    val editingMemo: MemoEntity? = null,       // null = not editing
    val editTitle: String = "",
    val editContent: String = "",
    val showDeleteConfirm: MemoEntity? = null,
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState

    init {
        viewModelScope.launch {
            memoRepository.getAllMemos().collect { list ->
                _uiState.update { it.copy(memos = list) }
            }
        }
    }

    fun startEdit(memo: MemoEntity) {
        _uiState.update {
            it.copy(editingMemo = memo, editTitle = memo.title, editContent = memo.content)
        }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(editingMemo = MemoEntity(title = "", content = ""), editTitle = "", editContent = "")
        }
    }

    fun onEditTitle(title: String)     { _uiState.update { it.copy(editTitle = title) } }
    fun onEditContent(content: String) { _uiState.update { it.copy(editContent = content) } }

    fun saveEdit() {
        val state = _uiState.value
        val memo = state.editingMemo ?: return
        val title = state.editTitle.trim()
        val content = state.editContent.trim()
        if (title.isBlank()) { cancelEdit(); return }
        viewModelScope.launch {
            if (memo.id == 0L) {
                memoRepository.saveMemo(title, content, memo.companion)
            } else {
                memoRepository.updateMemo(memo.copy(title = title, content = content))
            }
            _uiState.update { it.copy(editingMemo = null) }
        }
    }

    fun cancelEdit() { _uiState.update { it.copy(editingMemo = null) } }

    fun togglePin(memo: MemoEntity) {
        viewModelScope.launch { memoRepository.togglePin(memo) }
    }

    fun requestDelete(memo: MemoEntity) { _uiState.update { it.copy(showDeleteConfirm = memo) } }
    fun cancelDelete()                  { _uiState.update { it.copy(showDeleteConfirm = null) } }

    fun confirmDelete() {
        val memo = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            memoRepository.deleteMemo(memo.id)
            _uiState.update { it.copy(showDeleteConfirm = null) }
        }
    }
}
