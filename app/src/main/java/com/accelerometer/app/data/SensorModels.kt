package com.accelerometer.app.data

/**
 * Один сэмпл с датчика WT901: ускорение в g + ориентация (градусы).
 * SDK WitMotion возвращает значения уже в единицах g.
 */
data class SensorSample(
    val timestampSec: Double,
    val accXg: Double,
    val accYg: Double,
    val accZg: Double,
    val angleXDeg: Double,
    val angleYDeg: Double,
    val angleZDeg: Double
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
    val sxMm: Double,      // Позиция X (обрезанная для отображения)
    val syMm: Double,      // Позиция Y (обрезанная для отображения)
    val sxMmRaw: Double,   // Позиция X (сырая, для расчёта метрик)
    val syMmRaw: Double,   // Позиция Y (сырая, для расчёта метрик)
    val hasArtifact: Boolean = false  // Флаг артефакта (до ограничения позиции)
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
    val result: MeasurementResult? = null,
    val isValid: Boolean = true,  // Валидность теста (false при обрывах BLE или артефактах)
    val validationMessage: String? = null  // Сообщение о проблеме
)

