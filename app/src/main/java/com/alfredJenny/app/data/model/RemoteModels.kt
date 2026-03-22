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
data class BackendChatRequestDto(
    @Json(name = "companion_id") val companionId: String,
    @Json(name = "messages") val messages: List<BackendChatMessageDto>,
    @Json(name = "personality_level") val personalityLevel: Int = 3,
    @Json(name = "session_id") val sessionId: String = "",
    @Json(name = "summary_context") val summaryContext: String = "",
    @Json(name = "provider_override") val providerOverride: String = ""
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
    @Json(name = "error") val error: String? = null
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

// ── Smart Home / Tuya ────────────────────────────────────────────────────────

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

// ── API error ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "detail") val detail: String?
)
