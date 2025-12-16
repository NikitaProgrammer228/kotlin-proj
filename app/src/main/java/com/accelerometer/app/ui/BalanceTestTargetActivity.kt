package com.accelerometer.app.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
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
import com.accelerometer.app.databinding.ActivityBalanceTestTargetBinding
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.export.PdfExportService
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.service.MeasurementRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.hypot
import java.util.Locale

/**
 * Activity для отображения баланс-теста с графиком "Мишень"
 * Показывает круговую мишень с концентрическими кольцами и траекторией движения
 */
class BalanceTestTargetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBalanceTestTargetBinding
    private val viewModel: TargetViewModel by viewModels()
    private lateinit var bluetoothService: BluetoothAccelerometerService
    private lateinit var measurementRepository: MeasurementRepository

    private var currentUserId: Long = 0
    private var graphScale: Float = 1f
    
    // Настройки теста
    private var selectedPainLevel: Int = 9
    private var selectedTestDuration: Int = 10
    private var selectedDifficulty: String = "Простой"
    private var isAutostart: Boolean = false  // По умолчанию выключен
    private var isTestRunning: Boolean = false
    private var isTestPaused: Boolean = false  // Флаг паузы теста
    
    // Для автостарта по порогу движения
    private var baseAngleX: Double? = null
    private var baseAngleY: Double? = null
    private var autostartSampleCount: Int = 0
    private val AUTOSTART_SAMPLES_FOR_BASE = 10  // Сколько сэмплов для определения базовой позиции
    private var isAutostartArmed: Boolean = false  // Флаг "взведённости" автостарта
    
    // Диалог подготовки к тесту
    private var preparationDialog: Dialog? = null
    private var countdownTimer: CountDownTimer? = null
    private val PREPARATION_TIME_SEC = 10L

    companion object {
        private val SCALE_OPTIONS = listOf(1f, 2f, 4f, 8f)
        private val VIEW_OPTIONS = listOf("Время", "Мишень")
        private val PAIN_LEVELS = (0..10).toList()
        private val TEST_DURATIONS = listOf(10, 20, 30)
        private val DIFFICULTIES = listOf("Простой", "Средний", "Сложный")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBalanceTestTargetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getLongExtra("userId", 0L)

        bluetoothService = BluetoothAccelerometerService(this)
        measurementRepository = MeasurementRepository(AppDatabase.getDatabase(this))

        setupSpinners()
        setupTestSettingsSpinners()
        setupClickListeners()
        setupObservers()
        // Устанавливаем начальное состояние на основе реального состояния подключения
        val initialConnectionState = bluetoothService.connectionState.value
        updateSensorConnectionUI(
            initialConnectionState == BluetoothAccelerometerService.ConnectionState.CONNECTED
        )
        
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
                    graphScale = SCALE_OPTIONS[position]
                    binding.targetGraph.setScale(graphScale)
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
        binding.viewSpinner.setSelection(1) // По умолчанию "Мишень"

        binding.viewSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 0) {
                        // Переключение на экран с графиками по времени
                        val intent = android.content.Intent(this@BalanceTestTargetActivity, BalanceTestActivity::class.java)
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
        binding.painLevelSpinner.setSelection(9)

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
        binding.testDurationSpinner.setSelection(0)

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
        binding.difficultySpinner.setSelection(0)

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
            if (isChecked) {
                // При включении автостарта - взводим его (если сенсор уже подключен)
                armAutostart()
            } else {
                // При выключении - разряжаем
                isAutostartArmed = false
            }
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
        // Кнопка будет обновлена в renderMeasurementState
        binding.startButton.isEnabled = true
        binding.startButton.text = "ПАУЗА"
        binding.startButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#6DC188")
        )
    }

    private fun showTestResultsUI() {
        binding.testSetupCard.visibility = android.view.View.GONE
        binding.testSettingsCard.visibility = android.view.View.GONE
        binding.testResultsCard.visibility = android.view.View.VISIBLE
        binding.startButton.text = "СТАРТ"
        binding.startButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#DC433C")
        )
        binding.painLevelResultCard.visibility = android.view.View.VISIBLE
        binding.saveToDatabaseButton.visibility = android.view.View.VISIBLE
        binding.exportReportButton.visibility = android.view.View.VISIBLE
        binding.newTestButton.visibility = android.view.View.VISIBLE
        binding.startButton.isEnabled = true
        binding.startButton.text = "СТАРТ"
        
        binding.painLevelTextView.text = selectedPainLevel.toString()
        binding.difficultyLevelTextView.text = selectedDifficulty
    }

    private fun setupClickListeners() {
        // Кнопка СТАРТ / ПАУЗА
        binding.startButton.setOnClickListener {
            if (isTestRunning && !isTestPaused) {
                // Тест идёт — ставим на паузу
                pauseTest()
            } else {
                // Тест не идёт или на паузе — начинаем новый тест
                startTest()
            }
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
        
        // Если был на паузе — сбрасываем флаг и очищаем график
        if (isTestPaused) {
            isTestPaused = false
            clearGraph()
        }
        
        // Для базового баланс-теста запускаем сразу, без окна подготовки (диалог нужен только для PKT)
        actuallyStartTest()
    }
    
    private fun pauseTest() {
        isTestPaused = true
        isTestRunning = false
        viewModel.stopMeasurement()
        
        // Обновляем UI кнопки — показываем СТАРТ (красная)
        binding.startButton.text = "СТАРТ"
        binding.startButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#DC433C")
        )
        binding.startButton.isEnabled = true
        
        // График остаётся на экране (не очищаем)
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
            
            countdownTimer = object : CountDownTimer(PREPARATION_TIME_SEC * 1000, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                    timerText.text = secondsLeft.toString()
                    val progress = millisUntilFinished.toFloat() / (PREPARATION_TIME_SEC * 1000)
                    circularTimer.progress = progress
                }
                
                override fun onFinish() {
                    timerText.text = "0"
                    circularTimer.progress = 0f
                    dismiss()
                    preparationDialog = null
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
                        // Передаём данные в ViewModel (обрабатываются только когда тест запущен)
                        viewModel.onSensorSample(sample)
                        
                        // Проверка автостарта по порогу движения
                        checkAutostartThreshold(sample)
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
    
    /**
     * Проверка автостарта по порогу движения.
     * Если автостарт включен, взведён и движение превышает порог - запускаем тест.
     */
    private fun checkAutostartThreshold(sample: com.accelerometer.app.data.SensorSample) {
        // Пропускаем если автостарт выключен, не взведён или тест уже запущен
        if (!isAutostart || !isAutostartArmed || isTestRunning) return
        
        // Первые N сэмплов используем для определения базовой позиции
        if (autostartSampleCount < AUTOSTART_SAMPLES_FOR_BASE) {
            if (baseAngleX == null) {
                baseAngleX = sample.angleXDeg
                baseAngleY = sample.angleYDeg
            } else {
                // Усредняем базовую позицию
                baseAngleX = baseAngleX!! * 0.9 + sample.angleXDeg * 0.1
                baseAngleY = baseAngleY!! * 0.9 + sample.angleYDeg * 0.1
            }
            autostartSampleCount++
            return
        }
        
        // Проверяем отклонение от базовой позиции
        val deltaX = sample.angleXDeg - (baseAngleX ?: 0.0)
        val deltaY = sample.angleYDeg - (baseAngleY ?: 0.0)
        
        // Конвертируем углы в мм (как в MeasurementProcessor)
        val posXMm = deltaX * MeasurementConfig.ANGLE_TO_MM_SCALE_X
        val posYMm = deltaY * MeasurementConfig.ANGLE_TO_MM_SCALE_Y
        
        // Проверяем превышение порога
        val amplitude = kotlin.math.sqrt(posXMm * posXMm + posYMm * posYMm)
        if (amplitude > MeasurementConfig.AUTOSTART_THRESHOLD_MM) {
            // Порог превышен - снимаем "взведённость" и запускаем тест
            isAutostartArmed = false
            startTest()
        }
    }
    
    /**
     * "Взведение" автостарта - подготовка к автоматическому запуску теста.
     * Вызывается при подключении сенсора и при нажатии "Новый тест".
     */
    private fun armAutostart() {
        if (isAutostart) {
            isAutostartArmed = true
            resetAutostartBase()
        }
    }
    
    /**
     * Сброс базовой позиции для автостарта (без изменения флага взведённости)
     */
    private fun resetAutostartBase() {
        baseAngleX = null
        baseAngleY = null
        autostartSampleCount = 0
    }
    
    /**
     * Полный сброс состояния автостарта (при отключении сенсора)
     */
    private fun resetAutostartState() {
        baseAngleX = null
        baseAngleY = null
        autostartSampleCount = 0
        isAutostartArmed = false
    }

    private fun updateBatteryUI(level: Int) {
        binding.batteryProgress.progress = level
        binding.batteryPercent.text = "$level%"
    }

    /** Обновление подписей углов процентами (значения можно менять динамически). */
    private fun updateCornerPercents(
        topLeft: String,
        topRight: String,
        bottomLeft: String,
        bottomRight: String
    ) {
        binding.cornerPercentTopLeft.text = topLeft
        binding.cornerPercentTopRight.text = topRight
        binding.cornerPercentBottomLeft.text = bottomLeft
        binding.cornerPercentBottomRight.text = bottomRight
    }

    private fun setDefaultCornerPercents() {
        updateCornerPercents("25%", "25%", "25%", "25%")
    }

    private fun updateConnectionState(state: BluetoothAccelerometerService.ConnectionState) {
        when (state) {
            BluetoothAccelerometerService.ConnectionState.CONNECTED -> {
                updateSensorConnectionUI(true, getString(R.string.device_connected))
                // "Взводим" автостарт при подключении сенсора
                // Тест НЕ запускается сразу - только по кнопке "Старт" или по порогу движения
                armAutostart()
            }
            BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                updateSensorConnectionUI(false, getString(R.string.device_disconnected))
                viewModel.stopMeasurement()
                isTestRunning = false
                isTestPaused = false
                resetAutostartState()
                clearGraph()
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTING -> {
                updateSensorConnectionUI(false, getString(R.string.scanning))
            }
        }
    }

    private fun renderMeasurementState(state: MeasurementState) {
        val stability = state.metrics.stability
        val frequency = state.metrics.oscillationFrequency
        val coordination = state.metrics.coordinationFactor

        binding.stabilizationTextView.text = String.format(Locale.US, "%.0f%%", stability)
        binding.oscillationFrequencyTextView.text = String.format(Locale.US, "%.1f Гц", frequency)
        binding.coordinationFactorTextView.text = String.format(Locale.US, "%.0f", coordination)
        
        val fullName = "Сергеев Антон Геннадьевич"
        val nameParts = fullName.split(" ", limit = 2)
        if (nameParts.size >= 2) {
            binding.userNameRight.text = nameParts[0]
            binding.userNameRightFull.text = nameParts[1]
        } else {
            binding.userNameRight.text = fullName
            binding.userNameRightFull.text = ""
        }

        if (state.processedSamples.isNotEmpty()) {
            binding.targetGraph.setSamples(state.processedSamples)
            updateCornerPercentsFromSamples(state.processedSamples)
        } else {
            setDefaultCornerPercents()
        }

        when (state.status) {
            MeasurementStatus.FINISHED -> {
                isTestRunning = false
                isTestPaused = false
                // Автостарт уже "разряжен" (isAutostartArmed = false) после запуска теста,
                // поэтому повторно не сработает. Для нового теста нужно нажать "Новый тест".
                showTestResultsUI()
                binding.saveToDatabaseButton.isEnabled = true
                binding.exportReportButton.isEnabled = true
                binding.newTestButton.isEnabled = true
            }
            MeasurementStatus.CALIBRATING -> {
                showTestRunningUI()
                binding.testDurationDisplay.text = selectedTestDuration.toString()
                binding.startButton.text = "КАЛИБРОВКА..."
                binding.startButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFA500")  // Оранжевый для калибровки
                )
                binding.startButton.isEnabled = false
            }
            MeasurementStatus.RUNNING -> {
                showTestRunningUI()
                val remainingSec = (selectedTestDuration - state.elapsedSec).toInt().coerceAtLeast(0)
                binding.testDurationDisplay.text = remainingSec.toString()
                // Кнопка ПАУЗА (зелёная)
                binding.startButton.text = "ПАУЗА"
                binding.startButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#6DC188")
                )
                binding.startButton.isEnabled = true
            }
            else -> {}
        }

        // Обновляем состояние подключения сенсора на основе реального состояния
        val isConnected = bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED
        updateSensorConnectionUI(isConnected)
    }

    /**
     * Расчёт процентов квадрантов по ВРЕМЕНИ (количеству кадров) согласно ТЗ.
     * Считаем сколько кадров (= времени при 50 Гц) траектория была в каждом квадранте.
     * Сумма всех процентов = 100%.
     * Знаки — как на графике: X и Y инвертируем (centerX - sxMm, centerY - syMm).
     */
    private fun updateCornerPercentsFromSamples(samples: List<com.accelerometer.app.data.ProcessedSample>) {
        if (samples.isEmpty()) {
            setDefaultCornerPercents()
            return
        }

        // Считаем количество кадров в каждом квадранте
        // Экранные координаты: screenX = centerX - sxMm, screenY = centerY - syMm
        // Поэтому: screenX < center когда sxMm > 0, screenY < center когда syMm > 0
        var topLeft = 0     // верх-лево  (screenX < center, screenY < center) = (sxMm > 0, syMm > 0)
        var topRight = 0    // верх-право (screenX >= center, screenY < center) = (sxMm <= 0, syMm > 0)
        var bottomLeft = 0  // низ-лево   (screenX < center, screenY >= center) = (sxMm > 0, syMm <= 0)
        var bottomRight = 0 // низ-право  (screenX >= center, screenY >= center) = (sxMm <= 0, syMm <= 0)

        samples.forEach { s ->
            // Используем знаки данных напрямую (без дополнительной инверсии),
            // так как экранная система уже инвертирует их при отрисовке
            val x = s.sxMm
            val y = s.syMm

            when {
                y > 0 && x > 0 -> topLeft++
                y > 0 && x <= 0 -> topRight++
                y <= 0 && x > 0 -> bottomLeft++
                y <= 0 && x <= 0 -> bottomRight++
            }
        }

        val total = samples.size
        
        // Вычисляем проценты
        val percents = mutableListOf(
            (topLeft * 100.0 / total).roundToInt(),
            (topRight * 100.0 / total).roundToInt(),
            (bottomLeft * 100.0 / total).roundToInt(),
            (bottomRight * 100.0 / total).roundToInt()
        )

        // Подгоняем сумму к 100% (корректируем наибольший квадрант)
        val diff = 100 - percents.sum()
        if (diff != 0) {
            val counts = listOf(topLeft, topRight, bottomLeft, bottomRight)
            val maxIdx = counts.withIndex().maxByOrNull { it.value }?.index ?: 0
            percents[maxIdx] = (percents[maxIdx] + diff).coerceAtLeast(0)
        }

        updateCornerPercents(
            "${percents[0]}%",  // topLeft
            "${percents[1]}%",  // topRight
            "${percents[2]}%",  // bottomLeft
            "${percents[3]}%"   // bottomRight
        )
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

    private fun clearGraph() {
        binding.targetGraph.clear()
        setDefaultCornerPercents()
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
                Toast.makeText(this@BalanceTestTargetActivity, "Данные сохранены в базу", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@BalanceTestTargetActivity,
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
                val exportService = PdfExportService(this@BalanceTestTargetActivity)
                val userName = "User"
                val file = exportService.exportToPdf(result, userName)
                if (file != null) {
                    Toast.makeText(
                        this@BalanceTestTargetActivity,
                        "Отчет экспортирован: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@BalanceTestTargetActivity, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BalanceTestTargetActivity,
                    "Ошибка экспорта: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startNewTest() {
        viewModel.stopMeasurement()
        clearGraph()
        isTestRunning = false
        showTestSetupUI()
        binding.testDurationDisplay.text = selectedTestDuration.toString()
        // "Взводим" автостарт для нового теста
        armAutostart()
    }

    private fun showSensorSelectionDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_sensor_selection)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.CENTER)
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

    override fun onResume() {
        super.onResume()
        // Не запускаем автоматический поиск - ждём нажатия на кнопку подключения
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPreparation()
        bluetoothService.disconnect()
    }
}
