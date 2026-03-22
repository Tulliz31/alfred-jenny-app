package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.model.SmartHomeDevice
import com.alfredJenny.app.data.model.DeviceCommandDto
import com.alfredJenny.app.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartHomeRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getDevices(): Result<List<SmartHomeDevice>> = runCatching {
        val resp = apiService.getDevices()
        if (!resp.isSuccessful) error("Errore ${resp.code()}: ${resp.message()}")
        resp.body()?.map { dto ->
            val statusMap = dto.status.associate { it["code"].toString() to it["value"] }
            val isOn = (statusMap["switch_led"] as? Boolean)
                ?: (statusMap["switch"] as? Boolean)
                ?: false
            val brightness = (statusMap["bright_value_v2"] as? Number)?.toInt()
                ?: (statusMap["bright_value"] as? Number)?.toInt()
            val temp = (statusMap["temp_current"] as? Number)?.toFloat()?.let { it / 10f }
            SmartHomeDevice(
                id = dto.id,
                name = dto.name,
                category = dto.category,
                productName = dto.productName,
                online = dto.online,
                isOn = isOn,
                brightness = brightness,
                temperature = temp,
            )
        } ?: emptyList()
    }

    suspend fun refreshDeviceStatus(deviceId: String): Result<SmartHomeDevice> = runCatching {
        val resp = apiService.getDeviceStatus(deviceId)
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
        val dto = resp.body() ?: error("Risposta vuota")
        val statusMap = dto.status.associate { it["code"].toString() to it["value"] }
        val isOn = (statusMap["switch_led"] as? Boolean)
            ?: (statusMap["switch"] as? Boolean)
            ?: false
        val brightness = (statusMap["bright_value_v2"] as? Number)?.toInt()
            ?: (statusMap["bright_value"] as? Number)?.toInt()
        val temp = (statusMap["temp_current"] as? Number)?.toFloat()?.let { it / 10f }
        SmartHomeDevice(
            id = dto.deviceId,
            name = deviceId,
            category = "",
            online = dto.online,
            isOn = isOn,
            brightness = brightness,
            temperature = temp,
        )
    }

    suspend fun turnOn(deviceId: String): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "on"))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
    }

    suspend fun turnOff(deviceId: String): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "off"))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
    }

    suspend fun setBrightness(deviceId: String, level: Int): Result<Unit> = runCatching {
        val resp = apiService.sendCommand(deviceId, DeviceCommandDto(action = "brightness", value = level))
        if (!resp.isSuccessful) error("Errore ${resp.code()}")
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
}
