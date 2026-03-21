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
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            aiProvider = prefs[KEY_AI_PROVIDER]?.let {
                runCatching { AIProvider.valueOf(it) }.getOrDefault(AIProvider.OPENAI)
            } ?: AIProvider.OPENAI,
            apiKey              = prefs[KEY_API_KEY]           ?: "",
            selectedModel       = prefs[KEY_MODEL]             ?: "",
            baseUrl             = prefs[KEY_BASE_URL]          ?: "",
            jwtToken            = prefs[KEY_JWT_TOKEN]         ?: "",
            userRole            = prefs[KEY_USER_ROLE]         ?: "",
            username            = prefs[KEY_USERNAME]          ?: "",
            elevenLabsApiKey    = prefs[KEY_ELEVENLABS_KEY]    ?: "",
            voiceId             = prefs[KEY_VOICE_ID]          ?: "pNInz6obpgDQGcFmaJgB",
            voiceEnabled        = prefs[KEY_VOICE_ENABLED]     ?: false,
            jennyEnabled        = prefs[KEY_JENNY_ENABLED]     ?: false,
            jennyVoiceId        = prefs[KEY_JENNY_VOICE_ID]    ?: "EXAVITQu4vr4xnSDxMaL",
            jennyPersonalityLevel = prefs[KEY_JENNY_PERSONALITY] ?: 3
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
}
