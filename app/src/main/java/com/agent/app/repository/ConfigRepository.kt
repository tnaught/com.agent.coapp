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
        private val KEY_TAVILY_KEY = stringPreferencesKey("tavily_key")
        private val KEY_VOLC_KEY = stringPreferencesKey("volc_key")
        private val KEY_VOLC_ASR_APP_ID = stringPreferencesKey("volc_asr_app_id")
        private val KEY_VOLC_ASR_TOKEN = stringPreferencesKey("volc_asr_token")
        private val KEY_VOLC_ASR_CLUSTER = stringPreferencesKey("volc_asr_cluster")
        private val KEY_DEVICE_IP = stringPreferencesKey("device_ip")
        private val KEY_DEVICE_PORT = intPreferencesKey("device_port")
    }
    
    /**
     * 获取配置Flow
     */
    val configFlow: Flow<AgentConfig> = context.dataStore.data.map { preferences ->
        AgentConfig(
            llmBaseUrl = preferences[KEY_LLM_BASE_URL] ?: "https://api.xiaomimimo.com/v1",
            llmModel = preferences[KEY_LLM_MODEL] ?: "mimo-v2.5",
            llmApiKey = preferences[KEY_LLM_API_KEY] ?: "",
            tavilyKey = preferences[KEY_TAVILY_KEY] ?: "",
            volcKey = preferences[KEY_VOLC_KEY] ?: "",
            volcAsrAppId = preferences[KEY_VOLC_ASR_APP_ID] ?: "",
            volcAsrToken = preferences[KEY_VOLC_ASR_TOKEN] ?: "",
            volcAsrCluster = preferences[KEY_VOLC_ASR_CLUSTER] ?: "volcengine_streaming_common",
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
            preferences[KEY_TAVILY_KEY] = config.tavilyKey
            preferences[KEY_VOLC_KEY] = config.volcKey
            preferences[KEY_VOLC_ASR_APP_ID] = config.volcAsrAppId
            preferences[KEY_VOLC_ASR_TOKEN] = config.volcAsrToken
            preferences[KEY_VOLC_ASR_CLUSTER] = config.volcAsrCluster
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
