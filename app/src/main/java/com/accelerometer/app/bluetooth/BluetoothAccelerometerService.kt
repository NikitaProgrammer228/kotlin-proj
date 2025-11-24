package com.accelerometer.app.bluetooth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.accelerometer.app.data.AccelerometerData
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver
import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey
import com.wit.witsdk.modular.witsensorapi.modular.ble5.Bwt901ble
import com.wit.witsdk.modular.witsensorapi.modular.ble5.interfaces.IBwt901bleRecordObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        private const val HISTORY_LIMIT = 2_000
        private const val GRAVITY = 9.80665f
    }

    private var bluetoothManager: WitBluetoothManager? = null
    private val devices = mutableListOf<Bwt901ble>()
    private var connectedDevice: Bwt901ble? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _accelerometerData = MutableStateFlow<AccelerometerData?>(null)
    val accelerometerData: StateFlow<AccelerometerData?> = _accelerometerData.asStateFlow()

    private val _dataHistory = MutableStateFlow<List<AccelerometerData>>(emptyList())
    val dataHistory: StateFlow<List<AccelerometerData>> = _dataHistory.asStateFlow()

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
        _dataHistory.value = emptyList()
    }

    fun clearHistory() {
        _dataHistory.value = emptyList()
    }

    override fun onFoundBle(bluetoothBLE: BluetoothBLE) {
        if (!matchesDeviceName(bluetoothBLE.name)) {
            Log.d(TAG, "Skip device ${bluetoothBLE.name} not matching filter")
            return
        }
        if (devices.any { it.mac == bluetoothBLE.mac }) {
            return
        }
        val sensor = Bwt901ble(bluetoothBLE)
        devices.add(sensor)
        connectedDevice = sensor
        sensor.registerRecordObserver(this)
        try {
            sensor.open()
            stopDiscovery()
            _connectionState.value = ConnectionState.CONNECTED
        } catch (ex: OpenDeviceException) {
            Log.e(TAG, "Failed to open device ${sensor.deviceName}", ex)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun onFoundSPP(bluetoothSPP: BluetoothSPP) {
        // BLE-only приложение, поэтому игнорируем
    }

    override fun onRecord(bwt901ble: Bwt901ble) {
        val x = parseAcceleration(bwt901ble.getDeviceData(WitSensorKey.AccX)) ?: return
        val y = parseAcceleration(bwt901ble.getDeviceData(WitSensorKey.AccY)) ?: return
        val accelerometerData = AccelerometerData(x, y)
        _accelerometerData.value = accelerometerData

        val history = _dataHistory.value.toMutableList()
        history.add(accelerometerData)
        if (history.size > HISTORY_LIMIT) {
            history.removeAt(0)
        }
        _dataHistory.value = history
    }

    private fun parseAcceleration(raw: String?): Float? {
        val value = raw?.toFloatOrNull() ?: return null
        return value * GRAVITY
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
        val normalized = deviceName?.uppercase() ?: return false
        return DEVICE_NAME_FILTER.any { normalized.contains(it.uppercase()) }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
