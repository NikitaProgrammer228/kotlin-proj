package com.accelerometer.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * BLE-сервер для телефона, который читает встроенный акселерометр
 * и передаёт данные по BLE на планшет.
 * 
 * Протокол данных:
 * - 12 байт: accX (float), accY (float), accZ (float) в m/s²
 * - Конвертируем в g перед отправкой (делим на 9.81)
 */
@SuppressLint("MissingPermission")
class PhoneSensorServer(
    private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "PhoneSensorServer"
        
        // UUID для BLE сервиса и характеристики
        // Используем уникальные UUID для нашего приложения
        val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val ACCELEROMETER_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        // Имя устройства для отображения в списке
        const val DEVICE_NAME_PREFIX = "PhoneSensor"
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private var accelerometerCharacteristic: BluetoothGattCharacteristic? = null
    
    private val connectedDevices = mutableSetOf<BluetoothDevice>()
    private var isAdvertising = false
    @Volatile private var isServiceAdded = false
    
    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()
    
    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()
    
    // Счётчик для логов
    private var sampleCount = 0
    private var lastLogTime = System.currentTimeMillis()

    /**
     * Запустить BLE-сервер и начать передачу данных акселерометра.
     */
    fun start(): Boolean {
        Log.i(TAG, "🚀 Starting PhoneSensorServer...")
        
        // Инициализация Bluetooth
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "❌ Bluetooth is not available or not enabled")
            _serverState.value = ServerState.ERROR
            return false
        }
        
        // Инициализация акселерометра
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        if (accelerometer == null) {
            Log.e(TAG, "❌ Accelerometer not available on this device")
            _serverState.value = ServerState.ERROR
            return false
        }
        
        // Логируем характеристики акселерометра
        Log.i(TAG, "📱 Accelerometer info:")
        Log.i(TAG, "   Name: ${accelerometer?.name}")
        Log.i(TAG, "   Vendor: ${accelerometer?.vendor}")
        Log.i(TAG, "   Resolution: ${accelerometer?.resolution} m/s²")
        Log.i(TAG, "   Max Range: ${accelerometer?.maximumRange} m/s² (${accelerometer?.maximumRange?.div(9.81)} g)")
        Log.i(TAG, "   Min Delay: ${accelerometer?.minDelay} μs")
        
        // Запускаем GATT-сервер
        isServiceAdded = false
        if (!startGattServer()) {
            Log.e(TAG, "❌ Failed to start GATT server")
            _serverState.value = ServerState.ERROR
            return false
        }
        
        // Сервис добавляется асинхронно. 
        // Начинаем advertising с небольшой задержкой через Handler
        Log.i(TAG, "📦 Service requested, scheduling advertising...")
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_serverState.value == ServerState.STOPPED) {
                Log.w(TAG, "Server was stopped, not starting advertising")
                return@postDelayed
            }
            
            Log.i(TAG, "✅ Starting advertising after delay...")
            
            // Начинаем BLE advertising
            if (!startAdvertising()) {
                Log.e(TAG, "❌ Failed to start BLE advertising")
                stopGattServer()
                _serverState.value = ServerState.ERROR
                return@postDelayed
            }
            
            // Регистрируем слушатель акселерометра
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST // ~200Hz на большинстве устройств
            )
            
            _serverState.value = ServerState.RUNNING
            Log.i(TAG, "✅ PhoneSensorServer started successfully")
        }, 1000) // 1 секунда задержки для добавления сервиса
        
        // Возвращаем true - сервер запускается асинхронно
        _serverState.value = ServerState.RUNNING // Временно RUNNING пока ждём
        return true
    }

    /**
     * Остановить BLE-сервер.
     */
    fun stop() {
        Log.i(TAG, "🛑 Stopping PhoneSensorServer...")
        
        // Остановить слушатель акселерометра
        sensorManager?.unregisterListener(this)
        
        // Остановить advertising
        stopAdvertising()
        
        // Остановить GATT-сервер
        stopGattServer()
        
        connectedDevices.clear()
        _connectedClientsCount.value = 0
        _serverState.value = ServerState.STOPPED
        
        Log.i(TAG, "✅ PhoneSensorServer stopped")
    }

    private fun startGattServer(): Boolean {
        try {
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                Log.e(TAG, "❌ Failed to open GATT server")
                return false
            }
            
            // Создаём сервис акселерометра
            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            
            // Создаём характеристику для данных акселерометра
            accelerometerCharacteristic = BluetoothGattCharacteristic(
                ACCELEROMETER_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or 
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            
            // Добавляем дескриптор для нотификаций (CCCD)
            val cccd = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or 
                    BluetoothGattDescriptor.PERMISSION_WRITE
            )
            accelerometerCharacteristic?.addDescriptor(cccd)
            
            service.addCharacteristic(accelerometerCharacteristic)
            
            val added = gattServer?.addService(service)
            Log.i(TAG, "📦 Adding service ${SERVICE_UUID}, addService returned: $added")
            Log.i(TAG, "   Service UUID: $SERVICE_UUID")
            Log.i(TAG, "   Characteristic UUID: $ACCELEROMETER_CHAR_UUID")
            
            // Сервис добавляется асинхронно, ждём callback onServiceAdded
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting GATT server", e)
            return false
        }
    }

    private fun stopGattServer() {
        try {
            gattServer?.close()
            gattServer = null
            accelerometerCharacteristic = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT server", e)
        }
    }

    private fun startAdvertising(): Boolean {
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "❌ BLE Advertiser not available")
            return false
        }
        
        // Устанавливаем имя устройства ПЕРЕД началом advertising
        val originalName = bluetoothAdapter?.name ?: "Phone"
        val deviceName = "${DEVICE_NAME_PREFIX}_${originalName.takeLast(4)}"
        try {
            bluetoothAdapter?.name = deviceName
            Log.i(TAG, "📡 Set device name to: $deviceName")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set device name", e)
        }
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Без таймаута
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        
        // Основные данные рекламы
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        // Дополнительные данные в scan response
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build()
        
        Log.i(TAG, "📡 Starting advertising as: $deviceName with UUID: $SERVICE_UUID")
        bluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        return true
    }

    private fun stopAdvertising() {
        if (isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            Log.i(TAG, "✅ BLE advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "❌ BLE advertising failed: $errorMsg")
            _serverState.value = ServerState.ERROR
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Service added successfully: ${service?.uuid}")
                isServiceAdded = true
            } else {
                Log.e(TAG, "❌ Failed to add service: status=$status, uuid=${service?.uuid}")
                isServiceAdded = false
            }
        }
        
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            val statusName = when (status) {
                BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
                else -> "ERROR($status)"
            }
            val stateName = when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "UNKNOWN($newState)"
            }
            Log.i(TAG, "📱 onConnectionStateChange: status=$statusName, state=$stateName, device=${device.address}")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Client connected: ${device.address}")
                    connectedDevices.add(device)
                    _connectedClientsCount.value = connectedDevices.size
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "📱 Client disconnected: ${device.address}")
                    connectedDevices.remove(device)
                    _connectedClientsCount.value = connectedDevices.size
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == ACCELEROMETER_CHAR_UUID) {
                val value = characteristic.value ?: ByteArray(12)
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            } else {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == CCCD_UUID) {
                // Клиент включает/выключает нотификации
                val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                Log.i(TAG, "📱 Notifications ${if (enabled) "enabled" else "disabled"} for ${device.address}")
                
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }
            }
        }
    }

    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (connectedDevices.isEmpty()) return
        
        // Конвертируем из m/s² в g
        val accXg = event.values[0] / 9.81f
        val accYg = event.values[1] / 9.81f
        val accZg = event.values[2] / 9.81f
        
        // Упаковываем данные в байты (3 float = 12 байт)
        val buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putFloat(accXg)
        buffer.putFloat(accYg)
        buffer.putFloat(accZg)
        val data = buffer.array()
        
        // Обновляем характеристику и отправляем нотификации
        accelerometerCharacteristic?.value = data
        
        for (device in connectedDevices) {
            try {
                gattServer?.notifyCharacteristicChanged(
                    device,
                    accelerometerCharacteristic,
                    false // без подтверждения (indication=false)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify device ${device.address}", e)
            }
        }
        
        // Логируем частоту
        sampleCount++
        val now = System.currentTimeMillis()
        if (now - lastLogTime >= 1000) {
            Log.d(TAG, "📊 Sending $sampleCount samples/sec to ${connectedDevices.size} clients")
            sampleCount = 0
            lastLogTime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    enum class ServerState {
        STOPPED,
        RUNNING,
        ERROR
    }
}
