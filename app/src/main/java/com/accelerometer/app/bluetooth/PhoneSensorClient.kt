package com.accelerometer.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import com.accelerometer.app.data.SensorSample
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BLE-клиент для подключения к телефону (PhoneSensorServer).
 * Получает данные акселерометра по BLE и преобразует в SensorSample.
 */
@SuppressLint("MissingPermission")
class PhoneSensorClient(
    private val context: Context
) {

    companion object {
        private const val TAG = "PhoneSensorClient"
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val discoveredPhones = mutableListOf<DiscoveredPhone>()
    private var isScanning = false
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Буфер 512 сэмплов (~5 сек при 100 Hz), при переполнении отбрасываем СТАРЫЕ данные
    private val _sensorSamples = MutableSharedFlow<SensorSample>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sensorSamples: SharedFlow<SensorSample> = _sensorSamples.asSharedFlow()
    
    // Счётчик для логов
    private var sampleCount = 0
    private var totalSampleCount = 0L
    private var lastLogTime = System.currentTimeMillis()

    data class DiscoveredPhone(
        val name: String,
        val address: String,
        val device: BluetoothDevice
    )

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    /**
     * Начать поиск телефонов с PhoneSensorServer.
     */
    fun startDiscovery() {
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "❌ BLE Scanner not available")
            return
        }
        
        if (isScanning) {
            Log.w(TAG, "⚠️ Already scanning")
            return
        }
        
        discoveredPhones.clear()
        Log.i(TAG, "🔍 Starting scan for PhoneSensor devices...")
        
        // Сканируем БЕЗ фильтра по UUID - многие телефоны не рекламируют UUID правильно
        // Фильтрация будет по имени в callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // Запускаем сканирование без фильтров
        bluetoothLeScanner?.startScan(null, settings, scanCallback)
        isScanning = true
        
        // Останавливаем сканирование через 10 секунд
        Handler(Looper.getMainLooper()).postDelayed({
            if (isScanning) {
                Log.d(TAG, "📊 Scan timeout, found ${discoveredPhones.size} phones")
            }
        }, 10000)
    }

    /**
     * Остановить поиск.
     */
    fun stopDiscovery() {
        if (isScanning) {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.i(TAG, "🛑 Scan stopped")
        }
    }

    /**
     * Получить список найденных телефонов.
     */
    fun getDiscoveredPhones(): List<DiscoveredPhone> {
        return discoveredPhones.toList()
    }

    /**
     * Подключиться к выбранному телефону.
     */
    fun connectToPhone(phone: DiscoveredPhone) {
        stopDiscovery()
        
        Log.i(TAG, "📱 Connecting to ${phone.name} (${phone.address})...")
        _connectionState.value = ConnectionState.CONNECTING
        
        // Подключение нужно делать из main thread на некоторых устройствах
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "📱 Calling connectGatt on main thread...")
                bluetoothGatt = phone.device.connectGatt(
                    context,
                    false, // autoConnect = false для быстрого подключения
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
                
                if (bluetoothGatt == null) {
                    Log.e(TAG, "❌ connectGatt returned null!")
                    _connectionState.value = ConnectionState.DISCONNECTED
                } else {
                    Log.i(TAG, "✅ connectGatt called successfully, waiting for callback...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during connectGatt", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Отключиться от телефона.
     */
    fun disconnect() {
        stopDiscovery()
        
        bluetoothGatt?.let { gatt ->
            Log.i(TAG, "📱 Disconnecting...")
            gatt.disconnect()
            gatt.close()
        }
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name
            
            // Логируем все найденные устройства для отладки
            if (name != null) {
                Log.d(TAG, "🔍 Found BLE device: '$name' (${device.address})")
            }
            
            // Проверяем, что это наш PhoneSensor (по имени или по Service UUID)
            val isPhoneSensor = name?.startsWith(PhoneSensorServer.DEVICE_NAME_PREFIX) == true ||
                result.scanRecord?.serviceUuids?.any { it.uuid == PhoneSensorServer.SERVICE_UUID } == true
            
            if (!isPhoneSensor) {
                return
            }
            
            // Проверяем, не добавлен ли уже
            if (discoveredPhones.any { it.address == device.address }) {
                return
            }
            
            val displayName = name ?: "PhoneSensor_${device.address.takeLast(5)}"
            val phone = DiscoveredPhone(displayName, device.address, device)
            discoveredPhones.add(phone)
            Log.i(TAG, "📱 Found phone sensor: $displayName (${device.address}), total: ${discoveredPhones.size}")
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "❌ Scan failed: $errorMsg")
            isScanning = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
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
            Log.i(TAG, "📱 onConnectionStateChange: status=$statusName, newState=$stateName")
            
            // Если статус не успешный - это ошибка подключения
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ Connection failed with status $status")
                _connectionState.value = ConnectionState.DISCONNECTED
                gatt.close()
                bluetoothGatt = null
                return
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Connected to phone")
                    // Запрашиваем высокий приоритет соединения
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    // Небольшая задержка перед обнаружением сервисов (помогает на некоторых устройствах)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.i(TAG, "📱 Starting service discovery...")
                        gatt.discoverServices()
                    }, 300)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "📱 Disconnected from phone")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ Service discovery failed: $status")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }
            
            Log.i(TAG, "✅ Services discovered, listing all services:")
            
            // Логируем ВСЕ найденные сервисы для отладки
            val services = gatt.services
            if (services.isEmpty()) {
                Log.w(TAG, "⚠️ No services found on device!")
            } else {
                for (service in services) {
                    Log.d(TAG, "  📦 Service: ${service.uuid}")
                    for (char in service.characteristics) {
                        Log.d(TAG, "      └─ Characteristic: ${char.uuid}")
                    }
                }
            }
            
            Log.d(TAG, "🔍 Looking for service: ${PhoneSensorServer.SERVICE_UUID}")
            
            // Находим наш сервис и характеристику
            val service = gatt.getService(PhoneSensorServer.SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "❌ PhoneSensor service not found")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }
            
            val characteristic = service.getCharacteristic(PhoneSensorServer.ACCELEROMETER_CHAR_UUID)
            if (characteristic == null) {
                Log.e(TAG, "❌ Accelerometer characteristic not found")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }
            
            // Включаем нотификации
            gatt.setCharacteristicNotification(characteristic, true)
            
            // Записываем в CCCD для включения нотификаций
            val cccd = characteristic.getDescriptor(PhoneSensorServer.CCCD_UUID)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            }
            
            _connectionState.value = ConnectionState.CONNECTED
            Log.i(TAG, "✅ Notifications enabled, ready to receive data")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != PhoneSensorServer.ACCELEROMETER_CHAR_UUID) return
            
            val data = characteristic.value
            if (data == null || data.size < 12) {
                Log.w(TAG, "⚠️ Invalid data received: ${data?.size ?: 0} bytes")
                return
            }
            
            // Распаковываем данные (3 float = 12 байт)
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val accXg = buffer.float.toDouble()
            val accYg = buffer.float.toDouble()
            val accZg = buffer.float.toDouble()
            
            val timestampSec = SystemClock.elapsedRealtimeNanos() / 1_000_000_000.0
            
            val sample = SensorSample(
                timestampSec = timestampSec,
                accXg = accXg,
                accYg = accYg,
                accZg = accZg,
                angleXDeg = 0.0, // Телефон не передаёт углы
                angleYDeg = 0.0,
                angleZDeg = 0.0
            )
            
            // С DROP_OLDEST tryEmit всегда успешен (старые данные удаляются при переполнении)
            _sensorSamples.tryEmit(sample)
            
            // Логируем частоту
            sampleCount++
            totalSampleCount++
            val now = System.currentTimeMillis()
            if (now - lastLogTime >= 1000) {
                Log.d(TAG, "📊 Receiving $sampleCount samples/sec (total: $totalSampleCount)")
                sampleCount = 0
                lastLogTime = now
            }
            
            // Логируем данные каждые 50 сэмплов
            if (totalSampleCount % 50 == 0L) {
                Log.d(TAG, "📦 Phone data: (${String.format("%.4f", accXg)}g, ${String.format("%.4f", accYg)}g, ${String.format("%.4f", accZg)}g)")
            }
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
