# 开发流程

## 环境准备

### 必需工具
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- 一台 Android 手机（minSdk 26，即 Android 8.0+）

### 首次设置
```bash
# 1. 克隆仓库
git clone git@github.com:tnaught/com.vela.openapp.git
cd com.vela.openapp

# 2. 用 Android Studio 打开项目（自动下载 Gradle + 依赖）
#    或命令行生成 Gradle Wrapper：
gradle wrapper --gradle-version 8.4

# 3. 配置 local.properties（Android Studio 自动生成，或手动）
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
```

---

## 开发流程

### 分支策略
```
main          ← 稳定版本，保护分支
├── dev       ← 日常开发集成分支
├── feature/* ← 功能开发（从 dev 拉出）
├── bugfix/*  ← Bug 修复
└── release/* ← 发版准备
```

日常开发：
```bash
git checkout dev
git pull origin dev
git checkout -b feature/xxx
# ... 开发 ...
git push -u origin feature/xxx
# 提 PR 合入 dev
```

---

## 编译

### Debug 构建
```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建
```bash
./gradlew assembleRelease
# 需配置签名（app/build.gradle.kts 中 signingConfigs）
```

### 清理重建
```bash
./gradlew clean assembleDebug
```

### 常用 Gradle 命令
| 命令 | 用途 |
|------|------|
| `./gradlew assembleDebug` | 编译 Debug APK |
| `./gradlew installDebug` | 编译并安装到连接的设备 |
| `./gradlew clean` | 清理构建产物 |
| `./gradlew dependencies` | 查看依赖树 |
| `./gradlew lint` | 静态代码检查 |
| `./gradlew test` | 运行单元测试 |
| `./gradlew connectedAndroidTest` | 运行设备测试 |

---

## 调试

### 安装到设备
```bash
# USB 连接手机，开启开发者选项 + USB 调试
adb devices                          # 确认设备连接
./gradlew installDebug               # 编译并安装
adb shell am start -n com.agent.coapp/.MainActivity  # 启动 App
```

### Logcat 日志
```bash
# 查看 App 全部日志
adb logcat --pid=$(adb shell pidof com.agent.coapp)

# 按 Tag 过滤
adb logcat -s BleManager:V WebSocketManager:V DeviceApiService:V

# BLE 相关
adb logcat -s BleManager:V BluetoothGatt:V
```

### Android Studio 调试
1. 打开项目 → Run → Debug 'app'
2. 设置断点（BleManager / WebSocketManager 等关键类）
3. 使用 Layout Inspector 检查 Compose UI

### 常见调试场景

**BLE 扫描无结果：**
```bash
# 确认蓝牙和位置权限
adb shell dumpsys package com.agent.coapp | grep permission
# 确认位置服务开启
adb shell settings get secure location_mode
# 查看系统 BLE 日志
adb logcat -s BtGatt.GattService:V bt_btif:V
```

**WebSocket 连接失败：**
```bash
# 确认设备 IP 可达
adb shell ping -c 3 <device_ip>
# 确认端口开放
adb shell nc -zv <device_ip> 8080
```

---

## 验证

### 功能验证清单

| # | 功能 | 验证步骤 | 预期结果 |
|---|------|----------|----------|
| 1 | BLE 扫描 | 配网页 → 点击扫描 | 列出附近 Agent-* 设备 |
| 2 | BLE 配网 | 选择设备 → 输入 WiFi → 配网 | 状态显示"配网成功" |
| 3 | 配置同步 | 配置页 → 输入 IP → 从设备同步 | 显示设备当前配置 |
| 4 | 配置推送 | 修改配置 → 同步到设备 | 提示"同步成功" |
| 5 | 技能列表 | 技能页 → 加载 | 显示设备已安装技能 |
| 6 | 技能推送 | 点击推送 → 填写信息 → 确认 | 提示"推送成功"，列表刷新 |
| 7 | WebSocket 对话 | 对话页 → 连接 → 发消息 | 收到设备回复 |
| 8 | 日志查看 | 日志页 → 加载 | 显示设备日志，支持过滤 |

### 自动化测试
```bash
# 单元测试
./gradlew test

# UI 测试（需连接设备）
./gradlew connectedAndroidTest
```

### Lint 检查
```bash
./gradlew lint
# 报告：app/build/reports/lint-results-debug.html
```

---

## 发版流程

1. 从 `dev` 创建 `release/x.y.z` 分支
2. 更新 `versionCode` / `versionName`（app/build.gradle.kts）
3. 执行完整验证清单
4. 签名打包：`./gradlew assembleRelease`
5. 合入 `main`，打 tag：`git tag v1.0.0`
6. 推送：`git push origin main --tags`

---

## 项目约定

- **代码风格**：Kotlin 官方风格（ktlint），Compose 函数大驼峰
- **提交规范**：`type: description`（feat/fix/refactor/docs/chore）
- **包结构**：按功能分层（data/network/repository/viewmodel/ui）
- **状态管理**：StateFlow + collectAsState，禁止 LiveData
