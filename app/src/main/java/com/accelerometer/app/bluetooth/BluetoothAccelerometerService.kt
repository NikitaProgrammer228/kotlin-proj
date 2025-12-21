package com.accelerometer.app.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.accelerometer.app.data.SensorSample
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothSPP
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.WitBluetoothManager
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver
import com.wit.witsdk.sensor.modular.device.exceptions.OpenDeviceException
import com.wit.example.ble5.Bwt901ble
import com.wit.example.ble5.interfaces.IBwt901bleRecordObserver
import com.wit.example.ble5.data.WitSensorKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Сервис, обёртывающий SDK WitMotion BLE 5.0.
 */
class BluetoothAccelerometerService(
    private val context: Context
) : IBluetoothFoundObserver, IBwt901bleRecordObserver {

    companion object {
        private const val TAG = "BluetoothAccelerometer"
        // Фильтр по имени устройства. Если пустой список - показываем все BLE устройства
        // Можно добавить другие варианты имен, например: "WT901BLECL", "WIT-MOTION", и т.д.
        private val DEVICE_NAME_FILTER = listOf("WT", "BWT", "WT901", "WIT", "BLECL")
        // Фильтр для телефонов-датчиков
        private const val PHONE_SENSOR_PREFIX = "PhoneSensor"
    }

    private var bluetoothManager: WitBluetoothManager? = null
    private val devices = mutableListOf<Bwt901ble>()
    private var connectedDevice: Bwt901ble? = null
    private val discoveredDevices = mutableListOf<DiscoveredDevice>()
    
    // Поддержка телефона как датчика
    private var phoneSensorClient: PhoneSensorClient? = null
    private var connectedToPhone = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Буфер 512 сэмплов (~5 сек при 100 Hz), при переполнении отбрасываем СТАРЫЕ данные
    private val _sensorSamples = MutableSharedFlow<SensorSample>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sensorSamples: SharedFlow<SensorSample> = _sensorSamples.asSharedFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    // Счётчик частоты данных
    private var sampleCount = 0
    private var lastLogTime = System.currentTimeMillis()

    data class DiscoveredDevice(
        val name: String?,
        val mac: String,
        val bluetoothBLE: BluetoothBLE? = null,
        val isPhone: Boolean = false,
        val phoneDevice: PhoneSensorClient.DiscoveredPhone? = null
    )

    init {
        initBluetoothManager()
        phoneSensorClient = PhoneSensorClient(context)
    }

    private fun initBluetoothManager() {
        try {
            if (bluetoothManager != null) {
                Log.d(TAG, "BluetoothManager already initialized")
                return
            }

            Log.d(TAG, "Initializing WitBluetoothManager...")
            if (context is Activity) {
                WitBluetoothManager.requestPermissions(context)
                WitBluetoothManager.initInstance(context)
            } else {
                WitBluetoothManager.initInstance(context.applicationContext)
            }
            bluetoothManager = WitBluetoothManager.getInstance()
            
            // ⚠️ КРИТИЧЕСКИ ВАЖНО: SDK фильтрует устройства по DeviceNameFilter
            // Если список пуст, SDK НЕ ПОКАЗЫВАЕТ устройства!
            // Добавляем наши фильтры в SDK
            val sdkFilter = WitBluetoothManager.DeviceNameFilter
            sdkFilter.clear()
            DEVICE_NAME_FILTER.forEach { filter ->
                sdkFilter.add(filter)
                Log.d(TAG, "Added to SDK filter: $filter")
            }
            Log.i(TAG, "✅ WitBluetoothManager initialized successfully with ${sdkFilter.size} name filters")
        } catch (ex: Exception) {
            Log.e(TAG, "❌ Failed to init WitBluetoothManager", ex)
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
        val manager = bluetoothManager ?: run {
            Log.e(TAG, "❌ BluetoothManager is null, cannot start discovery")
            return
        }
        discoveredDevices.clear()
        Log.i(TAG, "🔍 Starting device discovery for selection...")
        try {
            // Проверяем, зарегистрирован ли уже observer
            Log.d(TAG, "Registering observer: ${this::class.simpleName}")
            manager.registerObserver(this)
            Log.d(TAG, "Observer registered successfully")
            
            // Проверяем состояние Bluetooth адаптера
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Log.e(TAG, "❌ Bluetooth adapter is null - Bluetooth not supported")
                return
            }
            if (!bluetoothAdapter.isEnabled) {
                Log.e(TAG, "❌ Bluetooth adapter is not enabled")
                return
            }
            Log.d(TAG, "✅ Bluetooth adapter is enabled")
            
            // ⚠️ ВАЖНО: Проверяем уже сопряженные устройства
            // SDK может не находить уже сопряженные устройства через сканирование
            try {
                @SuppressLint("MissingPermission")
                val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                Log.d(TAG, "📱 Found ${pairedDevices.size} paired devices")
                pairedDevices.forEach { device ->
                    val deviceName = device.name ?: "Unknown"
                    val deviceMac = device.address
                    Log.d(TAG, "  - Paired device: $deviceName ($deviceMac)")
                    
                    // Проверяем, подходит ли устройство по фильтру
                    if (matchesDeviceName(deviceName)) {
                        // Проверяем тип устройства
                        try {
                            @SuppressLint("MissingPermission")
                            val deviceType = device.type
                            if (deviceType == BluetoothDevice.DEVICE_TYPE_LE || deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
                                // Создаем BluetoothBLE объект для сопряженного устройства
                                val bluetoothBLE = com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE(
                                    context as? Activity ?: context.applicationContext as Activity,
                                    deviceMac,
                                    deviceName
                                )
                                bluetoothBLE.setUUID(
                                    com.wit.witsdk.sensor.modular.connector.modular.bluetooth.constant.BleUUID.UUID_SERVICE.toString(),
                                    com.wit.witsdk.sensor.modular.connector.modular.bluetooth.constant.BleUUID.UUID_SEND.toString(),
                                    com.wit.witsdk.sensor.modular.connector.modular.bluetooth.constant.BleUUID.UUID_READ.toString()
                                )
                                
                                if (discoveredDevices.none { it.mac == deviceMac }) {
                                    discoveredDevices.add(DiscoveredDevice(deviceName, deviceMac, bluetoothBLE))
                                    Log.i(TAG, "✅ Added paired device to list: $deviceName ($deviceMac)")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to get device type for $deviceName", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get paired devices", e)
            }
            
            Log.d(TAG, "Calling manager.startDiscovery()...")
            manager.startDiscovery()
            Log.d(TAG, "✅ Discovery started successfully")
            
            // Логируем через 2 секунды, чтобы увидеть, были ли найдены устройства
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "📊 Discovery status after 2s: found ${discoveredDevices.size} devices")
            }, 2000)
        } catch (ex: BluetoothBLEException) {
            Log.e(TAG, "❌ Discovery failed", ex)
        } catch (ex: Exception) {
            Log.e(TAG, "❌ Unexpected error during discovery", ex)
        }
        
        // Также ищем телефоны с PhoneSensorServer
        Log.i(TAG, "🔍 Also starting phone sensor discovery...")
        phoneSensorClient?.startDiscovery()
    }

    /**
     * Получить список найденных устройств (включая телефоны).
     */
    fun getDiscoveredDevices(): List<DiscoveredDevice> {
        val allDevices = mutableListOf<DiscoveredDevice>()
        
        // Добавляем WitMotion датчики
        allDevices.addAll(discoveredDevices)
        
        // Добавляем телефоны
        phoneSensorClient?.getDiscoveredPhones()?.forEach { phone ->
            if (allDevices.none { it.mac == phone.address }) {
                allDevices.add(DiscoveredDevice(
                    name = phone.name,
                    mac = phone.address,
                    bluetoothBLE = null,
                    isPhone = true,
                    phoneDevice = phone
                ))
                Log.d(TAG, "📱 Added phone to list: ${phone.name}")
            }
        }
        
        return allDevices.toList()
    }

    /**
     * Подключиться к выбранному устройству (WitMotion или телефон).
     */
    fun connectToDevice(device: DiscoveredDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        stopDiscovery()
        
        if (device.isPhone && device.phoneDevice != null) {
            // Подключаемся к телефону
            connectToPhone(device.phoneDevice)
        } else if (device.bluetoothBLE != null) {
            // Подключаемся к WitMotion датчику
            connectToWitMotion(device)
        } else {
            Log.e(TAG, "❌ Invalid device: no BLE or phone data")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Подключиться к WitMotion датчику.
     */
    private fun connectToWitMotion(device: DiscoveredDevice) {
        val manager = bluetoothManager ?: return
        
        try {
            val sensor = Bwt901ble(device.bluetoothBLE)
            devices.add(sensor)
            connectedDevice = sensor
            connectedToPhone = false
            sensor.registerRecordObserver(this)
            sensor.open()
            
            // ⚡ HIGH PRIORITY автоматически устанавливается через bluetoothkit
            // (InukerBluetoothBLE.connect() передаёт BleConnectOptions, 
            //  BleConnectWorker автоматически вызывает requestConnectionPriority(HIGH))
            
            configureSensor(sensor)
            _connectionState.value = ConnectionState.CONNECTED
        } catch (ex: OpenDeviceException) {
            Log.e(TAG, "Failed to open device ${device.name}", ex)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Подключиться к телефону как датчику.
     */
    private fun connectToPhone(phone: PhoneSensorClient.DiscoveredPhone) {
        Log.i(TAG, "📱 Connecting to phone: ${phone.name}")
        connectedToPhone = true
        
        // Подключаемся к телефону
        phoneSensorClient?.connectToPhone(phone)
        
        // Запускаем сборщик данных от телефона
        kotlinx.coroutines.GlobalScope.launch {
            phoneSensorClient?.sensorSamples?.collect { sample ->
                // С DROP_OLDEST tryEmit всегда успешен
                _sensorSamples.tryEmit(sample)
            }
        }
        
        // Следим за состоянием подключения
        // Используем drop(1) чтобы пропустить начальное значение DISCONNECTED
        kotlinx.coroutines.GlobalScope.launch {
            phoneSensorClient?.connectionState?.collect { state ->
                Log.d(TAG, "📱 Phone connection state changed: $state")
                when (state) {
                    PhoneSensorClient.ConnectionState.CONNECTED -> {
                        Log.i(TAG, "✅ Phone connected successfully!")
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                    PhoneSensorClient.ConnectionState.DISCONNECTED -> {
                        // Только если мы действительно были подключены или пытались подключиться
                        if (connectedToPhone && _connectionState.value != ConnectionState.CONNECTING) {
                            Log.i(TAG, "📱 Phone disconnected")
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }
                    }
                    PhoneSensorClient.ConnectionState.CONNECTING -> {
                        Log.i(TAG, "📱 Phone connecting...")
                        _connectionState.value = ConnectionState.CONNECTING
                    }
                }
            }
        }
    }

    /**
     * Остановить поиск устройств (WitMotion и телефонов).
     */
    fun stopDiscovery() {
        // Останавливаем поиск телефонов
        phoneSensorClient?.stopDiscovery()
        
        // Останавливаем поиск WitMotion
        val manager = bluetoothManager ?: return
        try {
            manager.removeObserver(this)
            manager.stopDiscovery()
        } catch (ex: BluetoothBLEException) {
            Log.w(TAG, "stopDiscovery error", ex)
        }
    }

    /**
     * Отключение от текущего устройства (WitMotion или телефон).
     */
    fun disconnect() {
        stopDiscovery()
        
        // Отключаемся от телефона
        if (connectedToPhone) {
            phoneSensorClient?.disconnect()
            connectedToPhone = false
        }
        
        // Отключаемся от WitMotion
        devices.forEach {
            it.removeRecordObserver(this)
            it.close()
        }
        devices.clear()
        connectedDevice = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onFoundBle(bluetoothBLE: BluetoothBLE) {
        // Логируем все найденные устройства для отладки
        Log.i(TAG, "🔍 Found BLE device: name='${bluetoothBLE.name}', mac='${bluetoothBLE.mac}'")
        
        if (!matchesDeviceName(bluetoothBLE.name)) {
            Log.d(TAG, "⏭️ Skip device '${bluetoothBLE.name}' (${bluetoothBLE.mac}) - not matching filter ${DEVICE_NAME_FILTER}")
            return
        }
        
        // Если это режим выбора устройств, добавляем в список без подключения
        if (discoveredDevices.none { it.mac == bluetoothBLE.mac }) {
            discoveredDevices.add(DiscoveredDevice(bluetoothBLE.name, bluetoothBLE.mac, bluetoothBLE))
            Log.i(TAG, "✅ Added device to list: ${bluetoothBLE.name} (${bluetoothBLE.mac}), total devices: ${discoveredDevices.size}")
            return
        } else {
            Log.d(TAG, "⚠️ Device ${bluetoothBLE.mac} already in list, skipping")
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
            
            // ⚡ HIGH PRIORITY автоматически устанавливается через bluetoothkit
            // (InukerBluetoothBLE.connect() передаёт BleConnectOptions, 
            //  BleConnectWorker автоматически вызывает requestConnectionPriority(HIGH))
            
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
     * Настройка датчика для МАКСИМАЛЬНОЙ частоты по BLE.
     * 
     * КЛЮЧЕВОЕ РЕШЕНИЕ от заказчика:
     * Канал BLE перегружен лишними данными (углы, гироскоп, магнитометр, батарея).
     * Если установить RSW (Return Content) ТОЛЬКО на ускорение,
     * канал освободится и датчик сможет выдавать 50 Hz вместо 10 Hz.
     * 
     * Регистры:
     * - RSW (0x02): Что возвращать (RSW_ACC=0x02 - только ускорение)
     * - RRATE (0x03): Частота (RRATE_50HZ=0x08)
     */
    private fun configureSensor(sensor: Bwt901ble) {
        Thread {
            try {
                Log.i(TAG, "🔧 Configuring sensor for 50 Hz (optimized for BLE)...")
                
                // ⚠️ ВАЖНО: Ждём пока соединение полностью установится
                // Датчик должен быть готов к приёму команд
                var waitCount = 0
                while (!sensor.isOpen() && waitCount < 20) {
                    Thread.sleep(100)
                    waitCount++
                }
                if (!sensor.isOpen()) {
                    Log.w(TAG, "⚠️ Sensor not ready after ${waitCount * 100}ms, proceeding anyway...")
                } else {
                    Log.d(TAG, "✅ Sensor is ready (waited ${waitCount * 100}ms)")
                }
                
                // Дополнительная задержка для стабилизации соединения
                Thread.sleep(500)
                
                // === ШАГ 1: Разблокировка регистров ===
                Log.d(TAG, "→ Sending UNLOCK command...")
                sensor.unlockReg()
                Thread.sleep(800)
                Log.d(TAG, "→ Registers unlocked")
                
                // === ШАГ 2: Установка диапазона ±2g (ПЕРВЫМ ДЕЛОМ) ===
                Log.d(TAG, "→ Setting ACCRANGE=0x00 (±2g)...")
                for (i in 1..4) {
                    sensor.unlockReg()
                    Thread.sleep(300)
                    // Прямая команда записи в регистр 0x21
                    sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x21, 0x00, 0x00), 500)
                    Thread.sleep(500)
                    // Попробуем еще через специальную команду калибровки, иногда это «пробивает» настройки
                    if (i == 3) {
                        Log.d(TAG, "  → Special attempt with calibration command...")
                        sensor.appliedCalibration()
                        Thread.sleep(800)
                    }
                    sensor.saveReg()
                    Thread.sleep(1000)
                    
                    // Проверяем
                    sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x27, 0x21, 0x00))
                    Thread.sleep(1000)
                    val check = sensor.getDeviceData("21")
                    if (check == "0") {
                        Log.i(TAG, "✅ ACCRANGE set to ±2g (attempt $i)")
                        break
                    } else {
                        Log.w(TAG, "⚠️ ACCRANGE still $check (attempt $i), retrying...")
                    }
                }

                // === ШАГ 3: Установка Return Content ===
                Log.d(TAG, "→ Sending RSW=0x02 (ACC_ONLY) command...")
                sensor.unlockReg()
                Thread.sleep(200)
                sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x02, 0x02, 0x00), 500)
                Thread.sleep(500)
                sensor.saveReg()
                Thread.sleep(800)
                
                // === ШАГ 4: Установка частоты 50 Hz ===
                Log.d(TAG, "→ Sending RRATE=0x08 (50Hz) command...")
                sensor.unlockReg()
                Thread.sleep(200)
                sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x03, 0x08, 0x00), 500)
                Thread.sleep(500)
                sensor.saveReg()
                Thread.sleep(800)
                
                // === ШАГ 5: Финальное сохранение ===
                Log.d(TAG, "→ Sending final SAVE command...")
            sensor.unlockReg()
                Thread.sleep(200)
                sensor.saveReg()
                Thread.sleep(1500)
                Log.d(TAG, "→ Settings saved to EEPROM")
            
                // === ШАГ 6: Читаем регистры для проверки ===
                fun readRegister(regName: String, regAddr: Int, expectedValue: String, maxRetries: Int = 3): String? {
                    for (attempt in 1..maxRetries) {
                        Log.d(TAG, "→ Reading $regName register (0x${regAddr.toString(16).uppercase()})... (attempt $attempt/$maxRetries)")
                        sensor.sendProtocolData(byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x27, regAddr.toByte(), 0x00))
                        Thread.sleep(800)
                        val value = sensor.getDeviceData(regAddr.toString(16).padStart(2, '0'))
                        Log.d(TAG, "  → $regName read result: '$value' (expect $expectedValue)")
                        if (value != null && value.isNotEmpty() && value != "null") {
                            return value
                        }
                        Thread.sleep(500)
                    }
                    return null
                }
                
                val rswValue = readRegister("RSW", 0x02, "2=ACC_ONLY")
                val currentRate = readRegister("RRATE", 0x03, "8=50Hz")
                val accRange = readRegister("ACCRANGE", 0x21, "0=±2g")
            
                Log.i(TAG, "✓ Sensor configuration complete:")
                Log.i(TAG, "  📤 RSW: ${rswValue ?: "null"}")
                Log.i(TAG, "  ⏱️ RRATE: ${currentRate ?: "null"}")
                Log.i(TAG, "  📏 ACCRANGE: ${accRange ?: "null"}")
                
                Log.i(TAG, "✓ Sensor configuration complete:")
                Log.i(TAG, "  📤 RSW (return content): ${rswValue ?: "null"} (expect 2=ACC_ONLY)")
                Log.i(TAG, "  ⏱️ RRATE (frequency): ${currentRate ?: "null"} (expect 8=50Hz)")
                Log.i(TAG, "  📏 ACCRANGE: ${accRange ?: "null"} (expect 0=±2g)")
                Log.i(TAG, "  🎯 Expected result: ~50 samples/sec instead of 10!")
                
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to configure sensor", ex)
        }
        }.start()
    }
    
    // ⚡ HIGH PRIORITY автоматически устанавливается через bluetoothkit:
    // - InukerBluetoothBLE.connect() передаёт BleConnectOptions
    // - BleConnectWorker.onConnectionStateChange() автоматически вызывает requestConnectionPriority(HIGH)
    // - Логи можно найти по тегу "BleConnectWorker" в Logcat

    override fun onFoundSPP(bluetoothSPP: BluetoothSPP) {
        Log.d(TAG, "🔍 onFoundSPP called (ignored - BLE-only app): name='${bluetoothSPP.name}', mac='${bluetoothSPP.mac}'")
        // BLE-only приложение, поэтому игнорируем
    }

    override fun onFoundDual(bluetoothBLE: BluetoothBLE) {
        Log.d(TAG, "🔍 onFoundDual called: name='${bluetoothBLE.name}', mac='${bluetoothBLE.mac}'")
        // Обрабатываем как обычный BLE
        onFoundBle(bluetoothBLE)
    }

    // Для редкого логирования батареи
    private var lastBatteryLogTime = 0L
    private var totalSampleCount = 0L  // Общий счётчик для логов

    override fun onRecord(bwt901ble: Bwt901ble) {
        // Счётчик частоты (логируем раз в секунду)
        sampleCount++
        totalSampleCount++
        val now = System.currentTimeMillis()
        if (now - lastLogTime >= 1000) {
            Log.d(TAG, "📊 Sample rate: $sampleCount samples/sec (total: $totalSampleCount)")
            sampleCount = 0
            lastLogTime = now
        }
        
        // Получаем RAW данные ускорения (int16)
        val rawAccX = bwt901ble.getDeviceData("61_0")
        val rawAccY = bwt901ble.getDeviceData("61_1")
        val rawAccZ = bwt901ble.getDeviceData("61_2")
        
        // SDK парсит ускорения в g
        val accXgStr = bwt901ble.getDeviceData(WitSensorKey.AccX)
        val accYgStr = bwt901ble.getDeviceData(WitSensorKey.AccY)
        val accZgStr = bwt901ble.getDeviceData(WitSensorKey.AccZ)
        
        val accXg = parseAccelerationG(accXgStr) ?: 0.0
        val accYg = parseAccelerationG(accYgStr) ?: 0.0
        val accZg = parseAccelerationG(accZgStr) ?: 0.0
        
        // Логируем проблему с нулевыми данными (только первые несколько раз)
        if (totalSampleCount <= 5 && (accXg == 0.0 && accYg == 0.0 && accZg == 0.0)) {
            Log.w(TAG, "⚠️ Zero acceleration data! RAW:($rawAccX,$rawAccY,$rawAccZ) SDK strings: AccX='$accXgStr', AccY='$accYgStr', AccZ='$accZgStr'")
            Log.w(TAG, "   Check: RSW register should be 0x02 (ACC_ONLY). Current value: ${bwt901ble.getDeviceData("02")}")
        }
        
        // Углы могут быть недоступны если RSW установлен на ACC_ONLY
        // Используем 0.0 как значение по умолчанию
        val angleX = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleX)) ?: 0.0
        val angleY = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleY)) ?: 0.0
        val angleZ = parseAngleDegrees(bwt901ble.getDeviceData(WitSensorKey.AngleZ)) ?: 0.0
        
        // Логируем RAW данные каждые 50 сэмплов (не чаще!)
        if (totalSampleCount % 50 == 0L) {
            Log.d(TAG, "📦 RAW:($rawAccX,$rawAccY,$rawAccZ) SDK:(${String.format("%.4f", accXg)}g,${String.format("%.4f", accYg)}g,${String.format("%.4f", accZg)}g)")
        }
        
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
        // С DROP_OLDEST tryEmit всегда успешен
        _sensorSamples.tryEmit(sample)

        // ⚠️ Батарею проверяем РЕДКО (раз в 30 секунд), чтобы не спамить BLE канал!
        if (now - lastBatteryLogTime >= 30_000) {
            lastBatteryLogTime = now
            try {
                val voltageRaw = bwt901ble.getDeviceData("ElectricQuantityPercentage")
                if (!voltageRaw.isNullOrBlank()) {
                    val voltage = voltageRaw.replace(',', '.').toDoubleOrNull()
                    if (voltage != null) {
                        val batteryPercent = voltage.toInt().coerceIn(0, 100)
                        _batteryLevel.value = batteryPercent
                        Log.d(TAG, "🔋 Battery: $batteryPercent%")
                    }
                }
            } catch (ex: Exception) {
                // Игнорируем ошибки батареи - это не критично
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
            // Углы могут быть недоступны если RSW установлен на ACC_ONLY - это нормально
            return null
        }
        val normalized = raw.replace(',', '.')
        return normalized.toDoubleOrNull()
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
        // Если имя null или пустое, пропускаем (но логируем)
        if (deviceName.isNullOrBlank()) {
            Log.d(TAG, "⚠️ Device name is null or blank")
            return false
        }
        val normalized = deviceName.uppercase()
        val matches = DEVICE_NAME_FILTER.any { normalized.contains(it.uppercase()) }
        if (!matches) {
            Log.d(TAG, "❌ Device name '$deviceName' (normalized: '$normalized') doesn't match any filter: $DEVICE_NAME_FILTER")
        }
        return matches
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
