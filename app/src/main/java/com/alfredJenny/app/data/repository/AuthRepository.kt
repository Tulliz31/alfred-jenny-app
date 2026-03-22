package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.model.ActivityLogEntry
import com.alfredJenny.app.data.model.CreateUserRequestDto
import com.alfredJenny.app.data.model.ChangePasswordRequestDto
import com.alfredJenny.app.data.model.LoginRequestDto
import com.alfredJenny.app.data.model.LoginResponseDto
import com.alfredJenny.app.data.model.UpdateUserRequestDto
import com.alfredJenny.app.data.model.UserEntry
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
                tokenStore.token = body.accessToken
                tokenStore.role  = body.role
                preferencesRepository.saveJwtToken(body.accessToken)
                preferencesRepository.saveUserRole(body.role)
                preferencesRepository.saveUsername(body.username)
                body
            } else {
                error(parseApiError(response.errorBody()?.string(), response.code()))
            }
        }
    }

    /**
     * Re-verifies admin credentials without changing the active session.
     * Used to unlock the Service section in Settings.
     */
    suspend fun verifyAdminPassword(password: String): Boolean {
        val username = preferencesRepository.userPreferences.first().username
        if (username.isBlank()) return false
        return runCatching {
            val response = apiService.login(LoginRequestDto(username, password))
            response.isSuccessful && response.body()?.role == "admin"
        }.getOrDefault(false)
    }

    suspend fun logout() {
        tokenStore.token = ""
        tokenStore.role  = ""
        preferencesRepository.saveJwtToken("")
        preferencesRepository.saveUserRole("")
    }

    suspend fun changeMyPassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = apiService.changeMyPassword(ChangePasswordRequestDto(currentPassword, newPassword))
        if (!response.isSuccessful) error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun listUsers(): Result<List<UserEntry>> = runCatching {
        val response = apiService.listAdminUsers()
        if (response.isSuccessful) response.body()!!.map { UserEntry(it.id, it.username, it.role) }
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun createUser(username: String, password: String, role: String): Result<UserEntry> = runCatching {
        val response = apiService.createAdminUser(CreateUserRequestDto(username, password, role))
        if (response.isSuccessful) response.body()!!.let { UserEntry(it.id, it.username, it.role) }
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun updateUser(username: String, newPassword: String?, newRole: String?): Result<UserEntry> = runCatching {
        val response = apiService.updateAdminUser(username, UpdateUserRequestDto(newRole, newPassword))
        if (response.isSuccessful) response.body()!!.let { UserEntry(it.id, it.username, it.role) }
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun deleteUser(username: String): Result<Unit> = runCatching {
        val response = apiService.deleteAdminUser(username)
        if (!response.isSuccessful) error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    suspend fun getActivityLog(): Result<List<ActivityLogEntry>> = runCatching {
        val response = apiService.getActivityLog()
        if (response.isSuccessful) response.body()!!.map {
            ActivityLogEntry(it.id, it.timestamp, it.username, it.action, it.details)
        }
        else error(parseApiError(response.errorBody()?.string(), response.code()))
    }

    private fun parseApiError(body: String?, code: Int): String {
        if (body.isNullOrBlank()) return "Errore $code"
        return try {
            val match = """"detail"\s*:\s*"([^"]+)"""".toRegex().find(body)
            match?.groupValues?.getOrNull(1) ?: "Errore $code"
        } catch (_: Exception) {
            "Errore $code"
        }
    }
}
