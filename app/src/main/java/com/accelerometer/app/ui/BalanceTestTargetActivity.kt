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
    private var isAutostart: Boolean = true
    private var isTestRunning: Boolean = false
    
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
        
        binding.painLevelTextView.text = selectedPainLevel.toString()
        binding.difficultyLevelTextView.text = selectedDifficulty
    }

    private fun setupClickListeners() {
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
                if (isAutostart && !isTestRunning) {
                    startTest()
                }
            }
            BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                updateSensorConnectionUI(false, getString(R.string.device_disconnected))
                viewModel.stopMeasurement()
                isTestRunning = false
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
                val remainingSec = (selectedTestDuration - state.elapsedSec).toInt().coerceAtLeast(0)
                binding.testDurationDisplay.text = remainingSec.toString()
                binding.startButton.text = "ИДЁТ ТЕСТ..."
            }
            else -> {}
        }

        // Обновляем состояние подключения сенсора на основе реального состояния
        val isConnected = bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED
        updateSensorConnectionUI(isConnected)
    }

    /**
     * Считаем проценты по квадрантам и обновляем подписи.
     * Основание — длина пройденного пути (сегменты), чтобы ближе совпасть
     * с немецким ПО. Если движения нет, падаем обратно на подсчёт по точкам.
     * Знаки — как на графике: X инвертируем, Y оставляем вверх.
     */
    private fun updateCornerPercentsFromSamples(samples: List<com.accelerometer.app.data.ProcessedSample>) {
        if (samples.size < 2) {
            setDefaultCornerPercents()
            return
        }

        var topLeftLen = 0.0
        var topRightLen = 0.0
        var bottomLeftLen = 0.0
        var bottomRightLen = 0.0

        // Интегрируем длину пути по сегментам, относя сегмент к квадранту конечной точки
        for (i in 1 until samples.size) {
            val prev = samples[i - 1]
            val curr = samples[i]

            val prevX = -prev.sxMm
            val prevY = prev.syMm
            val currX = -curr.sxMm
            val currY = curr.syMm

            val segLen = hypot(currX - prevX, currY - prevY)
            if (segLen == 0.0) continue

            when {
                currY >= 0 && currX < 0 -> topLeftLen += segLen
                currY >= 0 && currX >= 0 -> topRightLen += segLen
                currY < 0 && currX < 0 -> bottomLeftLen += segLen
                else -> bottomRightLen += segLen
            }
        }

        var totalLen = topLeftLen + topRightLen + bottomLeftLen + bottomRightLen

        // Если путь нулевой (почти статично), fallback на подсчёт по точкам
        if (totalLen == 0.0) {
            var tl = 0; var tr = 0; var bl = 0; var br = 0
            samples.forEach { s ->
                val xScreen = -s.sxMm
                val yScreen = s.syMm
                when {
                    yScreen >= 0 && xScreen < 0 -> tl++
                    yScreen >= 0 && xScreen >= 0 -> tr++
                    yScreen < 0 && xScreen < 0 -> bl++
                    else -> br++
                }
            }
            val total = samples.size
            val percents = listOf(tl, tr, bl, br)
                .map { (it * 100.0 / total).roundToInt() }
                .toMutableList()
            val diff = 100 - percents.sum()
            if (diff != 0) {
                val maxIdx = listOf(tl, tr, bl, br).withIndex().maxByOrNull { it.value }?.index ?: 0
                percents[maxIdx] = (percents[maxIdx] + diff).coerceAtLeast(0)
            }
            updateCornerPercents("${percents[0]}%", "${percents[1]}%", "${percents[2]}%", "${percents[3]}%")
            return
        }

        val percents = listOf(
            (topLeftLen * 100.0 / totalLen).roundToInt(),
            (topRightLen * 100.0 / totalLen).roundToInt(),
            (bottomLeftLen * 100.0 / totalLen).roundToInt(),
            (bottomRightLen * 100.0 / totalLen).roundToInt()
        ).toMutableList()

        // Подгоняем сумму к 100%
        val diff = 100 - percents.sum()
        if (diff != 0) {
            val lens = listOf(topLeftLen, topRightLen, bottomLeftLen, bottomRightLen)
            val maxIdx = lens.withIndex().maxByOrNull { it.value }?.index ?: 0
            percents[maxIdx] = (percents[maxIdx] + diff).coerceAtLeast(0)
        }

        updateCornerPercents(
            "${percents[0]}%",
            "${percents[1]}%",
            "${percents[2]}%",
            "${percents[3]}%"
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
