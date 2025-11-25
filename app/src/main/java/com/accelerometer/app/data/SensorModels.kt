package com.accelerometer.app.data

/**
 * Один сырый сэмпл с датчика.
 * [rawAx] и [rawAy] — значения из WT901 в формате «raw» (см. формулу raw/16384).
 */
data class SensorSample(
    val timestampSec: Double,
    val rawAx: Int,
    val rawAy: Int
)

/**
 * Обработанный сэмпл после конвертации и интегрирования (см. раздел 8.3 MicroSwing).
 */
data class ProcessedSample(
    val t: Double,
    val axMm: Double,
    val ayMm: Double,
    val vxMm: Double,
    val vyMm: Double,
    val sxMm: Double,
    val syMm: Double
)

/**
 * Набор метрик по текущему измерению.
 */
data class MeasurementMetrics(
    val stability: Double = 0.0,
    val oscillationFrequency: Double = 0.0,
    val coordinationFactor: Double = 0.0
)

data class MeasurementResult(
    val metrics: MeasurementMetrics,
    val durationSec: Double,
    val samples: List<ProcessedSample>
)

enum class MeasurementStatus {
    IDLE,
    CALIBRATING,
    RUNNING,
    FINISHED
}

data class MeasurementState(
    val status: MeasurementStatus = MeasurementStatus.IDLE,
    val elapsedSec: Double = 0.0,
    val processedSamples: List<ProcessedSample> = emptyList(),
    val metrics: MeasurementMetrics = MeasurementMetrics(),
    val result: MeasurementResult? = null
)

