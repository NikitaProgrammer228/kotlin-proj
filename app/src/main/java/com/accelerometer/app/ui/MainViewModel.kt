package com.accelerometer.app.ui

import androidx.lifecycle.ViewModel
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.measurement.MeasurementController
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val measurementController = MeasurementController()

    val measurementState: StateFlow<MeasurementState> = measurementController.state

    fun startMeasurement(durationSec: Double = MeasurementController.DEFAULT_DURATION_SEC) {
        measurementController.startMeasurement(durationSec)
    }

    fun stopMeasurement() {
        measurementController.stopMeasurement()
    }

    fun onSensorSample(sample: SensorSample) {
        measurementController.onSample(sample)
    }
}

