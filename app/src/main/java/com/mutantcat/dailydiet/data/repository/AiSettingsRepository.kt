package com.mutantcat.dailydiet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AiSettings(
    val enabled: Boolean = false,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "",
    val visionEnabled: Boolean = true,
) {
    val isConfigured: Boolean
        get() = enabled && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

class AiSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settingsFlow: Flow<AiSettings> = dataStore.data.map { preferences ->
        AiSettings(
            enabled = preferences[Keys.ENABLED] ?: false,
            baseUrl = preferences[Keys.BASE_URL] ?: "https://api.openai.com/v1",
            apiKey = preferences[Keys.API_KEY].orEmpty(),
            model = preferences[Keys.MODEL].orEmpty(),
            visionEnabled = preferences[Keys.VISION_ENABLED] ?: true,
        )
    }

    suspend fun current(): AiSettings = settingsFlow.first()

    suspend fun save(settings: AiSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = settings.enabled
            preferences[Keys.BASE_URL] = settings.baseUrl.trim()
            preferences[Keys.API_KEY] = settings.apiKey.trim()
            preferences[Keys.MODEL] = settings.model.trim()
            preferences[Keys.VISION_ENABLED] = settings.visionEnabled
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("ai_enabled")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
        val VISION_ENABLED = booleanPreferencesKey("ai_vision_enabled")
    }
}

