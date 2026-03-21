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
    @Json(name = "messages") val messages: List<BackendChatMessageDto>
)

@JsonClass(generateAdapter = true)
data class BackendChatResponseDto(
    @Json(name = "reply") val reply: String,
    @Json(name = "companion_id") val companionId: String,
    @Json(name = "provider") val provider: String
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
    @Json(name = "active") val active: Boolean
)

@JsonClass(generateAdapter = true)
data class SetProviderRequestDto(
    @Json(name = "provider") val provider: String
)

// ── API error ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "detail") val detail: String?
)
