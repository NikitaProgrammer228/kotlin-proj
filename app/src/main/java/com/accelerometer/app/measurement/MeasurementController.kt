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
    private val amplitudeThresholdMm: Double = MeasurementConfig.AMPLITUDE_THRESHOLD_MM
) {

    companion object {
        const val DEFAULT_DURATION_SEC = MeasurementConfig.MEASUREMENT_DURATION_SEC
    }

    private val processor = MeasurementProcessor()
    private val processed = mutableListOf<ProcessedSample>()
    private var status: MeasurementStatus = MeasurementStatus.IDLE
    private var targetDurationSec = DEFAULT_DURATION_SEC
    
    // Валидация теста
    private var isValid = true
    private var validationMessage: String? = null
    private var lastSampleTimestamp: Double? = null
    private val maxGapSec = 0.1  // Максимальный пропуск между пакетами (100 мс при 50 Гц)
    private val artifactThresholdMm = 40.0  // Порог артефакта согласно ТЗ

    private val _state = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = _state.asStateFlow()

    fun startMeasurement(durationSec: Double = DEFAULT_DURATION_SEC) {
        targetDurationSec = durationSec
        processor.reset()
        processed.clear()
        status = MeasurementStatus.RUNNING
        isValid = true
        validationMessage = null
        lastSampleTimestamp = null
        _state.value = MeasurementState(status = status, elapsedSec = 0.0, isValid = true)
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
        
        // Проверка обрыва BLE (пропуск пакетов)
        var bleGap: Int? = null
        if (lastSampleTimestamp != null) {
            val gap = sample.timestampSec - lastSampleTimestamp!!
            if (gap > maxGapSec) {
                isValid = false
                bleGap = (gap * 1000).toInt()
            }
        }
        lastSampleTimestamp = sample.timestampSec
        
        val processedSample = processor.process(sample)
        
        // Проверка артефактов (posX/posY > ±40 мм согласно ТЗ)
        // Используем флаг из процессора, который проверяет ДО ограничения
        // Артефакт имеет приоритет над обрывом BLE
        if (processedSample.hasArtifact) {
            isValid = false
            validationMessage = "Артефакт: смещение превышает ±${artifactThresholdMm.toInt()} мм"
        } else if (bleGap != null) {
            // Показываем обрыв BLE только если нет артефакта
            validationMessage = "Обрыв BLE: пропуск ${bleGap} мс"
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
                metrics = metrics,
                isValid = isValid,
                validationMessage = validationMessage
            )
        }
    }
    
    private fun abs(value: Double) = kotlin.math.abs(value)

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
            result = result,
            isValid = isValid,
            validationMessage = validationMessage
        )
    }
}

