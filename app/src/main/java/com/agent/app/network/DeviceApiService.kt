package com.agent.app.network

import com.agent.app.data.ConfigRequest
import com.agent.app.data.ConfigResponse
import com.agent.app.data.LogsResponse
import com.agent.app.data.Skill
import com.agent.app.data.SkillPushRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 设备API服务
 * 处理与Agent设备的HTTP通信
 */
class DeviceApiService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
    
    /**
     * 获取Agent配置
     */
    suspend fun getConfig(baseUrl: String): Result<ConfigResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/config")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                val config = gson.fromJson(body, ConfigResponse::class.java)
                Result.success(config)
            } else {
                Result.failure(IOException("获取配置失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新Agent配置
     */
    suspend fun updateConfig(baseUrl: String, config: ConfigRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(config)
            val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
            
            val request = Request.Builder()
                .url("$baseUrl/api/config")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("更新配置失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取技能列表
     */
    suspend fun getSkills(baseUrl: String): Result<List<Skill>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/skills")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                // 尝试解析为列表或包含skills的对象
                val skills: List<Skill> = try {
                    gson.fromJson(body, object : TypeToken<List<Skill>>() {}.type)
                } catch (e: Exception) {
                    try {
                        val responseObj = gson.fromJson(body, SkillsResponse::class.java)
                        responseObj.skills ?: emptyList()
                    } catch (e2: Exception) {
                        emptyList()
                    }
                }
                Result.success(skills)
            } else {
                Result.failure(IOException("获取技能列表失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 推送技能
     */
    suspend fun pushSkill(baseUrl: String, skill: SkillPushRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(skill)
            val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
            
            val request = Request.Builder()
                .url("$baseUrl/api/skills")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("推送技能失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除技能
     */
    suspend fun deleteSkill(baseUrl: String, skillName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/skills/$skillName")
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("删除技能失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取日志
     */
    suspend fun getLogs(baseUrl: String, lines: Int = 100): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/logs?lines=$lines")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                val logsResponse = gson.fromJson(body, LogsResponse::class.java)
                Result.success(logsResponse.logs ?: emptyList())
            } else {
                Result.failure(IOException("获取日志失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// 响应类
data class SkillsResponse(val skills: List<Skill>?)
