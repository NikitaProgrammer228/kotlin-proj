package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Процессор движения по алгоритму MicroSwing.
 * 
 * ИСПОЛЬЗУЕТ ДВОЙНУЮ ИНТЕГРАЦИЮ УСКОРЕНИЯ (как в немецком MicroSwing):
 * AccX/AccY → VelX/VelY → PosX/PosY
 * 
 * Этапы обработки (согласно ТЗ):
 * 1. Фильтрация (LPF 5 Гц для шумоподавления)
 * 2. Калибровка bias (вычитание среднего ускорения в покое)
 * 3. Первая интеграция: ускорение → скорость
 * 4. Вторая интеграция: скорость → позиция
 * 5. Коррекция дрейфа (velocity damping)
 * 6. Детекция амплитуд и артефактов
 */
class MicroSwingMotionProcessor(
    private val expectedSampleRateHz: Double = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
) {
    private val tag = "MotionProcessor"
    
    data class MotionState(
        val axMm: Double,      // Ускорение X в мм/с² (после фильтрации)
        val ayMm: Double,      // Ускорение Y в мм/с²
        val vxMm: Double,      // Скорость X в мм/с
        val vyMm: Double,      // Скорость Y в мм/с
        val sxMm: Double,      // Позиция X (ограниченная для отображения)
        val syMm: Double,      // Позиция Y (ограниченная для отображения)
        val sxMmRaw: Double,   // Позиция X (сырая, для расчёта метрик)
        val syMmRaw: Double,   // Позиция Y (сырая, для расчёта метрик)
        val hasArtifact: Boolean = false
    )

    companion object {
        // Ускорение свободного падения (м/с²)
        private const val G_TO_M_S2 = 9.80665
        // Преобразование м → мм
        private const val M_TO_MM = 1000.0
        // g → мм/с²
        private const val G_TO_MM_S2 = G_TO_M_S2 * M_TO_MM  // ≈ 9806.65
        
        // Low-pass фильтр 5 Гц (согласно ТЗ MicroSwing)
        // alpha = dt / (RC + dt), где RC = 1/(2*pi*fc), fc = 5 Гц
        // При 50 Гц: dt = 0.02, RC = 0.0318, alpha ≈ 0.386
        private const val LPF_CUTOFF_HZ = 5.0
        
        // Коэффициент затухания скорости для борьбы с дрейфом
        // 0.98 = скорость уменьшается на 2% каждый кадр
        private const val VELOCITY_DAMPING = 0.98
        
        // Порог для ZUPT (Zero Velocity Update)
        // Если ускорение меньше этого порога N кадров подряд - обнуляем скорость
        private const val ZUPT_THRESHOLD_G = 0.015  // 0.015g ≈ 0.15 м/с²
        private const val ZUPT_FRAMES = 5  // Кадров подряд для ZUPT
    }

    private val dt = 1.0 / expectedSampleRateHz

    // Коэффициент low-pass фильтра
    private val lpfAlpha: Double

    // Калибровка bias
    private var calibrated = false
    private var calibrationSamples = 0
    private var biasAccX = 0.0
    private var biasAccY = 0.0

    // Low-pass фильтр (состояние)
    private var lpfAccX = 0.0
    private var lpfAccY = 0.0
    private var lpfInitialized = false

    // Интеграция: скорость и позиция
    private var velX = 0.0
    private var velY = 0.0
    private var posX = 0.0
    private var posY = 0.0

    // ZUPT (детекция покоя)
    private var zuptCounter = 0

    private var sampleCount = 0

    init {
        // Вычисляем alpha для LPF: alpha = dt / (RC + dt)
        val rc = 1.0 / (2.0 * Math.PI * LPF_CUTOFF_HZ)
        lpfAlpha = dt / (rc + dt)
        
        if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
            Log.d(tag, "MicroSwing processor init: dt=$dt, lpfAlpha=$lpfAlpha")
        }
    }

    fun reset() {
        calibrated = false
        calibrationSamples = 0
        biasAccX = 0.0
        biasAccY = 0.0

        lpfAccX = 0.0
        lpfAccY = 0.0
        lpfInitialized = false

        velX = 0.0
        velY = 0.0
        posX = 0.0
        posY = 0.0

        zuptCounter = 0
        sampleCount = 0
    }

    fun isCalibrated(): Boolean = calibrated

    fun processSample(
        axG: Double,
        ayG: Double,
        azG: Double,
        angleXDeg: Double,  // Не используется в MicroSwing-алгоритме
        angleYDeg: Double,  // Не используется в MicroSwing-алгоритме
        timestampSec: Double
    ): MotionState {
        sampleCount++

        // === Калибровка bias (среднее ускорение в покое) ===
        if (!calibrated) {
            if (!MeasurementConfig.ENABLE_CALIBRATION) {
                // Калибровка выключена — используем первый сэмпл как bias
                biasAccX = axG
                biasAccY = ayG
                calibrated = true
                lpfAccX = 0.0
                lpfAccY = 0.0
                lpfInitialized = true
                
                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Quick calibration: biasX=${biasAccX.format(4)}g, biasY=${biasAccY.format(4)}g")
                }
            } else {
                // Накапливаем сэмплы для усреднения
                val calibrationSamplesNeeded = (MeasurementConfig.MOTION_CALIBRATION_DURATION_SEC * expectedSampleRateHz)
                    .toInt()
                    .coerceAtLeast(10)

                biasAccX += axG
                biasAccY += ayG
                calibrationSamples++

                if (calibrationSamples >= calibrationSamplesNeeded) {
                    biasAccX /= calibrationSamples
                    biasAccY /= calibrationSamples
                    calibrated = true
                    
                    // Инициализируем LPF нулём (после вычитания bias)
                    lpfAccX = 0.0
                    lpfAccY = 0.0
                    lpfInitialized = true

                    if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                        Log.d(tag, "Calibration done: biasX=${biasAccX.format(4)}g, biasY=${biasAccY.format(4)}g, samples=$calibrationSamples")
                    }
                } else {
                    return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
                }
            }
        }

        // === Вычитаем bias ===
        val invertX = MeasurementConfig.AXIS_INVERT_X
        val invertY = MeasurementConfig.AXIS_INVERT_Y
        var accX = (axG - biasAccX) * invertX
        var accY = (ayG - biasAccY) * invertY

        // === Low-pass фильтр 5 Гц (для шумоподавления) ===
        if (!lpfInitialized) {
            lpfAccX = accX
            lpfAccY = accY
            lpfInitialized = true
        } else {
            lpfAccX = lpfAlpha * accX + (1.0 - lpfAlpha) * lpfAccX
            lpfAccY = lpfAlpha * accY + (1.0 - lpfAlpha) * lpfAccY
        }
        accX = lpfAccX
        accY = lpfAccY

        // === Преобразование g → мм/с² ===
        val accXMmS2 = accX * G_TO_MM_S2
        val accYMmS2 = accY * G_TO_MM_S2

        // === ZUPT (Zero Velocity Update) ===
        // Если ускорение мало N кадров подряд — обнуляем скорость
        val accMagnitude = sqrt(accX * accX + accY * accY)
        if (accMagnitude < ZUPT_THRESHOLD_G) {
            zuptCounter++
            if (zuptCounter >= ZUPT_FRAMES) {
                velX = 0.0
                velY = 0.0
            }
        } else {
            zuptCounter = 0
        }

        // === Первая интеграция: ускорение → скорость ===
        velX += accXMmS2 * dt
        velY += accYMmS2 * dt

        // === Velocity damping (затухание для борьбы с дрейфом) ===
        velX *= VELOCITY_DAMPING
        velY *= VELOCITY_DAMPING

        // === Вторая интеграция: скорость → позиция ===
        posX += velX * dt
        posY += velY * dt

        // === Детекция артефактов ДО ограничения ===
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val hasArtifact = abs(posX) > limit || abs(posY) > limit

        // Сохраняем сырые значения для метрик
        val posXRaw = posX
        val posYRaw = posY

        // === Ограничение позиции (±40 мм согласно ТЗ) ===
        val posXClamped = posX.coerceIn(-limit, limit)
        val posYClamped = posY.coerceIn(-limit, limit)

        if (MeasurementConfig.ENABLE_DEBUG_LOGS && sampleCount % 25 == 0) {
            Log.d(tag, "t=${timestampSec.format(2)} " +
                "acc=(${accX.format(4)}g, ${accY.format(4)}g) " +
                "vel=(${velX.format(1)}, ${velY.format(1)}) " +
                "pos=(${posXClamped.format(1)}, ${posYClamped.format(1)}) mm")
        }

        return MotionState(
            axMm = accXMmS2,
            ayMm = accYMmS2,
            vxMm = velX,
            vyMm = velY,
            sxMm = posXClamped,
            syMm = posYClamped,
            sxMmRaw = posXRaw,
            syMmRaw = posYRaw,
            hasArtifact = hasArtifact
        )
    }

    private fun Double.format(decimals: Int = 4): String = String.format("%.${decimals}f", this)
}
