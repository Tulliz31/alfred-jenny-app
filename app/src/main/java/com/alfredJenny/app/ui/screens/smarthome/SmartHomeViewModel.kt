package com.alfredJenny.app.ui.screens.smarthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SmartHomeUiState(
    val devices: List<SmartHomeDevice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val tuyaConfigured: Boolean = true,     // false when backend returns "non configurato"
    val pendingDeviceId: String? = null,     // device currently receiving a command
    val expandedDeviceId: String? = null,    // device card expanded for brightness slider
    val renamingDeviceId: String? = null,    // device currently being renamed (dialog open)
    val renameInput: String = "",
    val lastUpdated: String = "",
    val commandFeedback: String? = null,     // snackbar message after rename/sync
    val isSyncing: Boolean = false,
)

@HiltViewModel
class SmartHomeViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState: StateFlow<SmartHomeUiState> = _uiState

    private var pollJob: Job? = null

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    init {
        load()
        startPolling()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getDevices()
                .onSuccess { (devices, ts) ->
                    _uiState.update {
                        it.copy(
                            devices = devices,
                            isLoading = false,
                            tuyaConfigured = true,
                            lastUpdated = formatTimestamp(ts),
                        )
                    }
                }
                .onFailure { err ->
                    val msg = err.message ?: "Errore sconosciuto"
                    val notConfigured = "non configurato" in msg.lowercase()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (!notConfigured) msg else null,
                            tuyaConfigured = !notConfigured,
                        )
                    }
                }
        }
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (true) {
                delay(10_000)
                repository.getDevices()
                    .onSuccess { (devices, ts) ->
                        _uiState.update {
                            it.copy(
                                devices = devices,
                                tuyaConfigured = true,
                                lastUpdated = formatTimestamp(ts),
                            )
                        }
                    }
            }
        }
    }

    fun toggleDevice(device: SmartHomeDevice) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeviceId = device.id) }
            val result = if (device.isOn) repository.turnOff(device.id)
                         else repository.turnOn(device.id)
            result
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            pendingDeviceId = null,
                            devices = state.devices.map {
                                if (it.id == device.id) it.copy(isOn = !device.isOn) else it
                            },
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(pendingDeviceId = null, error = err.message) }
                }
        }
    }

    fun setBrightness(device: SmartHomeDevice, level: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeviceId = device.id) }
            repository.setBrightness(device.id, level)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            pendingDeviceId = null,
                            devices = state.devices.map {
                                if (it.id == device.id) it.copy(brightness = level) else it
                            },
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(pendingDeviceId = null, error = err.message) }
                }
        }
    }

    fun expandDevice(deviceId: String?) {
        _uiState.update { it.copy(expandedDeviceId = deviceId) }
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    fun startRename(deviceId: String, currentName: String) {
        _uiState.update {
            it.copy(renamingDeviceId = deviceId, renameInput = currentName)
        }
    }

    fun onRenameInputChange(text: String) {
        _uiState.update { it.copy(renameInput = text) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(renamingDeviceId = null, renameInput = "") }
    }

    fun confirmRename() {
        val deviceId = _uiState.value.renamingDeviceId ?: return
        val newName = _uiState.value.renameInput.trim()
        if (newName.isBlank()) { cancelRename(); return }
        viewModelScope.launch {
            repository.renameDevice(deviceId, newName)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            renamingDeviceId = null,
                            renameInput = "",
                            devices = state.devices.map {
                                if (it.id == deviceId) it.copy(nameCustom = newName) else it
                            },
                            commandFeedback = "✏️ Rinominato in \"$newName\"",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(renamingDeviceId = null, error = err.message) }
                }
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    fun syncDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            repository.syncDevices()
                .onSuccess { (devices, ts) ->
                    _uiState.update {
                        it.copy(
                            devices = devices,
                            isSyncing = false,
                            tuyaConfigured = true,
                            lastUpdated = formatTimestamp(ts),
                            commandFeedback = "🔄 ${devices.size} dispositivi aggiornati",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSyncing = false, error = err.message) }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissCommandFeedback() {
        _uiState.update { it.copy(commandFeedback = null) }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    private fun formatTimestamp(iso: String): String {
        return runCatching {
            timeFmt.format(Instant.parse(iso))
        }.getOrElse {
            val now = java.util.Calendar.getInstance()
            "%02d:%02d".format(now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE))
        }
    }
}
