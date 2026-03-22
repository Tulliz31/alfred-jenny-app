package com.alfredJenny.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.UserPreferences
import com.alfredJenny.app.data.remote.DEFAULT_BASE_URL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_API_KEY               = stringPreferencesKey("api_key")
        val KEY_AI_PROVIDER           = stringPreferencesKey("ai_provider")
        val KEY_MODEL                 = stringPreferencesKey("selected_model")
        val KEY_BASE_URL              = stringPreferencesKey("base_url")
        val KEY_JWT_TOKEN             = stringPreferencesKey("jwt_token")
        val KEY_USER_ROLE             = stringPreferencesKey("user_role")
        val KEY_USERNAME              = stringPreferencesKey("username")
        val KEY_ELEVENLABS_KEY        = stringPreferencesKey("elevenlabs_api_key")
        val KEY_VOICE_ID              = stringPreferencesKey("voice_id")
        val KEY_VOICE_ENABLED         = booleanPreferencesKey("voice_enabled")
        val KEY_JENNY_ENABLED         = booleanPreferencesKey("jenny_enabled")
        val KEY_JENNY_VOICE_ID        = stringPreferencesKey("jenny_voice_id")
        val KEY_JENNY_PERSONALITY     = intPreferencesKey("jenny_personality_level")
        // Memory
        val KEY_MEMORY_ENABLED        = booleanPreferencesKey("memory_enabled")
        val KEY_MEMORY_INTERVAL       = intPreferencesKey("memory_summary_interval")
        val KEY_MAX_CONTEXT_MESSAGES  = intPreferencesKey("max_context_messages")
        // Advanced
        val KEY_HTTP_TIMEOUT          = intPreferencesKey("http_timeout_seconds")
        val KEY_RETRY_COUNT           = intPreferencesKey("retry_count")
        val KEY_DEBUG_MODE            = booleanPreferencesKey("debug_mode")
        val KEY_FALLBACK_ENABLED      = booleanPreferencesKey("provider_fallback_enabled")
        // Smart Home
        val KEY_SMART_HOME_ENABLED    = booleanPreferencesKey("smart_home_enabled")
        // Onboarding
        val KEY_ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        // Jenny outfit
        val KEY_JENNY_OUTFIT          = stringPreferencesKey("jenny_outfit")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            aiProvider = prefs[KEY_AI_PROVIDER]?.let {
                runCatching { AIProvider.valueOf(it) }.getOrDefault(AIProvider.OPENAI)
            } ?: AIProvider.OPENAI,
            apiKey              = prefs[KEY_API_KEY]              ?: "",
            selectedModel       = prefs[KEY_MODEL]                ?: "",
            baseUrl             = prefs[KEY_BASE_URL]             ?: "",
            jwtToken            = prefs[KEY_JWT_TOKEN]            ?: "",
            userRole            = prefs[KEY_USER_ROLE]            ?: "",
            username            = prefs[KEY_USERNAME]             ?: "",
            elevenLabsApiKey    = prefs[KEY_ELEVENLABS_KEY]       ?: "",
            voiceId             = prefs[KEY_VOICE_ID]             ?: "pNInz6obpgDQGcFmaJgB",
            voiceEnabled        = prefs[KEY_VOICE_ENABLED]        ?: false,
            jennyEnabled        = prefs[KEY_JENNY_ENABLED]        ?: false,
            jennyVoiceId        = prefs[KEY_JENNY_VOICE_ID]       ?: "EXAVITQu4vr4xnSDxMaL",
            jennyPersonalityLevel = prefs[KEY_JENNY_PERSONALITY]  ?: 3,
            memoryEnabled       = prefs[KEY_MEMORY_ENABLED]       ?: true,
            memorySummaryInterval = prefs[KEY_MEMORY_INTERVAL]    ?: 20,
            maxContextMessages  = prefs[KEY_MAX_CONTEXT_MESSAGES] ?: 50,
            httpTimeoutSeconds  = prefs[KEY_HTTP_TIMEOUT]         ?: 60,
            retryCount          = prefs[KEY_RETRY_COUNT]          ?: 2,
            debugMode           = prefs[KEY_DEBUG_MODE]           ?: false,
            providerFallbackEnabled = prefs[KEY_FALLBACK_ENABLED] ?: true,
            smartHomeEnabled        = prefs[KEY_SMART_HOME_ENABLED] ?: false,
            onboardingCompleted     = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            jennyOutfit             = prefs[KEY_JENNY_OUTFIT] ?: "CASUAL",
        )
    }

    suspend fun saveApiKey(key: String)              { dataStore.edit { it[KEY_API_KEY]           = key } }
    suspend fun saveAiProvider(provider: AIProvider) { dataStore.edit { it[KEY_AI_PROVIDER]       = provider.name } }
    suspend fun saveModel(model: String)             { dataStore.edit { it[KEY_MODEL]             = model } }
    suspend fun saveBaseUrl(url: String)             { dataStore.edit { it[KEY_BASE_URL]          = url } }
    suspend fun saveJwtToken(token: String)          { dataStore.edit { it[KEY_JWT_TOKEN]         = token } }
    suspend fun saveUserRole(role: String)           { dataStore.edit { it[KEY_USER_ROLE]         = role } }
    suspend fun saveUsername(name: String)           { dataStore.edit { it[KEY_USERNAME]          = name } }
    suspend fun saveElevenLabsKey(key: String)       { dataStore.edit { it[KEY_ELEVENLABS_KEY]    = key } }
    suspend fun saveVoiceId(id: String)              { dataStore.edit { it[KEY_VOICE_ID]          = id } }
    suspend fun saveVoiceEnabled(enabled: Boolean)   { dataStore.edit { it[KEY_VOICE_ENABLED]     = enabled } }
    suspend fun saveJennyEnabled(enabled: Boolean)   { dataStore.edit { it[KEY_JENNY_ENABLED]     = enabled } }
    suspend fun saveJennyVoiceId(id: String)         { dataStore.edit { it[KEY_JENNY_VOICE_ID]    = id } }
    suspend fun saveJennyPersonalityLevel(level: Int){ dataStore.edit { it[KEY_JENNY_PERSONALITY] = level } }
    // Memory
    suspend fun saveMemoryEnabled(enabled: Boolean)  { dataStore.edit { it[KEY_MEMORY_ENABLED]    = enabled } }
    suspend fun saveMemorySummaryInterval(n: Int)    { dataStore.edit { it[KEY_MEMORY_INTERVAL]   = n } }
    suspend fun saveMaxContextMessages(n: Int)       { dataStore.edit { it[KEY_MAX_CONTEXT_MESSAGES] = n } }
    // Advanced
    suspend fun saveHttpTimeout(secs: Int)           { dataStore.edit { it[KEY_HTTP_TIMEOUT]      = secs } }
    suspend fun saveRetryCount(n: Int)               { dataStore.edit { it[KEY_RETRY_COUNT]       = n } }
    suspend fun saveDebugMode(enabled: Boolean)      { dataStore.edit { it[KEY_DEBUG_MODE]        = enabled } }
    suspend fun saveFallbackEnabled(enabled: Boolean){ dataStore.edit { it[KEY_FALLBACK_ENABLED]  = enabled } }
    // Smart Home
    suspend fun saveSmartHomeEnabled(enabled: Boolean){ dataStore.edit { it[KEY_SMART_HOME_ENABLED] = enabled } }
    // Onboarding
    suspend fun saveOnboardingCompleted(done: Boolean){ dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = done } }
    // Jenny outfit
    suspend fun saveJennyOutfit(outfit: String)       { dataStore.edit { it[KEY_JENNY_OUTFIT]          = outfit } }
}
