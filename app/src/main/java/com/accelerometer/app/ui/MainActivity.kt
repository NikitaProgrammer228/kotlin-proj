package com.accelerometer.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.accelerometer.app.R
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService
import com.accelerometer.app.bluetooth.PhoneSensorServer
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.MeasurementStatus
import com.accelerometer.app.databinding.ActivityMainBinding
import com.accelerometer.app.export.ExportService
import com.accelerometer.app.export.PdfExportService
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.service.MeasurementRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bluetoothService: BluetoothAccelerometerService
    private var measurementDurationSec = MeasurementConfig.MEASUREMENT_DURATION_SEC
    private val chartRangeMm = MeasurementConfig.CHART_AXIS_RANGE_MM.toFloat()
    private lateinit var measurementRepository: MeasurementRepository
    private var currentUserId: Long = 0  // TODO: получить из выбранного пользователя
    
    // Автозапуск теста
    private var autoStartEnabled = true
    private val motionThresholdG = 0.05  // Порог движения для автозапуска (0.05g)
    private var lastAccelerationMagnitude = 0.0
    
    // Режим сервера (телефон как датчик)
    private var phoneSensorServer: PhoneSensorServer? = null
    private var isServerMode = false
    
    companion object {
        private val TEST_DURATIONS = listOf(10.0, 20.0, 30.0)
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            showDeviceSelectionDialog()
        } else {
            Toast.makeText(this, "Необходимы разрешения для работы с Bluetooth", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        bluetoothService = BluetoothAccelerometerService(this)
        measurementRepository = MeasurementRepository(AppDatabase.getDatabase(this))
        
        setupDurationSpinner()
        setupCharts()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupDurationSpinner() {
        val durations = TEST_DURATIONS.map { "${it.toInt()} сек" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = adapter
        
        // Устанавливаем значение по умолчанию (10 сек)
        val defaultIndex = TEST_DURATIONS.indexOf(measurementDurationSec)
        if (defaultIndex >= 0) {
            binding.spinnerDuration.setSelection(defaultIndex)
        }
        
        binding.spinnerDuration.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                measurementDurationSec = TEST_DURATIONS[position]
                // Если тест уже запущен, перезапускаем с новым временем
                if (bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED) {
                    viewModel.stopMeasurement()
                    viewModel.startMeasurement(measurementDurationSec)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    private fun setupCharts() {
        setupTimeChart(binding.chartX)
        setupTimeChart(binding.chartY)
    }
    
    private fun setupTimeChart(chart: com.github.mikephil.charting.charts.LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        
        chart.legend.isEnabled = false
        chart.animateX(500)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bluetoothService.connectionState.collect { updateConnectionState(it) }
                }
                launch {
                    bluetoothService.sensorSamples.collect { sample ->
                        // Автозапуск теста по порогу движения
                        if (autoStartEnabled && viewModel.measurementState.value.status == MeasurementStatus.IDLE) {
                            val accMagnitude = kotlin.math.sqrt(
                                sample.accXg * sample.accXg + 
                                sample.accYg * sample.accYg + 
                                sample.accZg * sample.accZg
                            )
                            if (accMagnitude > motionThresholdG) {
                                viewModel.startMeasurement(measurementDurationSec)
                                binding.tvMeasurementStatus.text = getString(R.string.measurement_running)
                            }
                            lastAccelerationMagnitude = accMagnitude
                        }
                        viewModel.onSensorSample(sample)
                    }
                }
                launch {
                    viewModel.measurementState.collect { renderMeasurementState(it) }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            if (isServerMode || bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED) {
                disconnect()
            } else {
                checkPermissionsAndConnect()
            }
        }
        
        binding.btnUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
        
        binding.btnBalanceTest.setOnClickListener {
            val intent = Intent(this, BalanceTestActivity::class.java)
            intent.putExtra("userId", currentUserId)
            startActivity(intent)
        }
        
        binding.btnExport.setOnClickListener {
            exportMeasurement()
        }
    }
    
    private fun checkPermissionsAndConnect() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE, // Для режима сервера
                Manifest.permission.ACCESS_FINE_LOCATION // required unless we opt-out via neverForLocation flag
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            showDeviceSelectionDialog()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun showDeviceSelectionDialog() {
        // Показываем диалог выбора режима
        val options = arrayOf(
            "🔍 Поиск датчиков (клиент)",
            "📱 Использовать этот телефон как датчик (сервер)"
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Выберите режим")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startClientMode()
                    1 -> startServerMode()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun startClientMode() {
        isServerMode = false
        Toast.makeText(this, "🔍 Поиск датчиков и телефонов...\nПодождите 5 секунд", Toast.LENGTH_LONG).show()
        binding.connectionStatus.text = "🔍 Поиск устройств..."
        bluetoothService.startDiscoveryForSelection()
        
        // Показываем диалог выбора устройства через 5 секунд (дольше для надёжности)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showDeviceListDialog()
        }, 5000)
    }
    
    private fun showDeviceListDialog() {
        val devices = bluetoothService.getDiscoveredDevices()
        
        if (devices.isEmpty()) {
            Toast.makeText(this, "Устройства не найдены. Повторите поиск.", Toast.LENGTH_LONG).show()
            return
        }
        
        val deviceNames = devices.map { device ->
            if (device.isPhone) {
                "📱 ${device.name ?: device.mac}"
            } else {
                "🔵 ${device.name ?: device.mac}"
            }
        }.toTypedArray()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Выберите устройство")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = devices[which]
                bluetoothService.connectToDevice(selectedDevice)
            }
            .setNegativeButton("Повторить поиск") { _, _ ->
                startClientMode()
            }
            .setNeutralButton("Отмена") { _, _ ->
                bluetoothService.stopDiscovery()
            }
            .show()
    }
    
    private fun startServerMode() {
        isServerMode = true
        
        if (phoneSensorServer == null) {
            phoneSensorServer = PhoneSensorServer(this)
        }
        
        if (phoneSensorServer?.start() == true) {
            // Показываем подробную инструкцию
            android.app.AlertDialog.Builder(this)
                .setTitle("📱 Режим сервера запущен")
                .setMessage(
                    "Этот телефон теперь работает как датчик.\n\n" +
                    "Инструкция:\n" +
                    "1. Положите телефон на платформу Simprove\n" +
                    "2. На ПЛАНШЕТЕ откройте приложение\n" +
                    "3. Нажмите 'Подключиться' → 'Поиск датчиков'\n" +
                    "4. Выберите '📱 PhoneSensor_...' из списка\n\n" +
                    "Имя устройства: PhoneSensor_*"
                )
                .setPositiveButton("OK", null)
                .show()
            
            binding.connectionStatus.text = "📱 СЕРВЕР АКТИВЕН - ищите с планшета"
            binding.btnConnect.text = "Остановить сервер"
            
            // Наблюдаем за состоянием сервера
            lifecycleScope.launch {
                phoneSensorServer?.connectedClientsCount?.collect { count ->
                    if (count > 0) {
                        binding.connectionStatus.text = "📱 Сервер: $count клиент(ов) подключено ✅"
                    } else {
                        binding.connectionStatus.text = "📱 СЕРВЕР АКТИВЕН - ищите с планшета"
                    }
                }
            }
        } else {
            Toast.makeText(this, "❌ Не удалось запустить режим сервера.\nПроверьте разрешения Bluetooth.", Toast.LENGTH_LONG).show()
            isServerMode = false
        }
    }
    
    private fun stopServerMode() {
        phoneSensorServer?.stop()
        isServerMode = false
        binding.connectionStatus.text = getString(R.string.disconnected)
        binding.btnConnect.text = getString(R.string.connect)
    }
    
    private fun disconnect() {
        if (isServerMode) {
            stopServerMode()
        } else {
            bluetoothService.disconnect()
        }
        viewModel.stopMeasurement()
        clearCharts()
    }
    
    private fun updateConnectionState(state: BluetoothAccelerometerService.ConnectionState) {
        when (state) {
            BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                binding.connectionStatus.text = getString(R.string.disconnected)
                binding.btnConnect.text = getString(R.string.connect)
                viewModel.stopMeasurement()
                clearCharts()
                binding.tvMeasurementStatus.text = getString(R.string.measurement_idle)
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTING -> {
                binding.connectionStatus.text = getString(R.string.scanning)
                binding.btnConnect.text = getString(R.string.connect)
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTED -> {
                binding.connectionStatus.text = getString(R.string.connected)
                binding.btnConnect.text = getString(R.string.disconnect)
                viewModel.startMeasurement(measurementDurationSec)
                binding.tvMeasurementStatus.text = getString(R.string.measurement_running)
            }
        }
    }

    private fun renderMeasurementState(state: MeasurementState) {
        val statusText = when (state.status) {
            MeasurementStatus.IDLE -> getString(R.string.measurement_idle)
            MeasurementStatus.CALIBRATING -> getString(R.string.measurement_calibrating)
            MeasurementStatus.RUNNING -> getString(R.string.measurement_running)
            MeasurementStatus.FINISHED -> getString(R.string.measurement_finished)
        }
        binding.tvMeasurementStatus.text = statusText
        binding.tvMeasurementTime.text = getString(R.string.measurement_time, state.elapsedSec.toInt())
        
        // Включаем кнопку экспорта только после завершения измерения
        binding.btnExport.isEnabled = state.status == MeasurementStatus.FINISHED && state.result != null
        
        // Сохраняем результат в БД при завершении измерения (только один раз)
        if (state.status == MeasurementStatus.FINISHED && state.result != null && currentUserId > 0) {
            // TODO: добавить флаг, чтобы не сохранять дважды
            // saveMeasurementToDatabase(state)
        }
        
        // Отображение валидности теста
        if (!state.isValid && state.validationMessage != null) {
            binding.tvMeasurementStatus.text = "${statusText}\n⚠ ${state.validationMessage}"
            binding.tvMeasurementStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            // Возвращаем стандартный цвет текста
            binding.tvMeasurementStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }

        val stability = state.metrics.stability
        val frequency = state.metrics.oscillationFrequency
        val coordination = state.metrics.coordinationFactor

        binding.tvStability.text = getString(R.string.stability) + ": ${String.format(Locale.US, "%.2f", stability)}%"
        binding.tvOscillationFrequency.text =
            getString(R.string.oscillation_frequency) + ": ${String.format(Locale.US, "%.2f", frequency)} Hz"
        binding.tvCoordination.text = getString(
            R.string.coordination_factor_label,
            String.format(Locale.US, "%.2f", coordination)
        )

        updateTimeChart(binding.chartX, state.processedSamples.map { Entry(it.t.toFloat(), it.sxMm.toFloat()) }, android.graphics.Color.BLUE)
        updateTimeChart(binding.chartY, state.processedSamples.map { Entry(it.t.toFloat(), it.syMm.toFloat()) }, android.graphics.Color.RED)
        binding.targetTrajectory.setSamples(state.processedSamples)
    }

    private fun updateTimeChart(
        chart: com.github.mikephil.charting.charts.LineChart,
        entries: List<Entry>,
        color: Int
    ) {
        val durationFloat = measurementDurationSec.toFloat()
        if (entries.isEmpty()) {
            chart.clear()
        } else {
            val dataSet = LineDataSet(entries, "data").apply {
                this.color = color
                setDrawCircles(false)
                lineWidth = 2f
                setDrawValues(false)
            }
            chart.data = LineData(dataSet)
        }
        chart.axisLeft.axisMinimum = -chartRangeMm
        chart.axisLeft.axisMaximum = chartRangeMm
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = durationFloat
        chart.notifyDataSetChanged()
        chart.invalidate()
    }
    private fun clearCharts() {
        binding.chartX.clear()
        binding.chartY.clear()
        binding.chartX.invalidate()
        binding.chartY.invalidate()
        binding.targetTrajectory.clear()
    }
    
    private fun exportMeasurement() {
        val state = viewModel.measurementState.value
        val result = state.result ?: return
        
        // Показываем диалог выбора формата
        val formats = arrayOf(getString(R.string.export_csv), getString(R.string.export_pdf))
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.export))
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> exportToCsv(result)
                    1 -> exportToPdf(result)
                }
            }
            .show()
    }
    
    private fun exportToCsv(result: com.accelerometer.app.data.MeasurementResult) {
        try {
            val exportService = ExportService(this)
            val userName = "User" // TODO: получить из выбранного пользователя
            val file = exportService.exportToCsv(result, userName)
            if (file != null) {
                showExportSuccess(file, "CSV")
            } else {
                Toast.makeText(this, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.export_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportToPdf(result: com.accelerometer.app.data.MeasurementResult) {
        try {
            val exportService = PdfExportService(this)
            val userName = "User" // TODO: получить из выбранного пользователя
            val file = exportService.exportToPdf(result, userName)
            if (file != null) {
                showExportSuccess(file, "PDF")
            } else {
                Toast.makeText(this, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.export_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showExportSuccess(file: java.io.File, format: String) {
        val filePath = file.absolutePath
        val message = "Файл сохранён:\n$filePath\n\nНажмите OK, чтобы открыть файл"
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Экспорт $format завершён")
            .setMessage(message)
            .setPositiveButton("Открыть файл") { _, _ ->
                openFile(file)
            }
            .setNegativeButton("OK", null)
            .show()
    }
    
    private fun openFile(file: java.io.File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            } else {
                @Suppress("DEPRECATION")
                android.net.Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.extension))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Открыть файл"))
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть файл: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
    }
    
    private fun saveMeasurementToDatabase(state: MeasurementState) {
        val result = state.result ?: return
        lifecycleScope.launch {
            try {
                measurementRepository.saveMeasurement(
                    userId = currentUserId,
                    result = result,
                    isValid = state.isValid,
                    validationMessage = state.validationMessage
                )
            } catch (e: Exception) {
                // Ошибка сохранения - не критично, просто логируем
                android.util.Log.e("MainActivity", "Failed to save measurement", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        phoneSensorServer?.stop()
        bluetoothService.disconnect()
    }
}

