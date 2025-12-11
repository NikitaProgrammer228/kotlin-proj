package com.accelerometer.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accelerometer.app.R
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.database.FootSide
import com.accelerometer.app.database.Measurement
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.MeasurementStatus
import com.accelerometer.app.databinding.ActivityPktBinding
import com.accelerometer.app.export.ExportService
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.service.MeasurementRepository
import kotlinx.coroutines.launch

/**
 * Activity для проведения PKT-протокола (20 тестов: 10 на правую ногу, 10 на левую)
 */
class PktActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPktBinding
    private lateinit var bluetoothService: BluetoothAccelerometerService
    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var viewModel: MainViewModel
    
    private var userId: Long = 0
    private var currentTestNumber = 0
    private var currentFoot: FootSide = FootSide.RIGHT
    private val completedTests = mutableListOf<Measurement>()
    private var isTestRunning = false
    
    companion object {
        private const val TOTAL_TESTS = 20
        private const val TESTS_PER_FOOT = 10
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPktBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userId = intent.getLongExtra("userId", 0L)
        if (userId == 0L) {
            Toast.makeText(this, "Ошибка: не выбран пользователь", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        bluetoothService = BluetoothAccelerometerService(this)
        measurementRepository = MeasurementRepository(AppDatabase.getDatabase(this))
        viewModel = MainViewModel()
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        binding.btnStartTest.setOnClickListener {
            if (!isTestRunning) {
                startNextTest()
            }
        }
        
        binding.btnExportResults.setOnClickListener {
            exportPktResults()
        }
        
        updateTestInfo()
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            bluetoothService.connectionState.collect { state ->
                when (state) {
                    BluetoothAccelerometerService.ConnectionState.CONNECTED -> {
                        binding.tvConnectionStatus.text = "Подключено"
                        binding.btnStartTest.isEnabled = true
                    }
                    BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                        binding.tvConnectionStatus.text = "Отключено"
                        binding.btnStartTest.isEnabled = false
                    }
                    else -> {
                        binding.tvConnectionStatus.text = "Подключение..."
                        binding.btnStartTest.isEnabled = false
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            bluetoothService.sensorSamples.collect { sample ->
                if (isTestRunning) {
                    viewModel.onSensorSample(sample)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.measurementState.collect { state ->
                handleMeasurementState(state)
            }
        }
    }
    
    private fun handleMeasurementState(state: MeasurementState) {
        when (state.status) {
            MeasurementStatus.FINISHED -> {
                if (state.result != null) {
                    saveTestResult(state)
                }
            }
            MeasurementStatus.RUNNING -> {
                binding.tvTestStatus.text = "Тест в процессе..."
                binding.progressBar.progress = ((state.elapsedSec / MeasurementConfig.MEASUREMENT_DURATION_SEC) * 100).toInt()
            }
            else -> {
                binding.tvTestStatus.text = "Готов к тесту"
            }
        }
    }
    
    private fun startNextTest() {
        if (currentTestNumber >= TOTAL_TESTS) {
            Toast.makeText(this, "Все тесты завершены!", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentTestNumber++
        
        // Определяем ногу: первые 10 тестов - правая, следующие 10 - левая
        currentFoot = if (currentTestNumber <= TESTS_PER_FOOT) {
            FootSide.RIGHT
        } else {
            FootSide.LEFT
        }
        
        isTestRunning = true
        viewModel.startMeasurement(MeasurementConfig.MEASUREMENT_DURATION_SEC)
        updateTestInfo()
    }
    
    private fun saveTestResult(state: MeasurementState) {
        val result = state.result ?: return
        
        lifecycleScope.launch {
            try {
                val measurementId = measurementRepository.saveMeasurement(
                    userId = userId,
                    result = result,
                    isValid = state.isValid,
                    validationMessage = state.validationMessage,
                    foot = currentFoot,
                    testNumber = currentTestNumber
                )
                
                val measurement = measurementRepository.getMeasurementById(measurementId)
                if (measurement != null) {
                    completedTests.add(measurement)
                }
                
                isTestRunning = false
                updateTestInfo()
                
                if (currentTestNumber < TOTAL_TESTS) {
                    Toast.makeText(this@PktActivity, "Тест $currentTestNumber завершён. Готов к следующему.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PktActivity, "Все тесты завершены!", Toast.LENGTH_LONG).show()
                    binding.btnExportResults.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@PktActivity, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateTestInfo() {
        val footText = if (currentFoot == FootSide.RIGHT) "Правая нога" else "Левая нога"
        binding.tvTestInfo.text = "Тест $currentTestNumber из $TOTAL_TESTS\n$footText"
        binding.tvProgress.text = "Завершено: ${completedTests.size}/$TOTAL_TESTS"
    }
    
    private fun exportPktResults() {
        if (completedTests.isEmpty()) {
            Toast.makeText(this, "Нет завершённых тестов для экспорта", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val exportService = ExportService(this@PktActivity)
                
                // Экспорт для правой ноги
                val rightFootTests = completedTests.filter { it.foot == FootSide.RIGHT }
                if (rightFootTests.isNotEmpty()) {
                    val rightFootData = rightFootTests.map { measurement ->
                        "User" to com.accelerometer.app.data.MeasurementResult(
                            metrics = measurement.metrics,
                            durationSec = measurement.durationSec,
                            samples = measurement.samples
                        )
                    }
                    exportService.exportPktToCsv(rightFootData, "RIGHT")
                }
                
                // Экспорт для левой ноги
                val leftFootTests = completedTests.filter { it.foot == FootSide.LEFT }
                if (leftFootTests.isNotEmpty()) {
                    val leftFootData = leftFootTests.map { measurement ->
                        "User" to com.accelerometer.app.data.MeasurementResult(
                            metrics = measurement.metrics,
                            durationSec = measurement.durationSec,
                            samples = measurement.samples
                        )
                    }
                    exportService.exportPktToCsv(leftFootData, "LEFT")
                }
                
                Toast.makeText(this@PktActivity, "Результаты экспортированы", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@PktActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}

