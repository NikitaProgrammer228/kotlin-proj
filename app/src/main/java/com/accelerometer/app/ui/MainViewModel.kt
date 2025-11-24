package com.accelerometer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accelerometer.app.data.AccelerometerData
import com.accelerometer.app.utils.OscillationFrequencyCalculator
import com.accelerometer.app.utils.StabilityCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    
    private val dataPoints = mutableListOf<AccelerometerData>()
    private var measurementStartTime: Long = 0
    
    private val _stability = MutableStateFlow(0f)
    val stability: StateFlow<Float> = _stability.asStateFlow()
    
    private val _oscillationFrequency = MutableStateFlow(0f)
    val oscillationFrequency: StateFlow<Float> = _oscillationFrequency.asStateFlow()
    
    private val _measurementTime = MutableStateFlow(0L)
    val measurementTime: StateFlow<Long> = _measurementTime.asStateFlow()
    
    fun startMeasurement() {
        measurementStartTime = System.currentTimeMillis()
        dataPoints.clear()
    }
    
    fun addDataPoint(data: AccelerometerData) {
        dataPoints.add(data)
        calculateMetrics()
    }
    
    private fun calculateMetrics() {
        if (dataPoints.isEmpty()) return
        
        // Вычисляем стабильность
        val stabilityValue = StabilityCalculator.calculate(dataPoints)
        _stability.value = stabilityValue
        
        // Вычисляем время измерения в секундах
        val timeInSeconds = if (measurementStartTime > 0) {
            (System.currentTimeMillis() - measurementStartTime) / 1000f
        } else {
            0f
        }
        
        _measurementTime.value = timeInSeconds.toLong()
        
        // Вычисляем частоту колебаний
        if (timeInSeconds > 0) {
            val frequency = OscillationFrequencyCalculator.calculate(dataPoints, timeInSeconds)
            _oscillationFrequency.value = frequency
        }
    }
    
    fun reset() {
        dataPoints.clear()
        _stability.value = 0f
        _oscillationFrequency.value = 0f
        _measurementTime.value = 0L
        measurementStartTime = 0
    }
}

