package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.model.LoginRequestDto
import com.alfredJenny.app.data.model.LoginResponseDto
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import com.alfredJenny.app.data.remote.ApiService
import com.alfredJenny.app.data.remote.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesRepository: PreferencesRepository,
    private val tokenStore: TokenStore
) {
    // Restore session from DataStore on first injection (app launch).
    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = preferencesRepository.userPreferences.first()
            tokenStore.token   = prefs.jwtToken
            tokenStore.role    = prefs.userRole
            tokenStore.baseUrl = prefs.baseUrl.ifBlank { DEFAULT_BASE_URL }
        }
    }

    /** True when a saved JWT token exists in DataStore. */
    val isLoggedIn: Flow<Boolean> =
        preferencesRepository.userPreferences.map { it.jwtToken.isNotBlank() }

    fun isAdmin(): Boolean = tokenStore.role == "admin"

    fun currentRole(): String = tokenStore.role

    suspend fun login(username: String, password: String): Result<LoginResponseDto> {
        return runCatching {
            val response = apiService.login(LoginRequestDto(username, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                // Persist token and role
                tokenStore.token = body.accessToken
                tokenStore.role  = body.role
                preferencesRepository.saveJwtToken(body.accessToken)
                preferencesRepository.saveUserRole(body.role)
                body
            } else {
                error(parseApiError(response.errorBody()?.string(), response.code()))
            }
        }
    }

    suspend fun logout() {
        tokenStore.token = ""
        tokenStore.role  = ""
        preferencesRepository.saveJwtToken("")
        preferencesRepository.saveUserRole("")
    }

    private fun parseApiError(body: String?, code: Int): String {
        if (body.isNullOrBlank()) return "Errore $code"
        return try {
            // FastAPI returns {"detail": "..."}
            val match = """"detail"\s*:\s*"([^"]+)"""".toRegex().find(body)
            match?.groupValues?.getOrNull(1) ?: "Errore $code"
        } catch (_: Exception) {
            "Errore $code"
        }
    }
}
