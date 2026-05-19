package com.agent.app.data

/**
 * Agent配置数据类
 */
data class AgentConfig(
    val llmApiKey: String = "",
    val llmBaseUrl: String = "https://api.openai.com/v1",
    val llmModel: String = "gpt-4",
    val searchApiKey: String = "",
    val asrApiKey: String = "",
    val ttsApiKey: String = "",
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
