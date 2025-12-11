package com.accelerometer.app.bluetooth

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.accelerometer.app.data.SensorSample
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver
import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey
import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Сервис, обёртывающий SDK WitMotion BLE 5.0.
 */
class BluetoothAccelerometerService(
    private val context: Context
) : IBluetoothFoundObserver, IBwt901bleRecordObserver {

    companion object {
        private const val TAG = "BluetoothAccelerometer"
        private val DEVICE_NAME_FILTER = listOf("WT", "BWT", "WT901")
    }

    private var bluetoothManager: WitBluetoothManager? = null
    private val devices = mutableListOf<Bwt901ble>()
    private var connectedDevice: Bwt901ble? = null
    private val discoveredDevices = mutableListOf<DiscoveredDevice>()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorSamples = MutableSharedFlow<SensorSample>(extraBufferCapacity = 256)
    val sensorSamples: SharedFlow<SensorSample> = _sensorSamples.asSharedFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    data class DiscoveredDevice(
        val name: String?,
        val mac: String,
        val bluetoothBLE: BluetoothBLE
    )

    init {
        initBluetoothManager()
    }

    private fun initBluetoothManager() {
        try {
            if (bluetoothManager != null) return

            if (context is Activity) {
                WitBluetoothManager.requestPermissions(context)
                WitBluetoothManager.initInstance(context)
            } else {
                WitBluetoothManager.initInstance(context.applicationContext)
            }
            bluetoothManager = WitBluetoothManager.getInstance()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to init WitBluetoothManager", ex)
        }
    }

    /**
     * Начать поиск устройств и подключиться к первому найденному датчику.
     */
    fun startDiscovery() {
        initBluetoothManager()
        val manager = bluetoothManager ?: return
        _connectionState.value = ConnectionState.CONNECTING
        clearDevices()
        try {
            manager.registerObserver(this)
            manager.startDiscovery()
        } catch (ex: BluetoothBLEException) {
            Log.e(TAG, "Discovery failed", ex)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Начать поиск устройств без автоподключения.
     * Найденные устройства добавляются в список discoveredDevices.
     */
    fun startDiscoveryForSelection() {
        initBluetoothManager()
        val manager = bluetoothManager ?: return
        discoveredDevices.clear()
        try {
            manager.registerObserver(this)
            manager.startDiscovery()
        } catch (ex: BluetoothBLEException) {
            Log.e(TAG, "Discovery failed", ex)
        }
    }

    /**
     * Получить список найденных устройств.
     */
    fun getDiscoveredDevices(): List<DiscoveredDevice> {
        return discoveredDevices.toList()
    }

    /**
     * Подключиться к выбранному устройству.
     */
    fun connectToDevice(device: DiscoveredDevice) {
        val manager = bluetoothManager ?: return
        _connectionState.value = ConnectionState.CONNECTING
        stopDiscovery()
        
        try {
            val sensor = Bwt901ble(device.bluetoothBLE)
            devices.add(sensor)
            connectedDevice = sensor
            sensor.registerRecordObserver(this)
            sensor.open()
            configureSensor(sensor)
            _connectionState.value = ConnectionState.CONNECTED
        } catch (ex: OpenDeviceException) {
            Log.e(TAG, "Failed to open device ${device.name}", ex)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Остановить поиск устройств.
     */
    fun stopDiscovery() {
        val manager = bluetoothManager ?: return
        try {
            manager.removeObserver(this)
            manager.stopDiscovery()
        } catch (ex: BluetoothBLEException) {
            Log.w(TAG, "stopDiscovery error", ex)
        }
    }

    /**
     * Отключение от текущего устройства.
     */
    fun disconnect() {
        stopDiscovery()
        devices.forEach {
            it.removeRecordObserver(this)
            it.close()
        }
        devices.clear()
        connectedDevice = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onFoundBle(bluetoothBLE: BluetoothBLE) {
        if (!matchesDeviceName(bluetoothBLE.name)) {
            Log.d(TAG, "Skip device ${bluetoothBLE.name} not matching filter")
            return
        }
        
        // Если это режим выбора устройств, добавляем в список без подключения
        if (discoveredDevices.none { it.mac == bluetoothBLE.mac }) {
            discoveredDevices.add(DiscoveredDevice(bluetoothBLE.name, bluetoothBLE.mac, bluetoothBLE))
            Log.d(TAG, "Discovered device: ${bluetoothBLE.name} (${bluetoothBLE.mac})")
            return
        }
        
        // Старый режим автоподключения (для обратной совместимости)
        if (devices.any { it.mac == bluetoothBLE.mac }) {
            return
        }
        val sensor = Bwt901ble(bluetoothBLE)
        devices.add(sensor)
        connectedDevice = sensor
        sensor.registerRecordObserver(this)
        try {
            sensor.open()
            // Настраиваем датчик на 50 Гц (RRATE_50HZ = 0x08)
            configureSensor(sensor)
            stopDiscovery()
            _connectionState.value = ConnectionState.CONNECTED
        } catch (ex: OpenDeviceException) {
            Log.e(TAG, "Failed to open device ${sensor.deviceName}", ex)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Настройка датчика: частота 50 Гц, разблокировка регистров
     */
    private fun configureSensor(sensor: Bwt901ble) {
        try {
            // Разблокируем регистры для записи
            sensor.unlockReg()
            Thread.sleep(100)
            
            // Устанавливаем частоту 50 Гц (0x08 = RRATE_50HZ)
            sensor.setReturnRate(0x08.toByte())
            Thread.sleep(100)
            
            // Сохраняем настройки (команда: FF AA 00 00 00)
            sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x00, 0x00, 0x00))
            
            Log.i(TAG, "Sensor configured: 50 Hz output rate")
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to configure sensor", ex)
        }
    }

    override fun onFoundSPP(bluetoothSPP: BluetoothSPP) {
        // BLE-only приложение, поэтому игнорируем
    }

    override fun onRecord(bwt901ble: Bwt901ble) {
        // SDK возвращает значения уже в g (не raw), поэтому парсим напрямую как Double
        val accXg = parseAccelerationG(bwt901ble.getDeviceData(WitSensorKey.AccX)) ?: return
        val accYg = parseAccelerationG(bwt901ble.getDeviceData(WitSensorKey.AccY)) ?: return
        val accZg = parseAccelerationG(bwt901ble.getDeviceData(WitSensorKey.AccZ)) ?: return
        val angleX = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleX)) ?: return
        val angleY = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleY)) ?: return
        val angleZ = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleZ)) ?: return
        val timestampSec = SystemClock.elapsedRealtimeNanos() / 1_000_000_000.0
        val sample = SensorSample(
            timestampSec = timestampSec,
            accXg = accXg,
            accYg = accYg,
            accZg = accZg,
            angleXDeg = angleX,
            angleYDeg = angleY,
            angleZDeg = angleZ
        )
        if (!_sensorSamples.tryEmit(sample)) {
            Log.w(TAG, "Dropped sensor sample due to backpressure")
        }

        // Получаем уровень заряда батареи (напряжение в мВ, конвертируем в проценты)
        try {
            val voltageRaw = bwt901ble.getDeviceData("ElectricQuantityPercentage")
            if (!voltageRaw.isNullOrBlank()) {
                val voltage = voltageRaw.replace(',', '.').toDoubleOrNull()
                if (voltage != null) {
                    // Если SDK возвращает проценты напрямую
                    val batteryPercent = voltage.toInt().coerceIn(0, 100)
                    _batteryLevel.value = batteryPercent
                    Log.d(TAG, "Battery level: $batteryPercent%")
                }
            }
        } catch (ex: Exception) {
            // Некоторые сенсоры могут не поддерживать PowerPercent, пробуем Voltage
            try {
                val voltageRaw = bwt901ble.getDeviceData("Voltage")
                if (!voltageRaw.isNullOrBlank()) {
                    val voltage = voltageRaw.replace(',', '.').toDoubleOrNull()
                    if (voltage != null) {
                        // Конвертируем напряжение в проценты (3.3V = 0%, 4.2V = 100%)
                        val batteryPercent = ((voltage - 3.3) / (4.2 - 3.3) * 100).toInt().coerceIn(0, 100)
                        _batteryLevel.value = batteryPercent
                        Log.d(TAG, "Battery voltage: ${voltage}V -> $batteryPercent%")
                    }
                }
            } catch (ex2: Exception) {
                Log.w(TAG, "Failed to get battery level", ex2)
            }
        }
    }

    private fun parseAccelerationG(raw: String?): Double? {
        if (raw.isNullOrBlank()) {
            return null
        }
        // SDK возвращает значения с запятой в качестве разделителя (например "-0,0170")
        val normalized = raw.replace(',', '.')
        return normalized.toDoubleOrNull()
    }

    private fun parseAngleDegrees(raw: String?): Double? {
        if (raw.isNullOrBlank()) {
            Log.w(TAG, "parseAngle: empty value")
            return null
        }
        val normalized = raw.replace(',', '.')
        return normalized.toDoubleOrNull() ?: run {
            Log.w(TAG, "parseAngle: cannot parse $raw")
            null
        }
    }

    private fun clearDevices() {
        devices.forEach {
            it.removeRecordObserver(this)
            it.close()
        }
        devices.clear()
        connectedDevice = null
    }

    private fun matchesDeviceName(deviceName: String?): Boolean {
        if (DEVICE_NAME_FILTER.isEmpty()) return true
        val normalized = deviceName?.uppercase() ?: return true
        return DEVICE_NAME_FILTER.any { normalized.contains(it.uppercase()) }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
