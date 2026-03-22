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

    // ── Smart Home v2 (enriched) ─────────────────────────────────────────────

    @GET("devices")
    suspend fun getDevices(): Response<DevicesResponseDto>

    @GET("devices/connectors")
    suspend fun getConnectors(): Response<ConnectorsResponseDto>

    @GET("devices/{deviceId}/status")
    suspend fun getDeviceStatus(@Path("deviceId") deviceId: String): Response<DeviceStatusDto>

    @POST("devices/{deviceId}/command")
    suspend fun sendCommand(
        @Path("deviceId") deviceId: String,
        @Body command: DeviceCommandDto,
    ): Response<CommandResultDto>

    @PUT("devices/{deviceId}/name")
    suspend fun renameDevice(
        @Path("deviceId") deviceId: String,
        @Body request: RenameDeviceDto,
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @POST("devices/sync")
    suspend fun syncDevices(): Response<DevicesResponseDto>

    // ── Admin: legacy discover ─────────────────────────────────────────────────

    @POST("devices/discover")
    suspend fun discoverDevices(): Response<List<TuyaDeviceDto>>

    // ── Admin: Tuya config ────────────────────────────────────────────────────

    @PUT("devices/admin/tuya-config")
    suspend fun updateTuyaConfig(@Body config: TuyaConfigDto): Response<Map<String, @JvmSuppressWildcards Any>>

    // ── Jenny AI config ───────────────────────────────────────────────────────

    @POST("jenny/openrouter/models")
    suspend fun getOpenRouterModels(@Body request: OpenRouterKeyRequestDto): Response<List<OpenRouterModelDto>>

    @POST("jenny/test")
    suspend fun testJennyProvider(@Body request: TestJennyRequestDto): Response<TestJennyResponseDto>
}
