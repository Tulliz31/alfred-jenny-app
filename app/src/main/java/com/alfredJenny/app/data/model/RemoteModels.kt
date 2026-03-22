package com.alfredJenny.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Auth ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "role") val role: String,
    @Json(name = "username") val username: String
)

@JsonClass(generateAdapter = true)
data class UserMeDto(
    @Json(name = "username") val username: String,
    @Json(name = "role") val role: String
)

// ── Chat ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BackendChatMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class JennyAIConfigDto(
    @Json(name = "enabled") val enabled: Boolean = false,
    @Json(name = "provider_type") val providerType: String = "openrouter",
    @Json(name = "api_key") val apiKey: String = "",
    @Json(name = "model_id") val modelId: String = "",
    @Json(name = "base_url") val baseUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class BackendChatRequestDto(
    @Json(name = "companion_id") val companionId: String,
    @Json(name = "messages") val messages: List<BackendChatMessageDto>,
    @Json(name = "personality_level") val personalityLevel: Int = 3,
    @Json(name = "session_id") val sessionId: String = "",
    @Json(name = "summary_context") val summaryContext: String = "",
    @Json(name = "provider_override") val providerOverride: String = "",
    @Json(name = "jenny_ai_config") val jennyAiConfig: JennyAIConfigDto? = null,
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelPricingDto(
    @Json(name = "prompt") val prompt: Double? = null,
    @Json(name = "completion") val completion: Double? = null,
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "context_length") val contextLength: Int? = null,
    @Json(name = "prompt_cost") val promptCost: Double? = null,
    @Json(name = "completion_cost") val completionCost: Double? = null,
    @Json(name = "is_free") val isFree: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class OpenRouterKeyRequestDto(
    @Json(name = "api_key") val apiKey: String,
)

@JsonClass(generateAdapter = true)
data class TestJennyRequestDto(
    @Json(name = "jenny_ai_config") val jennyAiConfig: JennyAIConfigDto,
)

@JsonClass(generateAdapter = true)
data class TestJennyResponseDto(
    @Json(name = "reply") val reply: String,
    @Json(name = "provider") val provider: String,
)

@JsonClass(generateAdapter = true)
data class BackendChatResponseDto(
    @Json(name = "reply") val reply: String,
    @Json(name = "companion_id") val companionId: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "fallback_used") val fallbackUsed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SummarizeRequestDto(
    @Json(name = "messages") val messages: List<BackendChatMessageDto>
)

@JsonClass(generateAdapter = true)
data class SummarizeResponseDto(
    @Json(name = "summary") val summary: String
)

// ── SSE stream chunk ──────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class StreamChunkDto(
    @Json(name = "c") val chunk: String? = null,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "fallback") val fallback: String? = null,
    @Json(name = "done") val done: Boolean = false,
    @Json(name = "error") val error: String? = null,
    @Json(name = "cmd_ok") val cmdOk: String? = null,   // "device_name:action"
    @Json(name = "cmd_err") val cmdErr: String? = null  // "device_name:error"
)

// ── Companions ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CompanionDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "avatar_color") val avatarColor: String,
    @Json(name = "locked") val locked: Boolean
)

// ── Providers ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ProviderDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "default_model") val defaultModel: String,
    @Json(name = "active") val active: Boolean,
    @Json(name = "price_per_1k_input") val pricePerKInput: Float = 0f,
    @Json(name = "price_per_1k_output") val pricePerKOutput: Float = 0f,
    @Json(name = "avg_latency_ms") val avgLatencyMs: Int = 1500
)

@JsonClass(generateAdapter = true)
data class SetProviderRequestDto(
    @Json(name = "provider") val provider: String
)

// ── Voice / TTS ───────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class VoiceSpeakRequestDto(
    @Json(name = "text") val text: String,
    @Json(name = "voice_id") val voiceId: String = "pNInz6obpgDQGcFmaJgB",
    @Json(name = "api_key") val apiKey: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceSpeakResponseDto(
    @Json(name = "audio_base64") val audioBase64: String,
    @Json(name = "format") val format: String,
    @Json(name = "voice_id") val voiceId: String
)

// ── Smart Home / Tuya (legacy) ────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TuyaDeviceDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "product_name") val productName: String = "",
    @Json(name = "online") val online: Boolean = false,
    @Json(name = "status") val status: List<Map<String, @JvmSuppressWildcards Any>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "online") val online: Boolean,
    @Json(name = "status") val status: List<Map<String, @JvmSuppressWildcards Any>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviceCommandDto(
    @Json(name = "action") val action: String,
    @Json(name = "value") val value: Int? = null
)

@JsonClass(generateAdapter = true)
data class CommandResultDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "action") val action: String,
    @Json(name = "message") val message: String = ""
)

// ── Smart Home v2 (enriched) ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DeviceStatusInfoDto(
    @Json(name = "is_on") val isOn: Boolean = false,
    @Json(name = "brightness") val brightness: Int? = null,
    @Json(name = "colour") val colour: String? = null,
    @Json(name = "temperature") val temperature: Float? = null,
)

@JsonClass(generateAdapter = true)
data class DeviceEnrichedDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "name_custom") val nameCustom: String = "",
    @Json(name = "type") val type: String = "switch",
    @Json(name = "source") val source: String = "tuya",
    @Json(name = "online") val online: Boolean = false,
    @Json(name = "status") val status: DeviceStatusInfoDto = DeviceStatusInfoDto(),
    @Json(name = "capabilities") val capabilities: List<String> = emptyList(),
    @Json(name = "visible") val visible: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class DevicesResponseDto(
    @Json(name = "devices") val devices: List<DeviceEnrichedDto>,
    @Json(name = "last_updated") val lastUpdated: String = "",
)

@JsonClass(generateAdapter = true)
data class RenameDeviceDto(
    @Json(name = "name") val name: String,
)

@JsonClass(generateAdapter = true)
data class ConnectorInfoDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "connected") val connected: Boolean,
    @Json(name = "device_count") val deviceCount: Int = 0,
    @Json(name = "status") val status: String = "",
)

@JsonClass(generateAdapter = true)
data class ConnectorsResponseDto(
    @Json(name = "connectors") val connectors: List<ConnectorInfoDto>,
)

@JsonClass(generateAdapter = true)
data class TuyaConfigDto(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "client_secret") val clientSecret: String,
    @Json(name = "user_uid") val userUid: String = "",
    @Json(name = "base_url") val baseUrl: String = "https://openapi.tuyaeu.com",
)

// ── API error ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "detail") val detail: String?
)
