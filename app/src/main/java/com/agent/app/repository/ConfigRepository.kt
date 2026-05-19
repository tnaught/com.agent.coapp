package com.agent.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.agent.app.data.AgentConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agent_config")

/**
 * 配置数据仓库
 * 使用DataStore持久化配置
 */
class ConfigRepository(private val context: Context) {
    
    companion object {
        private val KEY_LLM_API_KEY = stringPreferencesKey("llm_api_key")
        private val KEY_LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        private val KEY_LLM_MODEL = stringPreferencesKey("llm_model")
        private val KEY_SEARCH_API_KEY = stringPreferencesKey("search_api_key")
        private val KEY_ASR_API_KEY = stringPreferencesKey("asr_api_key")
        private val KEY_TTS_API_KEY = stringPreferencesKey("tts_api_key")
        private val KEY_DEVICE_IP = stringPreferencesKey("device_ip")
        private val KEY_DEVICE_PORT = intPreferencesKey("device_port")
    }
    
    /**
     * 获取配置Flow
     */
    val configFlow: Flow<AgentConfig> = context.dataStore.data.map { preferences ->
        AgentConfig(
            llmApiKey = preferences[KEY_LLM_API_KEY] ?: "",
            llmBaseUrl = preferences[KEY_LLM_BASE_URL] ?: "https://api.openai.com/v1",
            llmModel = preferences[KEY_LLM_MODEL] ?: "gpt-4",
            searchApiKey = preferences[KEY_SEARCH_API_KEY] ?: "",
            asrApiKey = preferences[KEY_ASR_API_KEY] ?: "",
            ttsApiKey = preferences[KEY_TTS_API_KEY] ?: "",
            deviceIp = preferences[KEY_DEVICE_IP] ?: "",
            devicePort = preferences[KEY_DEVICE_PORT] ?: 8080
        )
    }
    
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: AgentConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LLM_API_KEY] = config.llmApiKey
            preferences[KEY_LLM_BASE_URL] = config.llmBaseUrl
            preferences[KEY_LLM_MODEL] = config.llmModel
            preferences[KEY_SEARCH_API_KEY] = config.searchApiKey
            preferences[KEY_ASR_API_KEY] = config.asrApiKey
            preferences[KEY_TTS_API_KEY] = config.ttsApiKey
            preferences[KEY_DEVICE_IP] = config.deviceIp
            preferences[KEY_DEVICE_PORT] = config.devicePort
        }
    }
    
    /**
     * 更新设备IP
     */
    suspend fun updateDeviceIp(ip: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_IP] = ip
        }
    }
    
    /**
     * 更新设备端口
     */
    suspend fun updateDevicePort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_PORT] = port
        }
    }
}
