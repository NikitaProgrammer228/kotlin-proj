package com.accelerometer.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.MeasurementStatus
import com.accelerometer.app.databinding.ActivityMainBinding
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
    private val measurementDurationSec = MeasurementConfig.MEASUREMENT_DURATION_SEC
    private val chartRangeMm = MeasurementConfig.CHART_AXIS_RANGE_MM.toFloat()
    
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
        
        setupCharts()
        setupObservers()
        setupClickListeners()
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
            if (bluetoothService.connectionState.value == BluetoothAccelerometerService.ConnectionState.CONNECTED) {
                disconnect()
            } else {
                checkPermissionsAndConnect()
            }
        }
        
        binding.btnUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
    }
    
    private fun checkPermissionsAndConnect() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
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
        Toast.makeText(this, getString(R.string.scanning), Toast.LENGTH_SHORT).show()
        bluetoothService.startDiscovery()
    }
    
    private fun disconnect() {
        bluetoothService.disconnect()
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
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}

