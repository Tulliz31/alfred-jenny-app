package com.alfredJenny.app.data.remote

import com.alfredJenny.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Health ────────────────────────────────────────────────────────────────

    @GET("health")
    suspend fun health(): Response<Map<String, @JvmSuppressWildcards Any>>

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @GET("auth/me")
    suspend fun getMe(): Response<UserMeDto>

    // ── Chat (non-streaming) ──────────────────────────────────────────────────

    @POST("chat")
    suspend fun chat(@Body request: BackendChatRequestDto): Response<BackendChatResponseDto>

    // ── Summarize ─────────────────────────────────────────────────────────────

    @POST("chat/summarize")
    suspend fun summarize(@Body request: SummarizeRequestDto): Response<SummarizeResponseDto>

    // ── Companions ────────────────────────────────────────────────────────────

    @GET("companions")
    suspend fun getCompanions(): Response<List<CompanionDto>>

    // ── Voice ────────────────────────────────────────────────────────────────

    @POST("voice/speak")
    suspend fun speak(@Body request: VoiceSpeakRequestDto): Response<VoiceSpeakResponseDto>

    // ── Providers ─────────────────────────────────────────────────────────────

    @GET("providers")
    suspend fun getProviders(): Response<List<ProviderDto>>

    @PUT("providers/active")
    suspend fun setActiveProvider(@Body request: SetProviderRequestDto): Response<ProviderDto>

    // ── Smart Home ────────────────────────────────────────────────────────────

    @GET("devices")
    suspend fun getDevices(): Response<List<TuyaDeviceDto>>

    @GET("devices/{deviceId}/status")
    suspend fun getDeviceStatus(@Path("deviceId") deviceId: String): Response<DeviceStatusDto>

    @POST("devices/{deviceId}/command")
    suspend fun sendCommand(
        @Path("deviceId") deviceId: String,
        @Body command: DeviceCommandDto,
    ): Response<CommandResultDto>

    @POST("devices/discover")
    suspend fun discoverDevices(): Response<List<TuyaDeviceDto>>
}
