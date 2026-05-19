package com.agent.app.repository

import com.agent.app.data.*
import com.agent.app.network.DeviceApiService
import com.agent.app.network.WebSocketManager

/**
 * 设备交互仓库
 * 整合HTTP和WebSocket通信
 */
class DeviceRepository(
    private val apiService: DeviceApiService = DeviceApiService(),
    private val webSocketManager: WebSocketManager = WebSocketManager()
) {
    
    // 获取基础URL
    private fun getBaseUrl(deviceIp: String, port: Int): String {
        return "http://$deviceIp:$port"
    }
    
    /**
     * 获取Agent配置
     */
    suspend fun getConfig(deviceIp: String, port: Int): Result<ConfigResponse> {
        val baseUrl = getBaseUrl(deviceIp, port)
        return apiService.getConfig(baseUrl)
    }
    
    /**
     * 更新Agent配置
     */
    suspend fun updateConfig(
        deviceIp: String,
        port: Int,
        llmApiKey: String,
        llmBaseUrl: String,
        llmModel: String,
        searchApiKey: String,
        asrApiKey: String,
        ttsApiKey: String
    ): Result<Unit> {
        val baseUrl = getBaseUrl(deviceIp, port)
        val request = ConfigRequest(
            llm_api_key = llmApiKey,
            llm_base_url = llmBaseUrl,
            llm_model = llmModel,
            search_api_key = searchApiKey,
            asr_api_key = asrApiKey,
            tts_api_key = ttsApiKey
        )
        return apiService.updateConfig(baseUrl, request)
    }
    
    /**
     * 获取技能列表
     */
    suspend fun getSkills(deviceIp: String, port: Int): Result<List<Skill>> {
        val baseUrl = getBaseUrl(deviceIp, port)
        return apiService.getSkills(baseUrl)
    }
    
    /**
     * 推送技能
     */
    suspend fun pushSkill(
        deviceIp: String,
        port: Int,
        name: String,
        description: String,
        code: String
    ): Result<Unit> {
        val baseUrl = getBaseUrl(deviceIp, port)
        val request = SkillPushRequest(name, description, code)
        return apiService.pushSkill(baseUrl, request)
    }
    
    /**
     * 删除技能
     */
    suspend fun deleteSkill(deviceIp: String, port: Int, skillName: String): Result<Unit> {
        val baseUrl = getBaseUrl(deviceIp, port)
        return apiService.deleteSkill(baseUrl, skillName)
    }
    
    /**
     * 获取日志
     */
    suspend fun getLogs(deviceIp: String, port: Int, lines: Int = 100): Result<List<String>> {
        val baseUrl = getBaseUrl(deviceIp, port)
        return apiService.getLogs(baseUrl, lines)
    }
    
    /**
     * 连接WebSocket
     */
    fun connectWebSocket(deviceIp: String, port: Int) {
        webSocketManager.connect(deviceIp, port)
    }
    
    /**
     * 断开WebSocket
     */
    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }
    
    /**
     * 发送聊天消息
     */
    fun sendMessage(content: String) {
        webSocketManager.sendMessage(content)
    }
    
    /**
     * 获取WebSocket连接状态
     */
    fun getWebSocketState() = webSocketManager.connectionState
    
    /**
     * 获取WebSocket消息
     */
    fun getWebSocketMessages() = webSocketManager.messages
    
    /**
     * 获取WebSocket状态消息
     */
    fun getWebSocketStatus() = webSocketManager.statusMessage
    
    /**
     * 清空WebSocket消息
     */
    fun clearWebSocketMessages() {
        webSocketManager.clearMessages()
    }
}
