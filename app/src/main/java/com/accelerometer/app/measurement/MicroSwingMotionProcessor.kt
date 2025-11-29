package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Процессор движения для измерения колебаний платформы.
 * 
 * Вместо двойного интегрирования ускорения (что даёт огромный дрейф),
 * мы вычисляем угол наклона датчика и переводим его в "виртуальное смещение".
 * 
 * Это соответствует тому, как работает MicroSwing — платформа качается,
 * и датчик измеряет угол отклонения от вертикали.
 */
class MicroSwingMotionProcessor(
    private val expectedSampleRateHz: Double = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
) {
    private val tag = "MotionProcessor"

    data class MotionState(
        val axMm: Double,
        val ayMm: Double,
        val vxMm: Double,
        val vyMm: Double,
        val sxMm: Double,
        val syMm: Double
    )

    private var startTimestampSec: Double? = null
    private var lastTimestampSec: Double? = null

    // Калибровка — запоминаем начальный наклон как "ноль"
    private var calibrated = false
    private var baseAngleXDeg = 0.0
    private var baseAngleYDeg = 0.0
    private var calibrationSumX = 0.0
    private var calibrationSumY = 0.0
    private var calibrationCount = 0

    // High-pass filter для удаления медленного дрейфа
    private var hpPrevX = 0.0
    private var hpPrevY = 0.0
    private var hpOutX = 0.0
    private var hpOutY = 0.0

    // Для расчёта "скорости" (производная позиции)
    private var prevSx = 0.0
    private var prevSy = 0.0

    fun reset() {
        startTimestampSec = null
        lastTimestampSec = null

        calibrated = false
        baseAngleXDeg = 0.0
        baseAngleYDeg = 0.0
        calibrationSumX = 0.0
        calibrationSumY = 0.0
        calibrationCount = 0

        hpPrevX = 0.0
        hpPrevY = 0.0
        hpOutX = 0.0
        hpOutY = 0.0

        prevSx = 0.0
        prevSy = 0.0
    }

    fun isCalibrated(): Boolean = calibrated

    fun processSample(
        axG: Double,
        ayG: Double,
        azG: Double,
        angleXDeg: Double,
        angleYDeg: Double,
        timestampSec: Double
    ): MotionState {
        if (startTimestampSec == null) {
            startTimestampSec = timestampSec
            lastTimestampSec = timestampSec
            return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val lastTs = lastTimestampSec ?: timestampSec
        var dt = (timestampSec - lastTs).coerceAtLeast(0.0)
        lastTimestampSec = timestampSec

        if (dt <= 0.0 || dt > 0.5) {
            dt = 1.0 / expectedSampleRateHz
        }

        val elapsed = timestampSec - (startTimestampSec ?: timestampSec)

        // === Используем углы напрямую от датчика ===
        // Датчик WT901 возвращает углы Эйлера (roll, pitch, yaw)
        // angleX = roll (наклон вокруг оси X)
        // angleY = pitch (наклон вокруг оси Y)

        // === Калибровка: запоминаем начальное положение ===
        if (!calibrated) {
            calibrationSumX += angleXDeg
            calibrationSumY += angleYDeg
            calibrationCount++

            if (elapsed >= MeasurementConfig.MOTION_CALIBRATION_DURATION_SEC) {
                baseAngleXDeg = calibrationSumX / calibrationCount
                baseAngleYDeg = calibrationSumY / calibrationCount
                calibrated = true
                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Calibration done: baseAngleX=${baseAngleXDeg.format(2)} baseAngleY=${baseAngleYDeg.format(2)}")
                }
            }
            return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // === Вычисляем отклонение от базового положения ===
        var deltaAngleX = angleXDeg - baseAngleXDeg
        var deltaAngleY = angleYDeg - baseAngleYDeg

        // === High-pass filter для удаления медленного дрейфа ===
        // Это убирает постоянное смещение, если датчик немного "уплывает"
        val alpha = MeasurementConfig.HIGH_PASS_ALPHA
        hpOutX = alpha * (hpOutX + deltaAngleX - hpPrevX)
        hpOutY = alpha * (hpOutY + deltaAngleY - hpPrevY)
        hpPrevX = deltaAngleX
        hpPrevY = deltaAngleY

        // Используем отфильтрованные значения
        deltaAngleX = hpOutX
        deltaAngleY = hpOutY

        // === Переводим угол в "виртуальное смещение" в мм ===
        // Коэффициент масштабирования: сколько мм на градус
        // Подбирается эмпирически для соответствия MicroSwing
        val mmPerDegree = MeasurementConfig.ANGLE_TO_MM_SCALE

        val sx = deltaAngleX * mmPerDegree
        val sy = deltaAngleY * mmPerDegree

        // === Вычисляем "скорость" как производную позиции ===
        val vx = (sx - prevSx) / dt
        val vy = (sy - prevSy) / dt
        prevSx = sx
        prevSy = sy

        // === Ограничение позиции ===
        var finalSx = sx
        var finalSy = sy
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val radiusSq = finalSx * finalSx + finalSy * finalSy
        if (radiusSq > limit * limit) {
            val scale = limit / sqrt(radiusSq)
            finalSx *= scale
            finalSy *= scale
        }

        if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
            Log.d(
                tag,
                "angle=(${deltaAngleX.format(2)}, ${deltaAngleY.format(2)}) " +
                    "pos=(${finalSx.format(1)}, ${finalSy.format(1)}) " +
                    "raw=(${angleXDeg.format(2)}, ${angleYDeg.format(2)})"
            )
        }

        return MotionState(
            axMm = deltaAngleX,  // Храним угол для отладки
            ayMm = deltaAngleY,
            vxMm = vx,
            vyMm = vy,
            sxMm = finalSx,
            syMm = finalSy
        )
    }

    private fun Double.format(decimals: Int = 4): String = String.format("%.${decimals}f", this)
}
