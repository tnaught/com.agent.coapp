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
        // BLE服务UUID
        val SERVICE_UUID: UUID = UUID.fromString("0000fe55-0000-1000-8000-00805f9b34fb")
        val CHAR_SSID: UUID = UUID.fromString("0000fe56-0000-1000-8000-00805f9b34fb")
        val CHAR_PASSWORD: UUID = UUID.fromString("0000fe57-0000-1000-8000-00805f9b34fb")
        val CHAR_STATUS: UUID = UUID.fromString("0000fe58-0000-1000-8000-00805f9b34fb")
        
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
     * 发送WiFi凭据
     */
    private fun sendWifiCredentials(ssid: String, password: String) {
        _provisioningStatus.value = ProvisioningStatus.SENDING_WIFI
        _statusMessage.value = "正在发送WiFi凭据..."
        
        try {
            val service = bluetoothGatt?.getService(SERVICE_UUID)
            val ssidChar = service?.getCharacteristic(CHAR_SSID)
            val passwordChar = service?.getCharacteristic(CHAR_PASSWORD)
            
            if (ssidChar != null && passwordChar != null) {
                // 写入SSID
                ssidChar.value = ssid.toByteArray(Charsets.UTF_8)
                bluetoothGatt?.writeCharacteristic(ssidChar)
                
                // 延迟后写入密码
                mainHandler.postDelayed({
                    passwordChar.value = password.toByteArray(Charsets.UTF_8)
                    bluetoothGatt?.writeCharacteristic(passwordChar)
                }, 500)
            } else {
                _statusMessage.value = "未找到WiFi服务特征"
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
            
            // 显示所有设备（含无名设备），方便排查
            val displayName = if (deviceName.isNullOrBlank()) "Unknown" else deviceName
            
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
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    mainHandler.post {
                        _provisioningStatus.value = ProvisioningStatus.CONNECTED
                        _statusMessage.value = "已连接，正在发现服务..."
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mainHandler.post {
                        _statusMessage.value = "连接已断开"
                        _provisioningStatus.value = ProvisioningStatus.IDLE
                    }
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 服务发现成功，启用Status通知
                val service = gatt.getService(SERVICE_UUID)
                val statusChar = service?.getCharacteristic(CHAR_STATUS)
                if (statusChar != null) {
                    gatt.setCharacteristicNotification(statusChar, true)
                    // 通知已启用，可以发送WiFi凭据了
                }
            } else {
                mainHandler.post {
                    _statusMessage.value = "服务发现失败"
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
