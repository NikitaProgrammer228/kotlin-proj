package com.accelerometer.app.ui

import androidx.lifecycle.ViewModel
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.measurement.MeasurementConfig
import com.accelerometer.app.measurement.MeasurementController
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel для графика мишени (target).
 * Использует отдельный коэффициент COORDINATION_SCALE_TARGET для расчёта КФ.
 */
class TargetViewModel : ViewModel() {

    private val measurementController = MeasurementController(
        coordinationScale = MeasurementConfig.COORDINATION_SCALE_TARGET
    )

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
