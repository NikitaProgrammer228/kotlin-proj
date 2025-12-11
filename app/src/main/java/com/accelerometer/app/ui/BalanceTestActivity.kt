package com.accelerometer.app.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.accelerometer.app.R
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.MeasurementStatus
import com.accelerometer.app.databinding.ActivityBalanceTestBinding
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.export.ExportService
import com.accelerometer.app.export.PdfExportService
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.service.MeasurementRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Activity для отображения баланс-теста с графиками по времени
 * Показывает графики X и Y осей с цветными зонами (красный, желтый, зеленый)
 */
class BalanceTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBalanceTestBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bluetoothService: BluetoothAccelerometerService
    private lateinit var measurementRepository: MeasurementRepository

    private var currentUserId: Long = 0
    private var timeScale: Float = 1f
    private var currentViewMode: ViewMode = ViewMode.TIME
    
    // Настройки теста
    private var selectedPainLevel: Int = 9
    private var selectedTestDuration: Int = 10
    private var selectedDifficulty: String = "Простой"
    private var isAutostart: Boolean = true
    private var isTestRunning: Boolean = false
    
    // Диалог подготовки к тесту
    private var preparationDialog: Dialog? = null
    private var countdownTimer: CountDownTimer? = null
    private val PREPARATION_TIME_SEC = 10L // 10 секунд на подготовку

    enum class ViewMode {
        TIME, TARGET
    }

    companion object {
        private val SCALE_OPTIONS = listOf(1f, 2f, 4f, 8f)
        private val VIEW_OPTIONS = listOf("Время", "Мишень")
        private val PAIN_LEVELS = (0..10).toList()
        private val TEST_DURATIONS = listOf(10, 20, 30)
        private val DIFFICULTIES = listOf("Простой", "Средний", "Сложный")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBalanceTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getLongExtra("userId", 0L)

        bluetoothService = BluetoothAccelerometerService(this)
        measurementRepository = MeasurementRepository(AppDatabase.getDatabase(this))

        setupSpinners()
        // Устанавливаем начальное состояние на основе реального состояния подключения
        val initialConnectionState = bluetoothService.connectionState.value
        updateSensorConnectionUI(
            initialConnectionState == BluetoothAccelerometerService.ConnectionState.CONNECTED
        )
        setupTestSettingsSpinners()
        setupClickListeners()
        setupObservers()
        
        // Показываем начальное состояние (настройка теста)
        showTestSetupUI()
    }

    private fun setupSpinners() {
        // Spinner для масштаба графика
        val scaleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            SCALE_OPTIONS.map { "x${it.toInt()}" }
        )
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.scaleSpinner.adapter = scaleAdapter
        binding.scaleSpinner.setSelection(0)

        binding.scaleSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    timeScale = SCALE_OPTIONS[position]
                    binding.xAxisGraph.setTimeScale(timeScale)
                    binding.yAxisGraph.setTimeScale(timeScale)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Spinner для вида графика
        val viewAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            VIEW_OPTIONS
        )
        viewAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.viewSpinner.adapter = viewAdapter
        binding.viewSpinner.setSelection(0)

        binding.viewSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 1) {
                        // Переключение на экран с мишенью
                        val intent = android.content.Intent(this@BalanceTestActivity, BalanceTestTargetActivity::class.java)
                        intent.putExtra("userId", currentUserId)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    private fun setupTestSettingsSpinners() {
        // Spinner для уровня болевого синдрома
        val painAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            PAIN_LEVELS.map { it.toString() }
        )
        painAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.painLevelSpinner.adapter = painAdapter
        binding.painLevelSpinner.setSelection(9) // По умолчанию 9

        binding.painLevelSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    selectedPainLevel = PAIN_LEVELS[position]
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Spinner для времени теста
        val durationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TEST_DURATIONS.map { it.toString() }
        )
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.testDurationSpinner.adapter = durationAdapter
        binding.testDurationSpinner.setSelection(0) // По умолчанию 10 сек

        binding.testDurationSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    selectedTestDuration = TEST_DURATIONS[position]
                    binding.testDurationDisplay.text = selectedTestDuration.toString()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Spinner для уровня сложности
        val difficultyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DIFFICULTIES
        )
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.difficultySpinner.adapter = difficultyAdapter
        binding.difficultySpinner.setSelection(0) // По умолчанию "Простой"

        binding.difficultySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    selectedDifficulty = DIFFICULTIES[position]
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Переключатель автостарта
        binding.autostartSwitch.isChecked = isAutostart
        binding.autostartSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAutostart = isChecked
        }
    }

    private fun showTestSetupUI() {
        binding.testSetupCard.visibility = android.view.View.VISIBLE
        binding.testSettingsCard.visibility = android.view.View.VISIBLE
        binding.testResultsCard.visibility = android.view.View.GONE
        binding.painLevelResultCard.visibility = android.view.View.GONE
        binding.saveToDatabaseButton.visibility = android.view.View.GONE
        binding.exportReportButton.visibility = android.view.View.GONE
        binding.newTestButton.visibility = android.view.View.GONE
    }

    private fun showTestRunningUI() {
        binding.testSetupCard.visibility = android.view.View.VISIBLE
        binding.testSettingsCard.visibility = android.view.View.GONE
        binding.testResultsCard.visibility = android.view.View.GONE
        binding.painLevelResultCard.visibility = android.view.View.GONE
        binding.saveToDatabaseButton.visibility = android.view.View.GONE
        binding.exportReportButton.visibility = android.view.View.GONE
        binding.newTestButton.visibility = android.view.View.GONE
        binding.startButton.isEnabled = false
        binding.startButton.text = "ИДЁТ ТЕСТ..."
    }

    private fun showTestResultsUI() {
        binding.testSetupCard.visibility = android.view.View.GONE
        binding.testSettingsCard.visibility = android.view.View.GONE
        binding.testResultsCard.visibility = android.view.View.VISIBLE
        binding.painLevelResultCard.visibility = android.view.View.VISIBLE
        binding.saveToDatabaseButton.visibility = android.view.View.VISIBLE
        binding.exportReportButton.visibility = android.view.View.VISIBLE
        binding.newTestButton.visibility = android.view.View.VISIBLE
        binding.startButton.isEnabled = true
        binding.startButton.text = "СТАРТ"
        
        // Обновляем уровень болевого синдрома и сложности в карточке результатов
        binding.painLevelTextView.text = selectedPainLevel.toString()
        binding.difficultyLevelTextView.text = selectedDifficulty
    }

    private fun setupClickListeners() {
        // Кнопка СТАРТ
        binding.startButton.setOnClickListener {
            startTest()
        }

        binding.saveToDatabaseButton.setOnClickListener {
            saveMeasurementToDatabase()
        }

        binding.exportReportButton.setOnClickListener {
            exportToPdf()
        }

        binding.newTestButton.setOnClickListener {
            startNewTest()
        }

        binding.fullscreenButton.setOnClickListener {
            // TODO: Реализовать полноэкранный режим
            Toast.makeText(this, "Полноэкранный режим в разработке", Toast.LENGTH_SHORT).show()
        }

        // Обработчик клика на кнопку подключения сенсора
        binding.powerButtonRing.setOnClickListener {
            if (bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED) {
                // Если подключено, отключаем
                bluetoothService.disconnect()
            } else {
                // Если не подключено, показываем модальное окно выбора сенсора
                showSensorSelectionDialog()
            }
        }
    }

    private fun startTest() {
        if (bluetoothService.connectionState.value != BluetoothAccelerometerService.ConnectionState.CONNECTED) {
            Toast.makeText(this, "Сначала подключите устройство", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Для базового баланс-теста запускаем сразу, без окна подготовки (диалог нужен только для PKT)
        actuallyStartTest()
    }
    
    private fun showPreparationDialog() {
        preparationDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_test_preparation)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            
            val timerText = findViewById<TextView>(R.id.timerSeconds)
            val circularTimer = findViewById<CircularTimerView>(R.id.circularTimer)
            val pauseButton = findViewById<MaterialButton>(R.id.pauseButton)
            
            pauseButton.setOnClickListener {
                cancelPreparation()
            }
            
            show()
            
            // Запускаем обратный отсчёт
            countdownTimer = object : CountDownTimer(PREPARATION_TIME_SEC * 1000, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                    timerText.text = secondsLeft.toString()
                    
                    // Обновляем прогресс круга (от 1.0 до 0.0)
                    val progress = millisUntilFinished.toFloat() / (PREPARATION_TIME_SEC * 1000)
                    circularTimer.progress = progress
                }
                
                override fun onFinish() {
                    timerText.text = "0"
                    circularTimer.progress = 0f
                    dismiss()
                    preparationDialog = null
                    
                    // Запускаем тест
                    actuallyStartTest()
                }
            }.start()
        }
    }
    
    private fun cancelPreparation() {
        countdownTimer?.cancel()
        countdownTimer = null
        preparationDialog?.dismiss()
        preparationDialog = null
    }
    
    private fun actuallyStartTest() {
        isTestRunning = true
        showTestRunningUI()
        
        // Устанавливаем длительность теста для графиков (чтобы линия росла постепенно)
        binding.xAxisGraph.setTestDuration(selectedTestDuration.toFloat())
        binding.yAxisGraph.setTestDuration(selectedTestDuration.toFloat())
        
        viewModel.startMeasurement(selectedTestDuration.toDouble())
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bluetoothService.connectionState.collect { state ->
                        updateConnectionState(state)
                    }
                }
                launch {
                    bluetoothService.sensorSamples.collect { sample ->
                        viewModel.onSensorSample(sample)
                    }
                }
                launch {
                    viewModel.measurementState.collect { state ->
                        renderMeasurementState(state)
                    }
                }
                launch {
                    bluetoothService.batteryLevel.collect { level ->
                        updateBatteryUI(level)
                    }
                }
            }
        }
    }

    private fun updateBatteryUI(level: Int) {
        binding.batteryProgress.progress = level
        binding.batteryPercent.text = "$level%"
    }

    private fun updateConnectionState(state: BluetoothAccelerometerService.ConnectionState) {
        when (state) {
            BluetoothAccelerometerService.ConnectionState.CONNECTED -> {
                updateSensorConnectionUI(true, getString(R.string.device_connected))
                // Не запускаем автоматически — ждём нажатия кнопки СТАРТ
                // Но если включён автостарт, запускаем
                if (isAutostart && !isTestRunning) {
                    startTest()
                }
            }
            BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                updateSensorConnectionUI(false, getString(R.string.device_disconnected))
                viewModel.stopMeasurement()
                isTestRunning = false
                clearGraphs()
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTING -> {
                updateSensorConnectionUI(false, getString(R.string.scanning))
            }
        }
    }

    private fun renderMeasurementState(state: MeasurementState) {
        // Обновляем метрики (формат: значение справа, жирным)
        val stability = state.metrics.stability
        val frequency = state.metrics.oscillationFrequency
        val coordination = state.metrics.coordinationFactor

        binding.stabilizationTextView.text = String.format(Locale.US, "%.0f%%", stability)
        binding.oscillationFrequencyTextView.text = String.format(Locale.US, "%.1f Гц", frequency)
        binding.coordinationFactorTextView.text = String.format(Locale.US, "%.0f", coordination)
        
        // Обновляем имя пользователя в правой панели (разделяем на две строки)
        val fullName = "Сергеев Антон Геннадьевич" // TODO: получить из базы данных
        val nameParts = fullName.split(" ", limit = 2)
        if (nameParts.size >= 2) {
            binding.userNameRight.text = nameParts[0] // "Сергеев"
            binding.userNameRightFull.text = nameParts[1] // "Антон Геннадьевич"
        } else {
            binding.userNameRight.text = fullName
            binding.userNameRightFull.text = ""
        }

        // Обновляем графики
        if (state.processedSamples.isNotEmpty()) {
            val xData = state.processedSamples.map { it.sxMm.toFloat() }
            val yData = state.processedSamples.map { it.syMm.toFloat() }
            val times = state.processedSamples.map { it.t.toFloat() } // Временные метки

            // Диапазон графика ±40 мм (как в MicroSwing)
            // Зеленая зона: ±10 мм, Желтая: ±20 мм, Красная: ±40 мм
            val range = MeasurementConfig.MOTION_POSITION_LIMIT_MM.toFloat()
            binding.xAxisGraph.setData(xData, times, -range, range)
            binding.yAxisGraph.setData(yData, times, -range, range)
        }

        // Обновляем статус теста и переключаем UI
        when (state.status) {
            MeasurementStatus.FINISHED -> {
                isTestRunning = false
                showTestResultsUI()
                binding.saveToDatabaseButton.isEnabled = true
                binding.exportReportButton.isEnabled = true
                binding.newTestButton.isEnabled = true
            }
            MeasurementStatus.CALIBRATING -> {
                showTestRunningUI()
                binding.testDurationDisplay.text = selectedTestDuration.toString()
                binding.startButton.text = "КАЛИБРОВКА..."
            }
            MeasurementStatus.RUNNING -> {
                showTestRunningUI()
                // Обновляем отображение оставшегося времени
                val remainingSec = (selectedTestDuration - state.elapsedSec).toInt().coerceAtLeast(0)
                binding.testDurationDisplay.text = remainingSec.toString()
                binding.startButton.text = "ИДЁТ ТЕСТ..."
            }
            else -> {
                // IDLE или CALIBRATING — не меняем UI
            }
        }

        // Индикатор времени (2 сек) теперь статическая иконка
        // Текст "2 сек" встроен в векторный drawable ic_arrow_2sec

        // Обновляем состояние подключения сенсора на основе реального состояния
        val isConnected = bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED
        updateSensorConnectionUI(isConnected)
    }

    private fun updateSensorConnectionUI(connected: Boolean, statusTextOverride: String? = null) {
        val powerColor = if (connected) Color.parseColor("#38B36F") else Color.parseColor("#B7B7B7")
        val statusText = if (connected) "Устройство подключено" else "Устройство не подключено"
        val statusTextColor = if (connected) Color.parseColor("#38B36F") else Color.parseColor("#9EA5B2")

        binding.powerButtonRing.imageTintList = ColorStateList.valueOf(powerColor)
        binding.powerButtonSymbol.imageTintList = ColorStateList.valueOf(powerColor)
        binding.connectionStatusText.text = statusTextOverride ?: statusText
        binding.connectionStatusText.setTextColor(statusTextColor)
    }

    private fun clearGraphs() {
        val range = MeasurementConfig.MOTION_POSITION_LIMIT_MM.toFloat()
        // Устанавливаем длительность по умолчанию
        binding.xAxisGraph.setTestDuration(selectedTestDuration.toFloat())
        binding.yAxisGraph.setTestDuration(selectedTestDuration.toFloat())
        binding.xAxisGraph.setData(emptyList(), emptyList(), -range, range)
        binding.yAxisGraph.setData(emptyList(), emptyList(), -range, range)
    }

    private fun saveMeasurementToDatabase() {
        val state = viewModel.measurementState.value
        val result = state.result ?: run {
            Toast.makeText(this, "Нет данных для сохранения", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == 0L) {
            Toast.makeText(this, "Пользователь не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                measurementRepository.saveMeasurement(
                    userId = currentUserId,
                    result = result,
                    isValid = state.isValid,
                    validationMessage = state.validationMessage
                )
                Toast.makeText(this@BalanceTestActivity, "Данные сохранены в базу", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@BalanceTestActivity,
                    "Ошибка сохранения: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportToPdf() {
        val state = viewModel.measurementState.value
        val result = state.result ?: run {
            Toast.makeText(this, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val exportService = PdfExportService(this@BalanceTestActivity)
                val userName = "User" // TODO: получить из выбранного пользователя
                val file = exportService.exportToPdf(result, userName)
                if (file != null) {
                    Toast.makeText(
                        this@BalanceTestActivity,
                        "Отчет экспортирован: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@BalanceTestActivity, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BalanceTestActivity,
                    "Ошибка экспорта: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startNewTest() {
        viewModel.stopMeasurement()
        clearGraphs()
        isTestRunning = false
        showTestSetupUI()
        // Сбрасываем отображение времени
        binding.testDurationDisplay.text = selectedTestDuration.toString()
    }

    override fun onResume() {
        super.onResume()
        // Не запускаем автоматический поиск - ждём нажатия на кнопку подключения
    }

    private fun showSensorSelectionDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_sensor_selection)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                (resources.displayMetrics.heightPixels * 0.6).toInt()
            )
            setCancelable(true)
        }

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.sensorsRecyclerView)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.scanningProgress)
        val adapter = SensorAdapter { device ->
            // Подключаемся к выбранному устройству
            bluetoothService.connectToDevice(device)
            dialog.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Показываем прогресс
        progressBar.visibility = android.view.View.VISIBLE
        recyclerView.visibility = android.view.View.GONE

        // Начинаем поиск устройств
        bluetoothService.startDiscoveryForSelection()

        // Обновляем список каждые 500мс
        val handler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                val devices = bluetoothService.getDiscoveredDevices()
                if (devices.isNotEmpty()) {
                    progressBar.visibility = android.view.View.GONE
                    recyclerView.visibility = android.view.View.VISIBLE
                }
                adapter.updateSensors(devices)
                
                // Продолжаем обновление пока диалог открыт
                if (dialog.isShowing) {
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(updateRunnable, 500)

        dialog.setOnDismissListener {
            handler.removeCallbacks(updateRunnable)
            bluetoothService.stopDiscovery()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPreparation()
        bluetoothService.disconnect()
    }
}
