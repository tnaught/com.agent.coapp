# AgentApp 开发笔记

> 供 AI Coding Agent 参考的需求与进度文档

## 项目概述

open-vela AI Agent 的 Android 配套 App，用于：
1. BLE 扫描发现并配网 Agent 设备（手表/开发板）
2. WiFi 连接后管理 Agent 配置（LLM API Key、模型等）
3. 技能推送与状态查看
4. 实时对话（WebSocket）
5. 查看设备日志

## 技术栈

- Kotlin + Jetpack Compose
- BLE（蓝牙低功耗）扫描 + GATT 通信
- OkHttp（REST API + WebSocket）
- MVVM 架构（ViewModel + StateFlow）
- 包名：`com.agent.coapp`

## 项目结构

```
app/src/main/java/com/agent/app/
├── AgentApplication.kt          # Application
├── MainActivity.kt              # 单Activity入口
├── ble/
│   └── BleManager.kt            # BLE扫描+连接+配网
├── data/
│   ├── ChatMessage.kt           # 聊天消息数据类
│   ├── ConfigData.kt            # Agent配置数据类
│   └── SkillData.kt             # 技能数据类
├── network/
│   ├── DeviceApiService.kt      # HTTP API（配置/技能/日志）
│   └── WebSocketManager.kt      # WebSocket对话
├── repository/
│   ├── ConfigRepository.kt      # 配置持久化（SharedPreferences）
│   └── DeviceRepository.kt      # 设备信息管理
├── ui/
│   ├── chat/ChatScreen.kt       # 对话界面
│   ├── config/ConfigScreen.kt   # 配置界面
│   ├── logs/LogsScreen.kt       # 日志界面
│   ├── navigation/Navigation.kt # 导航
│   ├── provisioning/BleProvisioningScreen.kt  # BLE配网界面
│   ├── skills/SkillsScreen.kt   # 技能管理界面
│   └── theme/                   # 主题
├── viewmodel/
│   ├── ChatViewModel.kt
│   ├── ConfigViewModel.kt
│   ├── LogsViewModel.kt
│   ├── ProvisioningViewModel.kt
│   └── SkillsViewModel.kt
```

## 当前进度

### ✅ 已完成
- 整体框架搭建（23个Kotlin源文件，~3000行）
- 编译通过，生成 debug APK（15MB）
- 5个页面UI（BLE配网/配置/技能/对话/日志）
- BLE扫描+连接+配网流程代码
- HTTP API 通信（配置读写/技能推送/日志获取）
- WebSocket 实时对话
- SharedPreferences 配置持久化
- **真机安装验证**：界面布局、功能基本符合需求

### 🐛 已知问题

#### P0：BLE扫描找不到设备
- **现象**：真机上点击扫描，无法发现 Agent 设备
- **可能原因**：
  1. BLE UUID 不匹配 — 当前硬编码 `0000fe55-...`，需确认设备端实际广播的 Service UUID
  2. 扫描过滤逻辑 — 当前按设备名前缀 `Agent-` 过滤，设备可能不以此前缀广播
  3. Android 12+ 权限 — 需要 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`、`ACCESS_FINE_LOCATION` 运行时权限
  4. 位置服务未开启 — Android 要求位置开关打开才能扫描BLE
  5. 扫描超时太短 — 当前5秒自动停止，可能不够
- **建议排查步骤**：
  1. 先去掉过滤条件，扫描所有BLE设备，看能否发现目标设备
  2. 确认设备端广播的 Service UUID 和设备名格式
  3. 加详细日志：扫描开始/结束/发现设备/权限状态
  4. 检查 AndroidManifest.xml 权限声明是否完整
  5. 延长扫描时间到 10-15 秒

### 🔄 待开发/优化

#### P1：BLE配网流程完善
- [ ] 修复扫描找不到设备问题（见上方P0）
- [ ] 增加运行时权限请求（Android 12+ BLUETOOTH_SCAN/CONNECT）
- [ ] 增加位置服务检测与引导开启
- [ ] GATT 连接稳定性优化（重连、超时处理）
- [ ] 配网结果反馈（成功/失败原因）

#### P1：对话功能完善
- [ ] WebSocket 断线重连
- [ ] 消息发送失败重试
- [ ] 长消息/代码块显示优化
- [ ] 打字动画效果

#### P2：体验优化
- [ ] 设备列表持久化（记住已配对设备）
- [ ] 配置项表单验证
- [ ] 技能安装/卸载状态实时更新
- [ ] 日志过滤和搜索
- [ ] 深色模式适配

## 设备端API约定

设备（Agent）通过WiFi提供HTTP API，配网后通过IP:Port访问：

```
GET  /api/config          # 获取配置
POST /api/config          # 更新配置
GET  /api/skills          # 获取技能列表
POST /api/skills/push     # 推送技能
GET  /api/logs            # 获取日志
WS   /api/chat            # WebSocket对话
```

配置项：`llm_api_key`, `llm_base_url`, `llm_model`, `search_api_key`, `asr_api_key`, `tts_api_key`

## BLE配网协议约定

- Service UUID: `0000fe55-0000-1000-8000-00805f9b34fb`
- SSID Characteristic: `0000fe56-...`
- Password Characteristic: `0000fe57-...`
- Status Characteristic: `0000fe58-...`
- 设备名前缀: `Agent-`

⚠️ 以上UUID和前缀是初始占位值，需根据设备端实际实现调整。

## 编译与构建

```bash
cd AndroidAgentApp
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

依赖：Android SDK 34+, Kotlin 1.9+, Compose BOM 2024.02.00+, OkHttp 4.12+
