package com.agent.coapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agent.coapp.data.AgentConfig
import com.agent.coapp.repository.ConfigRepository
import com.agent.coapp.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 配置ViewModel
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configRepository = ConfigRepository(application)
    private val deviceRepository = DeviceRepository()
    
    val configFlow: StateFlow<AgentConfig> = MutableStateFlow(AgentConfig())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    
    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult
    
    init {
        viewModelScope.launch {
            configRepository.configFlow.collect { config ->
                (configFlow as MutableStateFlow).value = config
            }
        }
    }
    
    /**
     * 保存配置到本地
     */
    fun saveConfig(config: AgentConfig) {
        viewModelScope.launch {
            configRepository.saveConfig(config)
        }
    }
    
    /**
     * 从设备同步配置
     */
    fun syncFromDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            val config = configRepository.configFlow.first()
            
            if (config.deviceIp.isEmpty()) {
                _message.value = "请先配置设备IP地址"
                _isLoading.value = false
                return@launch
            }
            
            val result = deviceRepository.getConfig(config.deviceIp, config.devicePort)
            result.onSuccess { response ->
                val updatedConfig = config.copy(
                    llmApiKey = response.llm_api_key ?: config.llmApiKey,
                    llmBaseUrl = response.llm_base_url ?: config.llmBaseUrl,
                    llmModel = response.llm_model ?: config.llmModel,
                    tavilyKey = response.search_api_key ?: config.tavilyKey,
                    volcKey = response.asr_api_key ?: config.volcKey,
                    volcAsrToken = response.tts_api_key ?: config.volcAsrToken
                )
                configRepository.saveConfig(updatedConfig)
                _syncResult.value = SyncResult.Success("配置已从设备同步")
            }.onFailure { error ->
                _syncResult.value = SyncResult.Error("同步失败: ${error.message}")
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 同步配置到设备
     */
    fun syncToDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            val config = configRepository.configFlow.first()
            
            if (config.deviceIp.isEmpty()) {
                _message.value = "请先配置设备IP地址"
                _isLoading.value = false
                return@launch
            }
            
            val result = deviceRepository.updateConfig(
                deviceIp = config.deviceIp,
                port = config.devicePort,
                llmApiKey = config.llmApiKey,
                llmBaseUrl = config.llmBaseUrl,
                llmModel = config.llmModel,
                searchApiKey = config.tavilyKey,
                asrApiKey = config.volcKey,
                ttsApiKey = config.volcAsrToken
            )
            
            result.onSuccess {
                _syncResult.value = SyncResult.Success("配置已同步到设备")
            }.onFailure { error ->
                _syncResult.value = SyncResult.Error("同步失败: ${error.message}")
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 清除同步结果
     */
    fun clearSyncResult() {
        _syncResult.value = null
    }
}

/**
 * 同步结果
 */
sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
