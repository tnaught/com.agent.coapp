package com.agent.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agent.app.data.AgentConfig
import com.agent.app.data.Skill
import com.agent.app.repository.ConfigRepository
import com.agent.app.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 技能ViewModel
 */
class SkillsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configRepository = ConfigRepository(application)
    private val deviceRepository = DeviceRepository()
    
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    
    // 推送技能对话框状态
    private val _showPushDialog = MutableStateFlow(false)
    val showPushDialog: StateFlow<Boolean> = _showPushDialog
    
    /**
     * 获取技能列表
     */
    fun loadSkills() {
        viewModelScope.launch {
            _isLoading.value = true
            val config = configRepository.configFlow.first()
            
            if (config.deviceIp.isEmpty()) {
                _message.value = "请先配置设备IP地址"
                _isLoading.value = false
                return@launch
            }
            
            val result = deviceRepository.getSkills(config.deviceIp, config.devicePort)
            result.onSuccess { skillsList ->
                _skills.value = skillsList
            }.onFailure { error ->
                _message.value = "获取技能列表失败: ${error.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 推送技能
     */
    fun pushSkill(name: String, description: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val config = configRepository.configFlow.first()
            
            val result = deviceRepository.pushSkill(
                config.deviceIp,
                config.devicePort,
                name,
                description,
                code
            )
            
            result.onSuccess {
                _message.value = "技能推送成功"
                loadSkills() // 刷新列表
            }.onFailure { error ->
                _message.value = "推送失败: ${error.message}"
            }
            
            _isLoading.value = false
            _showPushDialog.value = false
        }
    }
    
    /**
     * 删除技能
     */
    fun deleteSkill(skillName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val config = configRepository.configFlow.first()
            
            val result = deviceRepository.deleteSkill(config.deviceIp, config.devicePort, skillName)
            result.onSuccess {
                _message.value = "技能已删除"
                loadSkills() // 刷新列表
            }.onFailure { error ->
                _message.value = "删除失败: ${error.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 显示推送对话框
     */
    fun showPushDialog() {
        _showPushDialog.value = true
    }
    
    /**
     * 隐藏推送对话框
     */
    fun hidePushDialog() {
        _showPushDialog.value = false
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = ""
    }
}
