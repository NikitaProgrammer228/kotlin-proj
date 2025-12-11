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
        val sxMm: Double,      // Позиция X (обрезанная для отображения)
        val syMm: Double,      // Позиция Y (обрезанная для отображения)
        val sxMmRaw: Double,   // Позиция X (сырая, для расчёта метрик)
        val syMmRaw: Double,   // Позиция Y (сырая, для расчёта метрик)
        val hasArtifact: Boolean = false  // Флаг артефакта (до ограничения)
    )

    private val dt = 1.0 / expectedSampleRateHz

    // Калибровка
    private var calibrated = false
    private var calibrationSamples = 0
    private var baseAngleX = 0.0
    private var baseAngleY = 0.0

    // Стабилизация после калибровки (прогрев фильтра)
    private var stabilized = false
    private var stabilizationSamples = 0
    private val stabilizationDurationSec: Double
        get() = MeasurementConfig.STABILIZATION_DURATION_SEC
    private val stabilizationSamplesNeeded: Int
        get() {
            val need = (stabilizationDurationSec * expectedSampleRateHz).toInt()
            return if (stabilizationDurationSec <= 0.0) 0 else need.coerceAtLeast(10)
        }

    // High-pass фильтр
    private var hpPrevX = 0.0
    private var hpPrevY = 0.0
    private var hpOutX = 0.0
    private var hpOutY = 0.0
    private var hpInitialized = false

    // Сглаживание (EMA) после high-pass
    // smoothAlpha = 1.0 — отключено (как у немцев, острые углы)
    // smoothAlpha = 0.3 — сильное сглаживание (округлые линии)
    private var smoothX = 0.0
    private var smoothY = 0.0
    private val smoothAlpha = 1.0  // 100% нового значения — без сглаживания, как у MicroSwing

    // Для скорости
    private var prevSx = 0.0
    private var prevSy = 0.0

    private var sampleCount = 0

    fun reset() {
        calibrated = false
        calibrationSamples = 0
        baseAngleX = 0.0
        baseAngleY = 0.0
        
        stabilized = false
        stabilizationSamples = 0

        hpPrevX = 0.0
        hpPrevY = 0.0
        hpOutX = 0.0
        hpOutY = 0.0
        hpInitialized = false

        smoothX = 0.0
        smoothY = 0.0

        prevSx = 0.0
        prevSy = 0.0
        sampleCount = 0
    }

    /**
     * Возвращает true, если и калибровка, и стабилизация завершены.
     * Только после этого данные можно использовать для отображения.
     */
    fun isCalibrated(): Boolean = calibrated && stabilized

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
        if (!calibrated) {
            if (!MeasurementConfig.ENABLE_CALIBRATION) {
                // Старт сразу: база = первый угол, стабилизация по конфигу
                baseAngleX = angleXDeg
                baseAngleY = angleYDeg
                calibrated = true
                // сбрасываем high-pass для работы от базы
                hpPrevX = 0.0
                hpPrevY = 0.0
                hpOutX = 0.0
                hpOutY = 0.0
                hpInitialized = false
                // если стабилизация отключена — сразу в рабочий режим
                if (stabilizationSamplesNeeded == 0) {
                    stabilized = true
                } else {
                    stabilized = false
                    stabilizationSamples = 0
                }
            } else {
                val calibrationSamplesNeeded = (MeasurementConfig.MOTION_CALIBRATION_DURATION_SEC * expectedSampleRateHz)
                    .toInt()
                .coerceAtLeast(10)

            baseAngleX += angleXDeg
            baseAngleY += angleYDeg
            calibrationSamples++

            if (calibrationSamples >= calibrationSamplesNeeded) {
                baseAngleX /= calibrationSamples
                baseAngleY /= calibrationSamples
                calibrated = true
                
                // Сбрасываем high-pass фильтр для работы с дельтами от базы
                hpPrevX = 0.0
                hpPrevY = 0.0
                hpOutX = 0.0
                hpOutY = 0.0
                hpInitialized = false

                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Calibration done: baseX=${baseAngleX.format(2)} baseY=${baseAngleY.format(2)}")
                }
                } else {
            return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
        }
            }
        }

        // === Отклонение от базы с учётом инверсии осей ===
        val invertX = MeasurementConfig.AXIS_INVERT_X
        val invertY = MeasurementConfig.AXIS_INVERT_Y
        var deltaX = (angleXDeg - baseAngleX) * invertX
        var deltaY = (angleYDeg - baseAngleY) * invertY

        // === High-pass фильтр ===
        if (!hpInitialized) {
            // Инициализация фильтра первым значением после калибровки
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

        // === Сглаживание (EMA) ===
        // Убираем ступеньки и одиночные всплески, чтобы быть ближе к графикам MicroSwing
        if (sampleCount == 1) {
            smoothX = hpOutX
            smoothY = hpOutY
        } else {
            smoothX = smoothAlpha * hpOutX + (1 - smoothAlpha) * smoothX
            smoothY = smoothAlpha * hpOutY + (1 - smoothAlpha) * smoothY
        }

        deltaX = smoothX
        deltaY = smoothY
        
        // === Период стабилизации ===
        // После калибровки фильтру нужно время, чтобы "прогреться"
        // В это время мы обрабатываем данные, но не возвращаем их для отображения
        if (!stabilized) {
            stabilizationSamples++
            
            // Логируем процесс стабилизации каждые 10 сэмплов
            if (MeasurementConfig.ENABLE_DEBUG_LOGS && stabilizationSamples % 10 == 0) {
                val scaleX = MeasurementConfig.ANGLE_TO_MM_SCALE_X
                val scaleY = MeasurementConfig.ANGLE_TO_MM_SCALE_Y
                val tempSx = deltaX * scaleX
                val tempSy = deltaY * scaleY
                Log.d(tag, "Stabilizing: $stabilizationSamples/$stabilizationSamplesNeeded, hp=(${deltaX.format(2)}, ${deltaY.format(2)}), pos=(${tempSx.format(1)}, ${tempSy.format(1)})")
            }
            
            if (stabilizationSamples >= stabilizationSamplesNeeded) {
                stabilized = true
                // Сбрасываем prevSx/prevSy чтобы скорость начиналась с 0
                prevSx = 0.0
                prevSy = 0.0
                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Stabilization done after $stabilizationSamples samples, hp final=(${deltaX.format(2)}, ${deltaY.format(2)})")
                }
            }
            return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
        }

        // === Угол → смещение (отдельные коэффициенты по осям) ===
        val scaleX = MeasurementConfig.ANGLE_TO_MM_SCALE_X
        val scaleY = MeasurementConfig.ANGLE_TO_MM_SCALE_Y
        var sx = deltaX * scaleX
        var sy = deltaY * scaleY

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

        if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
            Log.d(
                tag,
                "t=${timestampSec.format(3)} " +
                    "angle=(${angleXDeg.format(2)}, ${angleYDeg.format(2)}) " +
                    "delta=(${deltaX.format(2)}, ${deltaY.format(2)}) " +
                    "posRaw=(${sxRaw.format(1)}, ${syRaw.format(1)}) " +
                    "pos=(${sx.format(1)}, ${sy.format(1)}) " +
                    "artifact=$hasArtifact"
            )
        }

        return MotionState(
            axMm = deltaX,
            ayMm = deltaY,
            vxMm = vx,
            vyMm = vy,
            sxMm = sx,
            syMm = sy,
            sxMmRaw = sxRaw,
            syMmRaw = syRaw,
            hasArtifact = hasArtifact
        )
    }

    private fun Double.format(decimals: Int = 4): String = String.format("%.${decimals}f", this)
}
