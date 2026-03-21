package com.alfredJenny.app.data.remote

import com.alfredJenny.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @GET("auth/me")
    suspend fun getMe(): Response<UserMeDto>

    // ── Chat ──────────────────────────────────────────────────────────────────

    @POST("chat")
    suspend fun chat(@Body request: BackendChatRequestDto): Response<BackendChatResponseDto>

    // ── Companions ────────────────────────────────────────────────────────────

    @GET("companions")
    suspend fun getCompanions(): Response<List<CompanionDto>>

    // ── Providers ─────────────────────────────────────────────────────────────

    @GET("providers")
    suspend fun getProviders(): Response<List<ProviderDto>>

    @PUT("providers/active")
    suspend fun setActiveProvider(@Body request: SetProviderRequestDto): Response<ProviderDto>
}
