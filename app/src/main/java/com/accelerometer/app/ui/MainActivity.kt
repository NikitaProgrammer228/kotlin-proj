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
import androidx.lifecycle.lifecycleScope
import com.accelerometer.app.R
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService
import com.accelerometer.app.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bluetoothService: BluetoothAccelerometerService
    
    private var measurementStartTime: Long = 0
    
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
        // Настройка графика для оси X
        setupChart(binding.chartX, "X", android.graphics.Color.BLUE)
        
        // Настройка графика для оси Y
        setupChart(binding.chartY, "Y", android.graphics.Color.RED)
    }
    
    private fun setupChart(chart: com.github.mikephil.charting.charts.LineChart, label: String, color: Int) {
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
            bluetoothService.connectionState.collect { state ->
                updateConnectionState(state)
            }
        }
        
        lifecycleScope.launch {
            bluetoothService.accelerometerData.collect { data ->
                data?.let {
                    updateCharts(it)
                    viewModel.addDataPoint(it)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.stability.collect { stability ->
                binding.tvStability.text = getString(R.string.stability) + ": ${String.format("%.2f", stability)}%"
            }
        }
        
        lifecycleScope.launch {
            viewModel.oscillationFrequency.collect { frequency ->
                binding.tvOscillationFrequency.text = 
                    getString(R.string.oscillation_frequency) + ": ${String.format("%.2f", frequency)} Hz"
            }
        }
        
        lifecycleScope.launch {
            viewModel.measurementTime.collect { time ->
                binding.tvMeasurementTime.text = getString(R.string.measurement_time, time)
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
        viewModel.reset()
        clearCharts()
    }
    
    private fun updateConnectionState(state: BluetoothAccelerometerService.ConnectionState) {
        when (state) {
            BluetoothAccelerometerService.ConnectionState.DISCONNECTED -> {
                binding.connectionStatus.text = getString(R.string.disconnected)
                binding.btnConnect.text = getString(R.string.connect)
                measurementStartTime = 0
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTING -> {
                binding.connectionStatus.text = getString(R.string.scanning)
                binding.btnConnect.text = getString(R.string.connect)
            }
            BluetoothAccelerometerService.ConnectionState.CONNECTED -> {
                binding.connectionStatus.text = getString(R.string.connected)
                binding.btnConnect.text = getString(R.string.disconnect)
                measurementStartTime = System.currentTimeMillis()
                viewModel.startMeasurement()
            }
        }
    }
    
    private fun updateCharts(data: com.accelerometer.app.data.AccelerometerData) {
        val time = (data.timestamp - measurementStartTime) / 1000f
        
        // Обновляем график X
        updateChart(binding.chartX, time, data.x, android.graphics.Color.BLUE)
        
        // Обновляем график Y
        updateChart(binding.chartY, time, data.y, android.graphics.Color.RED)
    }
    
    private fun updateChart(chart: com.github.mikephil.charting.charts.LineChart, x: Float, y: Float, color: Int) {
        val data = chart.data
        if (data == null) {
            val entries = mutableListOf<Entry>()
            entries.add(Entry(x, y))
            val dataSet = LineDataSet(entries, "Data")
            dataSet.color = color
            dataSet.setCircleColor(color)
            dataSet.lineWidth = 2f
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            chart.data = LineData(dataSet)
        } else {
            val dataSet = data.getDataSetByIndex(0) as LineDataSet
            dataSet.addEntry(Entry(x, y))
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
            
            // Ограничиваем количество точек для производительности
            if (dataSet.entryCount > 1000) {
                val entries = dataSet.values.toMutableList()
                entries.removeAt(0)
                dataSet.values = entries
            }
        }
    }
    
    private fun clearCharts() {
        binding.chartX.clear()
        binding.chartY.clear()
        binding.chartX.invalidate()
        binding.chartY.invalidate()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}

