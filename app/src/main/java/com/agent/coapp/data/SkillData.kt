package com.agent.coapp.data

/**
 * 技能数据类
 */
data class Skill(
    val name: String,
    val description: String,
    val status: String = "unknown"
)

/**
 * 技能列表响应
 */
data class SkillsResponse(
    val skills: List<Skill>? = null
)

/**
 * 推送技能请求
 */
data class SkillPushRequest(
    val name: String,
    val description: String,
    val code: String
)
