package com.mutantcat.dailydiet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mutantcat.dailydiet.data.security.SecretCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AiSettings(
    val enabled: Boolean = false,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "",
) {
    val isConfigured: Boolean
        get() = enabled && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

class AiSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val secretCipher: SecretCipher = SecretCipher(),
) {
    val settingsFlow: Flow<AiSettings> = dataStore.data.map { preferences ->
        AiSettings(
            enabled = preferences[Keys.ENABLED] ?: false,
            baseUrl = preferences[Keys.BASE_URL] ?: "https://api.openai.com/v1",
            apiKey = secretCipher.decrypt(preferences[Keys.API_KEY].orEmpty()),
            model = preferences[Keys.MODEL].orEmpty(),
        )
    }

    suspend fun current(): AiSettings = settingsFlow.first()

    suspend fun save(settings: AiSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = settings.enabled
            preferences[Keys.BASE_URL] = settings.baseUrl.trim()
            // API Key 用 Keystore 加密后再落盘；加密失败（例如模拟器缺 Keystore）时退回明文，
            // 但仍能正常工作，不会因为加密不可用而丢失用户配置。
            preferences[Keys.API_KEY] = secretCipher.encrypt(settings.apiKey.trim())
                ?: settings.apiKey.trim()
            preferences[Keys.MODEL] = settings.model.trim()
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("ai_enabled")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
    }
}

