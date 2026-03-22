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
import javax.inject.Inject

data class SmartHomeUiState(
    val devices: List<SmartHomeDevice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingDeviceId: String? = null,   // device currently receiving a command
    val expandedDeviceId: String? = null,  // device card expanded for brightness slider
)

@HiltViewModel
class SmartHomeViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState: StateFlow<SmartHomeUiState> = _uiState

    private var pollJob: Job? = null

    init {
        load()
        startPolling()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getDevices()
                .onSuccess { devices ->
                    _uiState.update { it.copy(devices = devices, isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (true) {
                delay(10_000)
                repository.getDevices()
                    .onSuccess { devices ->
                        _uiState.update { it.copy(devices = devices) }
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
                    // Optimistic update — next poll will confirm
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

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
