package com.agent.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agent.app.data.ChatMessage
import com.agent.app.network.WebSocketState
import com.agent.app.repository.ConfigRepository
import com.agent.app.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 聊天ViewModel
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configRepository = ConfigRepository(application)
    private val deviceRepository = DeviceRepository()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _connectionState = MutableStateFlow(WebSocketState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketState> = _connectionState
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    
    init {
        // 监听WebSocket消息
        viewModelScope.launch {
            deviceRepository.getWebSocketMessages().collect { wsMessages ->
                // 处理收到的WebSocket消息
                wsMessages.forEach { wsResponse ->
                    if (wsResponse.type == "message") {
                        val currentMessages = _messages.value.toMutableList()
                        // 避免重复添加
                        if (currentMessages.none { 
                            it.content == wsResponse.content && !it.isFromUser 
                        }) {
                            currentMessages.add(
                                ChatMessage(
                                    content = wsResponse.content,
                                    isFromUser = false
                                )
                            )
                            _messages.value = currentMessages
                        }
                    }
                }
            }
        }
        
        // 监听连接状态
        viewModelScope.launch {
            deviceRepository.getWebSocketState().collect { state ->
                _connectionState.value = state
            }
        }
        
        // 监听状态消息
        viewModelScope.launch {
            deviceRepository.getWebSocketStatus().collect { status ->
                _statusMessage.value = status
            }
        }
    }
    
    /**
     * 连接设备
     */
    fun connect() {
        viewModelScope.launch {
            val config = configRepository.configFlow.first()
            
            if (config.deviceIp.isEmpty()) {
                _message.value = "请先配置设备IP地址"
                return@launch
            }
            
            deviceRepository.connectWebSocket(config.deviceIp, config.devicePort)
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        deviceRepository.disconnectWebSocket()
    }
    
    /**
     * 发送消息
     */
    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isEmpty()) return
        
        // 添加用户消息
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage(content = content, isFromUser = true))
        _messages.value = currentMessages
        
        // 清空输入框
        _inputText.value = ""
        
        // 发送消息
        deviceRepository.sendMessage(content)
    }
    
    /**
     * 更新输入文本
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    /**
     * 清空消息
     */
    fun clearMessages() {
        _messages.value = emptyList()
        deviceRepository.clearWebSocketMessages()
    }
    
    /**
     * 清除消息提示
     */
    fun clearMessage() {
        _message.value = ""
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
