package com.alfredJenny.app.ui.screens.avatar

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.repository.AvatarRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.ui.components.EyeState
import com.alfredJenny.app.ui.components.JennyOutfit
import com.alfredJenny.app.ui.components.MouthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvatarSlot(
    val filename: String,
    val label: String,
    val hasFilesDir: Boolean,
    val hasAssets: Boolean,
    val thumbnail: ImageBitmap? = null,
)

data class CustomOutfitSlot(
    val index: Int,
    val name: String,
    val filename: String,
    val hasFilesDir: Boolean,
    val thumbnail: ImageBitmap? = null,
)

data class AvatarManagerUiState(
    val jennyBodySlots: List<AvatarSlot> = emptyList(),
    val jennyEyeSlots: List<AvatarSlot> = emptyList(),
    val jennyMouthSlots: List<AvatarSlot> = emptyList(),
    val customOutfits: List<CustomOutfitSlot> = emptyList(),
    val isLoading: Boolean = false,
    val feedback: String? = null,
)

@HiltViewModel
class AvatarManagerViewModel @Inject constructor(
    private val avatarRepository: AvatarRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarManagerUiState())
    val uiState: StateFlow<AvatarManagerUiState> = _uiState

    init {
        loadSlots()
    }

    fun loadSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val bodySlots = JennyOutfit.builtIn().map { outfit ->
                AvatarSlot(
                    filename = outfit.assetFile,
                    label = outfit.label,
                    hasFilesDir = avatarRepository.jennyFileExists(outfit.assetFile),
                    hasAssets = avatarRepository.assetExists(outfit.assetFile),
                    thumbnail = avatarRepository.loadJennyThumbnail(outfit.assetFile),
                )
            }

            val eyeSlots = EyeState.values().map { eye ->
                AvatarSlot(
                    filename = eye.assetFile,
                    label = eye.name.lowercase().replaceFirstChar { it.uppercase() },
                    hasFilesDir = avatarRepository.jennyFileExists(eye.assetFile),
                    hasAssets = avatarRepository.assetExists(eye.assetFile),
                    thumbnail = avatarRepository.loadJennyThumbnail(eye.assetFile),
                )
            }

            val mouthSlots = MouthState.values().map { mouth ->
                AvatarSlot(
                    filename = mouth.assetFile,
                    label = mouth.name.lowercase().replaceFirstChar { it.uppercase() },
                    hasFilesDir = avatarRepository.jennyFileExists(mouth.assetFile),
                    hasAssets = avatarRepository.assetExists(mouth.assetFile),
                    thumbnail = avatarRepository.loadJennyThumbnail(mouth.assetFile),
                )
            }

            val customNames = preferencesRepository.getCustomOutfitNames()
            val customSlots = (0..5).map { i ->
                val filename = "body_custom_$i.png"
                CustomOutfitSlot(
                    index = i,
                    name = customNames.getOrNull(i) ?: "",
                    filename = filename,
                    hasFilesDir = avatarRepository.jennyFileExists(filename),
                    thumbnail = if (avatarRepository.jennyFileExists(filename))
                        avatarRepository.loadJennyThumbnail(filename) else null,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    jennyBodySlots = bodySlots,
                    jennyEyeSlots = eyeSlots,
                    jennyMouthSlots = mouthSlots,
                    customOutfits = customSlots,
                )
            }
        }
    }

    fun importJennyFile(filename: String, uri: Uri) {
        viewModelScope.launch {
            val ok = avatarRepository.saveJennyImage(filename, uri)
            _uiState.update {
                it.copy(feedback = if (ok) "✅ Importato: $filename" else "❌ Errore importazione")
            }
            loadSlots()
        }
    }

    fun removeJennyFile(filename: String) {
        viewModelScope.launch {
            avatarRepository.removeJennyImage(filename)
            _uiState.update { it.copy(feedback = "🗑 Rimosso: $filename") }
            loadSlots()
        }
    }

    fun updateCustomOutfitNameLocal(index: Int, name: String) {
        _uiState.update { state ->
            state.copy(
                customOutfits = state.customOutfits.map {
                    if (it.index == index) it.copy(name = name) else it
                }
            )
        }
    }

    fun saveCustomOutfitName(index: Int, name: String) {
        viewModelScope.launch {
            preferencesRepository.saveCustomOutfitName(index, name)
        }
    }

    fun importCustomOutfit(index: Int, uri: Uri) {
        val filename = "body_custom_$index.png"
        viewModelScope.launch {
            val ok = avatarRepository.saveJennyImage(filename, uri)
            _uiState.update {
                it.copy(feedback = if (ok) "✅ Outfit ${index + 1} importato" else "❌ Errore importazione")
            }
            loadSlots()
        }
    }

    fun removeCustomOutfit(index: Int) {
        viewModelScope.launch {
            avatarRepository.removeJennyImage("body_custom_$index.png")
            _uiState.update { it.copy(feedback = "🗑 Outfit ${index + 1} rimosso") }
            loadSlots()
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }
}
