package com.agent.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.agent.app.data.BleDevice
import com.agent.app.data.ProvisioningStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * BLE管理器
 * 处理蓝牙扫描和设备连接
 */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BleManager"
        // Nordic UART Service (NUS) UUIDs - matches watch ble_gatt.c
        val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val CHAR_RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")  // Write to watch
        val CHAR_TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")  // Notify from watch
        
        // Legacy aliases for compatibility
        val CHAR_SSID: UUID = CHAR_RX
        val CHAR_PASSWORD: UUID = CHAR_RX
        val CHAR_STATUS: UUID = CHAR_TX
        
        // 扫描过滤器前缀
        const val DEVICE_PREFIX = "Agent-"
        // 目标设备MAC地址
        const val TARGET_DEVICE_ADDRESS = "D0:C1:BF:B0:DF:F4"
    }
    
    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _scanResults = MutableStateFlow<List<BleDevice>>(emptyList())
    val scanResults: StateFlow<List<BleDevice>> = _scanResults
    
    private val _provisioningStatus = MutableStateFlow(ProvisioningStatus.IDLE)
    val provisioningStatus: StateFlow<ProvisioningStatus> = _provisioningStatus
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage
    
    private var isScanning = false
    private val devices = mutableMapOf<String, BleDevice>()
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    /**
     * 检查蓝牙是否开启
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 开始扫描BLE设备
     */
    fun startScan() {
        if (isScanning) return
        
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "蓝牙未开启，无法扫描")
            _statusMessage.value = "请先开启蓝牙"
            return
        }
        
        devices.clear()
        _scanResults.value = emptyList()
        _provisioningStatus.value = ProvisioningStatus.SCANNING
        _statusMessage.value = "正在扫描..."
        
        try {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "无法获取 BLE Scanner")
                _statusMessage.value = "BLE Scanner 不可用"
                _provisioningStatus.value = ProvisioningStatus.IDLE
                return
            }
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
            isScanning = true
            Log.d(TAG, "BLE 扫描已启动")
            
            // 10秒后自动停止扫描
            mainHandler.postDelayed({
                stopScan()
                if (devices.isEmpty()) {
                    _statusMessage.value = "未发现设备，请确认设备已开启并在附近"
                } else {
                    _statusMessage.value = "扫描完成，发现 ${devices.size} 个设备"
                }
            }, 10000)
        } catch (e: Exception) {
            Log.e(TAG, "扫描失败: ${e.message}", e)
            _statusMessage.value = "扫描失败: ${e.message}"
            _provisioningStatus.value = ProvisioningStatus.IDLE
        }
    }
    
    /**
     * 停止扫描
     */
    fun stopScan() {
        if (!isScanning) return
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "停止扫描失败: ${e.message}")
        }
        isScanning = false
        
        if (_provisioningStatus.value == ProvisioningStatus.SCANNING) {
            _provisioningStatus.value = ProvisioningStatus.IDLE
        }
    }
    
    /**
     * 连接到设备并进行配网
     */
    fun connectAndProvision(ssid: String, password: String) {
        // 优先连接目标设备，其次连接扫描列表中的第一个
        val device = devices[TARGET_DEVICE_ADDRESS] ?: devices.values.firstOrNull()
        if (device == null) {
            _statusMessage.value = "没有找到设备"
            _provisioningStatus.value = ProvisioningStatus.FAILED
            return
        }
        
        _provisioningStatus.value = ProvisioningStatus.CONNECTING
        _statusMessage.value = "正在连接 ${device.name}..."
        
        try {
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}")
            _statusMessage.value = "连接失败: ${e.message}"
            _provisioningStatus.value = ProvisioningStatus.FAILED
        }
    }
    
    /**
     * 发送WiFi凭据 - 通过NUS RX Characteristic写入JSON
     */
    private fun sendWifiCredentials(ssid: String, password: String) {
        _provisioningStatus.value = ProvisioningStatus.SENDING_WIFI
        _statusMessage.value = "正在发送WiFi凭据..."
        
        try {
            val service = bluetoothGatt?.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "未找到NUS服务: $SERVICE_UUID")
                _statusMessage.value = "未找到BLE数据服务"
                _provisioningStatus.value = ProvisioningStatus.FAILED
                return
            }
            
            val rxChar = service.getCharacteristic(CHAR_RX)
            if (rxChar == null) {
                Log.e(TAG, "未找到RX特征: $CHAR_RX")
                _statusMessage.value = "未找到写入特征"
                _provisioningStatus.value = ProvisioningStatus.FAILED
                return
            }
            
            // 发送WiFi配置JSON
            val json = """{"cmd":"wifi_config","ssid":"$ssid","password":"$password"}"""
            rxChar.value = json.toByteArray(Charsets.UTF_8)
            val success = bluetoothGatt?.writeCharacteristic(rxChar) ?: false
            Log.d(TAG, "写入WiFi凭据: success=$success, len=${json.length}")
            
            if (success) {
                _statusMessage.value = "WiFi凭据已发送，等待设备响应..."
            } else {
                _statusMessage.value = "写入失败"
                _provisioningStatus.value = ProvisioningStatus.FAILED
            }
        } catch (e: Exception) {
            Log.e(TAG, "发送WiFi凭据失败: ${e.message}")
            _statusMessage.value = "发送失败: ${e.message}"
            _provisioningStatus.value = ProvisioningStatus.FAILED
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e(TAG, "断开连接失败: ${e.message}")
        }
    }
    
    /**
     * 扫描回调
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name
            val address = result.device.address
            
            Log.d(TAG, "发现设备: name=$deviceName, address=$address, rssi=${result.rssi}")
            
            // 只显示目标设备
            if (!address.equals(TARGET_DEVICE_ADDRESS, ignoreCase = true)) return
            
            val displayName = if (deviceName.isNullOrBlank()) "XiaomiWatch S5" else deviceName
            
            val bleDevice = BleDevice(
                name = displayName,
                address = address,
                rssi = result.rssi
            )
            devices[address] = bleDevice
            _scanResults.value = devices.values.toList().sortedByDescending { it.rssi }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "扫描失败, errorCode=$errorCode")
            _statusMessage.value = "扫描失败 (错误码: $errorCode)"
            _provisioningStatus.value = ProvisioningStatus.IDLE
        }
    }
    
    /**
     * GATT回调
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    mainHandler.post {
                        _provisioningStatus.value = ProvisioningStatus.CONNECTED
                        _statusMessage.value = "已连接，正在发现服务..."
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mainHandler.post {
                        _statusMessage.value = "连接已断开 (status=$status)"
                        _provisioningStatus.value = ProvisioningStatus.IDLE
                    }
                    gatt.close()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services
                Log.d(TAG, "发现 ${services.size} 个服务:")
                services.forEach { service ->
                    Log.d(TAG, "  服务: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        Log.d(TAG, "    特征: ${char.uuid}")
                    }
                }
                
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.w(TAG, "未找到目标服务 $SERVICE_UUID")
                    mainHandler.post {
                        _statusMessage.value = "设备不支持配网服务"
                        _provisioningStatus.value = ProvisioningStatus.FAILED
                    }
                    return
                }
                
                val statusChar = service.getCharacteristic(CHAR_TX)
                if (statusChar != null) {
                    gatt.setCharacteristicNotification(statusChar, true)
                    // 写入CCCD descriptor启用通知
                    val descriptor = statusChar.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                    Log.d(TAG, "已启用 TX 通知")
                    mainHandler.post {
                        _statusMessage.value = "服务已就绪，可以发送数据"
                        _provisioningStatus.value = ProvisioningStatus.CONNECTED
                    }
                } else {
                    Log.w(TAG, "未找到 TX 特征")
                }
            } else {
                mainHandler.post {
                    _statusMessage.value = "服务发现失败 (status=$status)"
                    _provisioningStatus.value = ProvisioningStatus.FAILED
                }
            }
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "写入成功: ${characteristic.uuid}")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value
            if (value != null && value.isNotEmpty()) {
                val status = value[0].toInt()
                mainHandler.post {
                    when (status) {
                        0 -> {
                            _statusMessage.value = "配网进行中..."
                        }
                        1 -> {
                            _statusMessage.value = "配网成功！"
                            _provisioningStatus.value = ProvisioningStatus.SUCCESS
                            disconnect()
                        }
                        2 -> {
                            _statusMessage.value = "配网失败"
                            _provisioningStatus.value = ProvisioningStatus.FAILED
                            disconnect()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 设置配网回调（外部调用传入SSID和密码）
     */
    fun setWifiCredentials(ssid: String, password: String) {
        sendWifiCredentials(ssid, password)
    }
}
