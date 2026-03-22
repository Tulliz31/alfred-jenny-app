package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.model.DeviceCommandDto
import com.alfredJenny.app.data.model.RenameDeviceDto
import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.data.model.TuyaConfigDto
import com.alfredJenny.app.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartHomeRepository @Inject constructor(
    private val apiService: ApiService
) {

    // ── Device list ───────────────────────────────────────────────────────────

    suspend fun getDevices(): Result<Pair<List<SmartHomeDevice>, String>> = runCatching {
        val resp = apiService.getDevices()
        if (!resp.isSuccessful) error("Errore ${resp.code()}: ${resp.message()}")
        val body = resp.body() ?: error("Risposta vuota")
        val devices = body.devices.map { dto ->
            SmartHomeDevice(
                id = dto.id,
                name = dto.name,
                nameCustom = dto.nameCustom,
                category = dto.type,
                type = dto.type,
                source = dto.source,
                online = dto.online,
                isOn = dto.status.isOn,
                brightness = dto.status.brightness,
                temperature = dto.status.temperature,
                capabilities = dto.capabilities,
                visible = dto.visible,
            )
        }
        Pair(devices, body.lastUpdated)
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    suspend fun turnOn(deviceId: String): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "turn_on"))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
    }

    suspend fun turnOff(deviceId: String): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "turn_off"))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
    }

    suspend fun setBrightness(deviceId: String, level: Int): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "brightness", value = level))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    suspend fun renameDevice(deviceId: String, newName: String): Result<Unit> = runCatching {
        val resp = apiService.renameDevice(deviceId, RenameDeviceDto(name = newName))
        if (!resp.isSuccessful) error("Errore ${resp.code()}: ${resp.message()}")
    }

    // ── Sync / discover ───────────────────────────────────────────────────────

    suspend fun syncDevices(): Result<Pair<List<SmartHomeDevice>, String>> = runCatching {
        val resp = apiService.syncDevices()
        if (!resp.isSuccessful) error("Errore ${resp.code()}: ${resp.message()}")
        val body = resp.body() ?: error("Risposta vuota")
        val devices = body.devices.map { dto ->
            SmartHomeDevice(
                id = dto.id,
                name = dto.name,
                nameCustom = dto.nameCustom,
                category = dto.type,
                type = dto.type,
                source = dto.source,
                online = dto.online,
                isOn = dto.status.isOn,
                brightness = dto.status.brightness,
                temperature = dto.status.temperature,
                capabilities = dto.capabilities,
                visible = dto.visible,
            )
        }
        Pair(devices, body.lastUpdated)
    }

    suspend fun discoverDevices(): Result<List<SmartHomeDevice>> = runCatching {
        val resp = apiService.discoverDevices()
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
        resp.body()?.map { dto ->
            SmartHomeDevice(
                id = dto.id,
                name = dto.name,
                category = dto.category,
                productName = dto.productName,
                online = dto.online,
            )
        } ?: emptyList()
    }

    // ── Admin: Tuya config ────────────────────────────────────────────────────

    suspend fun updateTuyaConfig(
        clientId: String,
        clientSecret: String,
        userUid: String,
        region: String,
    ): Result<Unit> = runCatching {
        val baseUrl = when (region) {
            "US" -> "https://openapi.tuyaus.com"
            "CN" -> "https://openapi.tuyacn.com"
            "IN" -> "https://openapi.tuyain.com"
            else -> "https://openapi.tuyaeu.com"
        }
        val resp = apiService.updateTuyaConfig(
            TuyaConfigDto(
                clientId = clientId,
                clientSecret = clientSecret,
                userUid = userUid,
                baseUrl = baseUrl,
            )
        )
        if (!resp.isSuccessful) error("Errore ${resp.code()}: ${resp.message()}")
    }

    // ── Connectors status ─────────────────────────────────────────────────────

    suspend fun getConnectors() = runCatching {
        val resp = apiService.getConnectors()
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
        resp.body()?.connectors ?: emptyList()
    }
}
