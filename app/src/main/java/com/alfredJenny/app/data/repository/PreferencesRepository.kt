package com.alfredJenny.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfredJenny.app.data.model.AIProvider
import com.alfredJenny.app.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_MODEL = stringPreferencesKey("selected_model")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            aiProvider = prefs[KEY_AI_PROVIDER]?.let {
                runCatching { AIProvider.valueOf(it) }.getOrDefault(AIProvider.OPENAI)
            } ?: AIProvider.OPENAI,
            apiKey = prefs[KEY_API_KEY] ?: "",
            selectedModel = prefs[KEY_MODEL] ?: ""
        )
    }

    suspend fun saveApiKey(key: String) {
        dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun saveAiProvider(provider: AIProvider) {
        dataStore.edit { it[KEY_AI_PROVIDER] = provider.name }
    }

    suspend fun saveModel(model: String) {
        dataStore.edit { it[KEY_MODEL] = model }
    }
}
