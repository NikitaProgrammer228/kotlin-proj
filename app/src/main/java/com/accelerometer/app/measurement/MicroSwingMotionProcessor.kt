package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Процессор движения для MicroSwing-подобных измерений.
 * 
 * Использует УГЛЫ от датчика (roll/pitch) для определения смещения.
 * 
 * Почему не двойное интегрирование ускорения:
 * - MEMS-акселерометры имеют значительный дрейф
 * - Компенсация гравитации требует точной калибровки
 * - Ошибки накапливаются экспоненциально при интегрировании
 * 
 * Датчик WT901 имеет встроенный гироскоп и вычисляет углы Эйлера,
 * которые гораздо стабильнее чем интегрированное ускорение.
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
        val syMm: Double,
        val hasArtifact: Boolean = false  // Флаг артефакта (до ограничения)
    )

    private val dt = 1.0 / expectedSampleRateHz

    // Калибровка
    private var calibrated = false
    private var calibrationSamples = 0
    private var baseAngleX = 0.0
    private var baseAngleY = 0.0

    // High-pass фильтр
    private var hpPrevX = 0.0
    private var hpPrevY = 0.0
    private var hpOutX = 0.0
    private var hpOutY = 0.0
    private var hpInitialized = false

    // Для скорости
    private var prevSx = 0.0
    private var prevSy = 0.0

    private var sampleCount = 0

    fun reset() {
        calibrated = false
        calibrationSamples = 0
        baseAngleX = 0.0
        baseAngleY = 0.0

        hpPrevX = 0.0
        hpPrevY = 0.0
        hpOutX = 0.0
        hpOutY = 0.0
        hpInitialized = false

        prevSx = 0.0
        prevSy = 0.0
        sampleCount = 0
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
        sampleCount++

        // === Калибровка ===
        val calibrationSamplesNeeded = (MeasurementConfig.MOTION_CALIBRATION_DURATION_SEC * expectedSampleRateHz).toInt()
                .coerceAtLeast(10)

        if (!calibrated) {
            baseAngleX += angleXDeg
            baseAngleY += angleYDeg
            calibrationSamples++

            if (calibrationSamples >= calibrationSamplesNeeded) {
                baseAngleX /= calibrationSamples
                baseAngleY /= calibrationSamples
                calibrated = true

                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Calibration done: baseX=${baseAngleX.format(2)} baseY=${baseAngleY.format(2)}")
                }
            }
            return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
        }

        // === Отклонение от базы ===
        var deltaX = angleXDeg - baseAngleX
        var deltaY = angleYDeg - baseAngleY

        // === High-pass фильтр ===
        if (!hpInitialized) {
            hpPrevX = deltaX
            hpPrevY = deltaY
            hpOutX = 0.0
            hpOutY = 0.0
            hpInitialized = true
        }

        val alpha = MeasurementConfig.HIGH_PASS_ALPHA
        hpOutX = alpha * (hpOutX + deltaX - hpPrevX)
        hpOutY = alpha * (hpOutY + deltaY - hpPrevY)
        hpPrevX = deltaX
        hpPrevY = deltaY

        deltaX = hpOutX
        deltaY = hpOutY

        // === Угол → смещение ===
        val scale = MeasurementConfig.ANGLE_TO_MM_SCALE
        var sx = deltaX * scale
        var sy = deltaY * scale

        // === Детекция артефактов ДО ограничения ===
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val hasArtifact = abs(sx) > limit || abs(sy) > limit
        
        // Сохраняем исходные значения для логирования
        val sxRaw = sx
        val syRaw = sy

        // === Ограничение по осям (согласно ТЗ: артефакт при posX/posY > ±40 мм) ===
        // Ограничиваем каждую ось отдельно
        sx = sx.coerceIn(-limit, limit)
        sy = sy.coerceIn(-limit, limit)
        
        // Логируем артефакты
        if (hasArtifact && MeasurementConfig.ENABLE_DEBUG_LOGS) {
            Log.w(tag, "ARTIFACT: rawPos=(${sxRaw.format(1)}, ${syRaw.format(1)}) -> limited=(${sx.format(1)}, ${sy.format(1)})")
        }

        // === Скорость ===
        val vx = (sx - prevSx) / dt
        val vy = (sy - prevSy) / dt
        prevSx = sx
        prevSy = sy

        if (MeasurementConfig.ENABLE_DEBUG_LOGS && sampleCount % 10 == 0) {
            Log.d(tag, "angle=(${deltaX.format(2)}, ${deltaY.format(2)}) pos=(${sx.format(1)}, ${sy.format(1)})")
        }

        return MotionState(
            axMm = deltaX,
            ayMm = deltaY,
            vxMm = vx,
            vyMm = vy,
            sxMm = sx,
            syMm = sy,
            hasArtifact = hasArtifact
        )
    }

    private fun Double.format(decimals: Int = 4): String = String.format("%.${decimals}f", this)
}
