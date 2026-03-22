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
import kotlinx.coroutines.flow.first
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
        val KEY_SMART_HOME_AI_CONTROL = booleanPreferencesKey("smart_home_ai_control")
        // Tuya credentials
        val KEY_TUYA_CLIENT_ID        = stringPreferencesKey("tuya_client_id")
        val KEY_TUYA_CLIENT_SECRET    = stringPreferencesKey("tuya_client_secret")
        val KEY_TUYA_USER_ID          = stringPreferencesKey("tuya_user_id")
        val KEY_TUYA_REGION           = stringPreferencesKey("tuya_region")
        // Onboarding
        val KEY_ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        // Jenny outfit
        val KEY_JENNY_OUTFIT          = stringPreferencesKey("jenny_outfit")
        val KEY_JENNY_AUTO_OUTFIT     = booleanPreferencesKey("jenny_auto_outfit")
        // Jenny dedicated AI provider
        val KEY_JENNY_AI_ENABLED       = booleanPreferencesKey("jenny_ai_enabled")
        val KEY_JENNY_AI_PROVIDER_TYPE = stringPreferencesKey("jenny_ai_provider_type")
        val KEY_JENNY_AI_KEY           = stringPreferencesKey("jenny_ai_key")
        val KEY_JENNY_AI_MODEL_ID      = stringPreferencesKey("jenny_ai_model_id")
        val KEY_JENNY_AI_BASE_URL      = stringPreferencesKey("jenny_ai_base_url")
        // Theme
        val KEY_LIGHT_THEME           = booleanPreferencesKey("light_theme")
        // Notes & Calendar
        val KEY_NOTES_ENABLED            = booleanPreferencesKey("notes_enabled")
        val KEY_DEFAULT_CALENDAR_ID      = stringPreferencesKey("default_calendar_id")
        val KEY_CALENDAR_CONFIRM         = booleanPreferencesKey("calendar_confirm_before_add")
        val KEY_GOOGLE_CALENDAR_EMAIL    = stringPreferencesKey("google_calendar_email")
        // Custom outfit names (6 slots)
        val KEY_CUSTOM_OUTFIT_0_NAME  = stringPreferencesKey("custom_outfit_0_name")
        val KEY_CUSTOM_OUTFIT_1_NAME  = stringPreferencesKey("custom_outfit_1_name")
        val KEY_CUSTOM_OUTFIT_2_NAME  = stringPreferencesKey("custom_outfit_2_name")
        val KEY_CUSTOM_OUTFIT_3_NAME  = stringPreferencesKey("custom_outfit_3_name")
        val KEY_CUSTOM_OUTFIT_4_NAME  = stringPreferencesKey("custom_outfit_4_name")
        val KEY_CUSTOM_OUTFIT_5_NAME  = stringPreferencesKey("custom_outfit_5_name")
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
            smartHomeEnabled        = prefs[KEY_SMART_HOME_ENABLED]    ?: false,
            smartHomeAiControl      = prefs[KEY_SMART_HOME_AI_CONTROL] ?: true,
            tuyaClientId        = prefs[KEY_TUYA_CLIENT_ID]       ?: "",
            tuyaClientSecret    = prefs[KEY_TUYA_CLIENT_SECRET]   ?: "",
            tuyaUserId          = prefs[KEY_TUYA_USER_ID]         ?: "",
            tuyaRegion          = prefs[KEY_TUYA_REGION]          ?: "EU",
            onboardingCompleted     = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            jennyOutfit             = prefs[KEY_JENNY_OUTFIT]      ?: "CASUAL",
            jennyAutoOutfit         = prefs[KEY_JENNY_AUTO_OUTFIT] ?: true,
            lightTheme              = prefs[KEY_LIGHT_THEME]       ?: false,
            notesEnabled             = prefs[KEY_NOTES_ENABLED]          ?: true,
            defaultCalendarId        = prefs[KEY_DEFAULT_CALENDAR_ID]     ?: "",
            calendarConfirmBeforeAdd = prefs[KEY_CALENDAR_CONFIRM]        ?: true,
            googleCalendarEmail      = prefs[KEY_GOOGLE_CALENDAR_EMAIL]   ?: "",
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
    suspend fun saveSmartHomeEnabled(enabled: Boolean)   { dataStore.edit { it[KEY_SMART_HOME_ENABLED]    = enabled } }
    suspend fun saveSmartHomeAiControl(enabled: Boolean) { dataStore.edit { it[KEY_SMART_HOME_AI_CONTROL] = enabled } }
    // Tuya credentials
    suspend fun saveTuyaClientId(id: String)         { dataStore.edit { it[KEY_TUYA_CLIENT_ID]     = id } }
    suspend fun saveTuyaClientSecret(s: String)      { dataStore.edit { it[KEY_TUYA_CLIENT_SECRET] = s } }
    suspend fun saveTuyaUserId(id: String)           { dataStore.edit { it[KEY_TUYA_USER_ID]       = id } }
    suspend fun saveTuyaRegion(r: String)            { dataStore.edit { it[KEY_TUYA_REGION]        = r } }
    // Onboarding
    suspend fun saveOnboardingCompleted(done: Boolean){ dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = done } }
    // Jenny dedicated AI provider
    suspend fun saveJennyAiEnabled(enabled: Boolean)     { dataStore.edit { it[KEY_JENNY_AI_ENABLED]       = enabled } }
    suspend fun saveJennyAiProviderType(type: String)    { dataStore.edit { it[KEY_JENNY_AI_PROVIDER_TYPE] = type } }
    suspend fun saveJennyAiKey(key: String)              { dataStore.edit { it[KEY_JENNY_AI_KEY]           = key } }
    suspend fun saveJennyAiModelId(id: String)           { dataStore.edit { it[KEY_JENNY_AI_MODEL_ID]      = id } }
    suspend fun saveJennyAiBaseUrl(url: String)          { dataStore.edit { it[KEY_JENNY_AI_BASE_URL]      = url } }
    suspend fun getJennyAiConfig(): com.alfredJenny.app.data.model.JennyAIConfig = dataStore.data.map { prefs ->
        com.alfredJenny.app.data.model.JennyAIConfig(
            enabled      = prefs[KEY_JENNY_AI_ENABLED]       ?: false,
            providerType = prefs[KEY_JENNY_AI_PROVIDER_TYPE] ?: "openrouter",
            apiKey       = prefs[KEY_JENNY_AI_KEY]           ?: "",
            modelId      = prefs[KEY_JENNY_AI_MODEL_ID]      ?: "",
            baseUrl      = prefs[KEY_JENNY_AI_BASE_URL]      ?: "",
        )
    }.first()
    // Jenny outfit
    suspend fun saveJennyOutfit(outfit: String)         { dataStore.edit { it[KEY_JENNY_OUTFIT]          = outfit } }
    suspend fun saveJennyAutoOutfit(enabled: Boolean)   { dataStore.edit { it[KEY_JENNY_AUTO_OUTFIT]     = enabled } }
    // Theme
    suspend fun saveLightTheme(enabled: Boolean)        { dataStore.edit { it[KEY_LIGHT_THEME]           = enabled } }
    // Notes & Calendar
    suspend fun saveNotesEnabled(enabled: Boolean)          { dataStore.edit { it[KEY_NOTES_ENABLED]           = enabled } }
    suspend fun saveDefaultCalendarId(id: String)           { dataStore.edit { it[KEY_DEFAULT_CALENDAR_ID]     = id } }
    suspend fun saveCalendarConfirm(confirm: Boolean)       { dataStore.edit { it[KEY_CALENDAR_CONFIRM]        = confirm } }
    suspend fun saveGoogleCalendarEmail(email: String)      { dataStore.edit { it[KEY_GOOGLE_CALENDAR_EMAIL]   = email } }
    // Custom outfit names
    suspend fun getCustomOutfitNames(): List<String> = dataStore.data.map { prefs ->
        listOf(
            prefs[KEY_CUSTOM_OUTFIT_0_NAME] ?: "",
            prefs[KEY_CUSTOM_OUTFIT_1_NAME] ?: "",
            prefs[KEY_CUSTOM_OUTFIT_2_NAME] ?: "",
            prefs[KEY_CUSTOM_OUTFIT_3_NAME] ?: "",
            prefs[KEY_CUSTOM_OUTFIT_4_NAME] ?: "",
            prefs[KEY_CUSTOM_OUTFIT_5_NAME] ?: "",
        )
    }.first()
    suspend fun saveCustomOutfitName(index: Int, name: String) {
        val key = when (index) {
            0 -> KEY_CUSTOM_OUTFIT_0_NAME
            1 -> KEY_CUSTOM_OUTFIT_1_NAME
            2 -> KEY_CUSTOM_OUTFIT_2_NAME
            3 -> KEY_CUSTOM_OUTFIT_3_NAME
            4 -> KEY_CUSTOM_OUTFIT_4_NAME
            else -> KEY_CUSTOM_OUTFIT_5_NAME
        }
        dataStore.edit { it[key] = name }
    }
}
