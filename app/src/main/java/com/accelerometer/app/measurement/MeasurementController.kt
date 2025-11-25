package com.accelerometer.app.measurement

import com.accelerometer.app.data.MeasurementMetrics
import com.accelerometer.app.data.MeasurementResult
import com.accelerometer.app.data.MeasurementState
import com.accelerometer.app.data.MeasurementStatus
import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.utils.MeasurementMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeasurementController(
    private val correctionFactor: Double = MeasurementConfig.OSCILLATION_CORRECTION,
    private val coordinationScale: Double = MeasurementConfig.COORDINATION_SCALE,
    private val amplitudeThresholdMm: Double = MeasurementConfig.AMPLITUDE_THRESHOLD_MM,
    private val calibrationDurationSec: Double = MeasurementConfig.CALIBRATION_DURATION_SEC
) {

    companion object {
        const val DEFAULT_DURATION_SEC = MeasurementConfig.MEASUREMENT_DURATION_SEC
    }

    private val processor = MeasurementProcessor(calibrationDurationSec)
    private val processed = mutableListOf<ProcessedSample>()
    private var status: MeasurementStatus = MeasurementStatus.IDLE
    private var targetDurationSec = DEFAULT_DURATION_SEC

    private val _state = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = _state.asStateFlow()

    fun startMeasurement(durationSec: Double = DEFAULT_DURATION_SEC) {
        targetDurationSec = durationSec
        processor.reset()
        processed.clear()
        status = MeasurementStatus.CALIBRATING
        _state.value = MeasurementState(status = status)
    }

    fun stopMeasurement(resetToIdle: Boolean = true) {
        if (status == MeasurementStatus.IDLE) return
        finalizeMeasurement()
        if (resetToIdle) {
            processed.clear()
            status = MeasurementStatus.IDLE
            _state.value = MeasurementState(status = MeasurementStatus.IDLE)
        }
    }

    fun onSample(sample: SensorSample) {
        if (status == MeasurementStatus.IDLE || status == MeasurementStatus.FINISHED) return
        val processedSample = processor.process(sample)
        if (processedSample == null) {
            _state.value = MeasurementState(
                status = MeasurementStatus.CALIBRATING,
                processedSamples = emptyList()
            )
            return
        }

        processed += processedSample
        val elapsed = processedSample.t
        status = if (elapsed >= targetDurationSec) MeasurementStatus.FINISHED else MeasurementStatus.RUNNING

        val metrics = MeasurementMath.buildMetrics(
            processed,
            durationSec = elapsed.coerceAtLeast(0.0001),
            amplitudeThresholdMm = amplitudeThresholdMm,
            correctionFactor = correctionFactor,
            scalingCoefficient = coordinationScale
        )

        if (status == MeasurementStatus.FINISHED) {
            finalizeMeasurement(metrics)
        } else {
            _state.value = MeasurementState(
                status = status,
                elapsedSec = elapsed,
                processedSamples = processed.toList(),
                metrics = metrics
            )
        }
    }

    private fun finalizeMeasurement(precomputed: MeasurementMetrics? = null) {
        status = MeasurementStatus.FINISHED
        if (processed.isEmpty()) {
            _state.value = MeasurementState(status = MeasurementStatus.IDLE)
            status = MeasurementStatus.IDLE
            return
        }

        val duration = processed.last().t
        val metrics = precomputed ?: MeasurementMath.buildMetrics(
            processed,
            durationSec = duration.coerceAtLeast(0.0001),
            amplitudeThresholdMm = amplitudeThresholdMm,
            correctionFactor = correctionFactor,
            scalingCoefficient = coordinationScale
        )
        val result = MeasurementResult(
            metrics = metrics,
            durationSec = duration,
            samples = processed.toList()
        )
        _state.value = MeasurementState(
            status = MeasurementStatus.FINISHED,
            elapsedSec = duration,
            processedSamples = processed.toList(),
            metrics = metrics,
            result = result
        )
    }
}

