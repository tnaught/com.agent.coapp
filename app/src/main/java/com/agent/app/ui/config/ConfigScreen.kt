package com.agent.app.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agent.app.data.AgentConfig
import com.agent.app.viewmodel.ConfigViewModel
import com.agent.app.viewmodel.SyncResult

/**
 * 配置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = viewModel()
) {
    val config by viewModel.configFlow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    
    var editedConfig by remember(config) { mutableStateOf(config) }
    var hasChanges by remember { mutableStateOf(false) }
    
    // 监听同步结果
    LaunchedEffect(syncResult) {
        syncResult?.let {
            // 自动清除
            kotlinx.coroutines.delay(3000)
            viewModel.clearSyncResult()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent配置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // LLM配置
            ConfigSection(title = "LLM 配置") {
                ConfigTextField(
                    value = editedConfig.llmApiKey,
                    onValueChange = {
                        editedConfig = editedConfig.copy(llmApiKey = it)
                        hasChanges = true
                    },
                    label = "API Key",
                    isPassword = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigTextField(
                    value = editedConfig.llmBaseUrl,
                    onValueChange = {
                        editedConfig = editedConfig.copy(llmBaseUrl = it)
                        hasChanges = true
                    },
                    label = "Base URL",
                    placeholder = "https://api.openai.com/v1"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigTextField(
                    value = editedConfig.llmModel,
                    onValueChange = {
                        editedConfig = editedConfig.copy(llmModel = it)
                        hasChanges = true
                    },
                    label = "模型名称",
                    placeholder = "gpt-4"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // API Keys
            ConfigSection(title = "API Keys") {
                ConfigTextField(
                    value = editedConfig.searchApiKey,
                    onValueChange = {
                        editedConfig = editedConfig.copy(searchApiKey = it)
                        hasChanges = true
                    },
                    label = "搜索引擎 API Key"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigTextField(
                    value = editedConfig.asrApiKey,
                    onValueChange = {
                        editedConfig = editedConfig.copy(asrApiKey = it)
                        hasChanges = true
                    },
                    label = "ASR API Key"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigTextField(
                    value = editedConfig.ttsApiKey,
                    onValueChange = {
                        editedConfig = editedConfig.copy(ttsApiKey = it)
                        hasChanges = true
                    },
                    label = "TTS API Key"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 设备连接
            ConfigSection(title = "设备连接") {
                ConfigTextField(
                    value = editedConfig.deviceIp,
                    onValueChange = {
                        editedConfig = editedConfig.copy(deviceIp = it)
                        hasChanges = true
                    },
                    label = "设备IP地址",
                    placeholder = "192.168.1.100"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigTextField(
                    value = editedConfig.devicePort.toString(),
                    onValueChange = {
                        editedConfig = editedConfig.copy(devicePort = it.toIntOrNull() ?: 8080)
                        hasChanges = true
                    },
                    label = "端口",
                    placeholder = "8080"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 同步结果提示
            syncResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            is SyncResult.Success -> MaterialTheme.colorScheme.primaryContainer
                            is SyncResult.Error -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Text(
                        text = when (result) {
                            is SyncResult.Success -> result.message
                            is SyncResult.Error -> result.message
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.syncFromDevice() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("从设备拉取")
                }
                Button(
                    onClick = { viewModel.syncToDevice() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("同步到设备")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 保存按钮
            Button(
                onClick = { viewModel.saveConfig(editedConfig) },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasChanges && !isLoading
            ) {
                Text("保存配置到本地")
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * 配置分组
 */
@Composable
private fun ConfigSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 配置文本输入框
 */
@Composable
private fun ConfigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder) }} else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else 
            VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) 
            else KeyboardOptions.Default
    )
}
