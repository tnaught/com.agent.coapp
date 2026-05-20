package com.agent.app.data

/**
 * Agent配置数据类
 */
data class AgentConfig(
    // LLM 配置
    val llmBaseUrl: String = "https://api.xiaomimimo.com/v1",
    val llmModel: String = "mimo-v2.5",
    val llmApiKey: String = "",
    // Tavily 搜索
    val tavilyKey: String = "",
    // 火山引擎 ASR
    val volcKey: String = "",
    val volcAsrAppId: String = "",
    val volcAsrToken: String = "",
    val volcAsrCluster: String = "volcengine_streaming_common",
    // 设备连接
    val deviceIp: String = "",
    val devicePort: Int = 8080
)

/**
 * 配置请求体
 */
data class ConfigRequest(
    val llm_api_key: String,
    val llm_base_url: String,
    val llm_model: String,
    val search_api_key: String,
    val asr_api_key: String,
    val tts_api_key: String
)

/**
 * 配置响应体
 */
data class ConfigResponse(
    val llm_api_key: String?,
    val llm_base_url: String?,
    val llm_model: String?,
    val search_api_key: String?,
    val asr_api_key: String?,
    val tts_api_key: String?
)
