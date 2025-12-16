package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Процессор движения по алгоритму MicroSwing.
 * 
 * Использует ДВОЙНУЮ ИНТЕГРАЦИЮ УСКОРЕНИЯ с правильной обработкой:
 * 1. Перевод g → m/s² → mm/s²
 * 2. Калибровка bias (200+ сэмплов)
 * 3. Low-pass для выделения gravity
 * 4. High-pass для убирания DC/дрейфа
 * 5. Интеграция с демпфированием скорости
 * 6. ZUPT (обнуление скорости при покое)
 */
class MicroSwingMotionProcessor(
    private val expectedSampleRateHz: Double = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
) {
    private val tag = "MotionProcessor"
    
    data class MotionState(
        val axMm: Double,      // Отфильтрованное ускорение X в мм/с²
        val ayMm: Double,      // Отфильтрованное ускорение Y в мм/с²
        val vxMm: Double,      // Скорость X в мм/с
        val vyMm: Double,      // Скорость Y в мм/с
        val sxMm: Double,      // Позиция X (для отображения, с ограничением)
        val syMm: Double,      // Позиция Y (для отображения, с ограничением)
        val sxMmRaw: Double,   // Позиция X (сырая, для расчёта метрик)
        val syMmRaw: Double,   // Позиция Y (сырая, для расчёта метрик)
        val hasArtifact: Boolean = false
    )

    companion object {
        // Физические константы
        private const val G_MPS2 = 9.80665       // м/с²
        private const val G_MMPS2 = 9806.65      // мм/с²
        
        // Калибровка: минимум 200 сэмплов (4 сек при 50 Гц)
        private const val CALIBRATION_SAMPLES = 200
        
        // Low-pass для выделения gravity (очень низкая частота, ~0.1 Гц)
        private const val GRAVITY_LPF_HZ = 0.1
        
        // High-pass для убирания DC/дрейфа (~0.3 Гц)
        private const val SIGNAL_HPF_HZ = 0.3
        
        // Демпфирование скорости (экспоненциальное затухание)
        private const val VELOCITY_DAMPING = 3.0  // подбирается
        
        // ZUPT порог (м/с²) — если ускорение меньше, считаем что стоит
        private const val ZUPT_THRESHOLD_MPS2 = 0.15  // ~0.015g
        private const val ZUPT_FRAMES = 5
        
        // Масштаб для визуализации (подбирается под немецкий софт)
        private const val DISPLAY_SCALE = 50.0
    }

    // Время
    private var lastTimestamp: Double? = null
    
    // Калибровка bias
    private var calibrated = false
    private var calibrationSamples = 0
    private var biasAccX = 0.0
    private var biasAccY = 0.0
    private var biasAccZ = 0.0

    // Low-pass фильтры для выделения gravity
    private val lpfGravityX = LowPassFilter(GRAVITY_LPF_HZ)
    private val lpfGravityY = LowPassFilter(GRAVITY_LPF_HZ)
    private val lpfGravityZ = LowPassFilter(GRAVITY_LPF_HZ)
    
    // High-pass фильтры для убирания DC
    private val hpfX = HighPassFilter(SIGNAL_HPF_HZ)
    private val hpfY = HighPassFilter(SIGNAL_HPF_HZ)

    // Интеграция
    private var velX = 0.0  // мм/с
    private var velY = 0.0
    private var posX = 0.0  // мм (без ограничения!)
    private var posY = 0.0

    // ZUPT
    private var zuptCounter = 0

    private var sampleCount = 0

    fun reset() {
        lastTimestamp = null
        calibrated = false
        calibrationSamples = 0
        biasAccX = 0.0
        biasAccY = 0.0
        biasAccZ = 0.0
        
        lpfGravityX.reset()
        lpfGravityY.reset()
        lpfGravityZ.reset()
        hpfX.reset()
        hpfY.reset()
        
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
        angleXDeg: Double,
        angleYDeg: Double,
        timestampSec: Double
    ): MotionState {
        sampleCount++
        
        // === Вычисляем dt по реальным timestamp ===
        val dt = if (lastTimestamp != null) {
            (timestampSec - lastTimestamp!!).coerceIn(0.001, 0.1)  // защита от выбросов
        } else {
            1.0 / expectedSampleRateHz
        }
        lastTimestamp = timestampSec

        // === Перевод g → m/s² ===
        val axMps2 = axG * G_MPS2
        val ayMps2 = ayG * G_MPS2
        val azMps2 = azG * G_MPS2

        // === Калибровка bias (накапливаем среднее) ===
        if (!calibrated) {
            biasAccX += axMps2
            biasAccY += ayMps2
            biasAccZ += azMps2
            calibrationSamples++

            if (calibrationSamples >= CALIBRATION_SAMPLES) {
                biasAccX /= calibrationSamples
                biasAccY /= calibrationSamples
                biasAccZ /= calibrationSamples
                calibrated = true
                
                // Инициализируем gravity фильтры начальными значениями
                lpfGravityX.init(biasAccX)
                lpfGravityY.init(biasAccY)
                lpfGravityZ.init(biasAccZ)

                if (MeasurementConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, "Calibration done: biasX=${biasAccX.format(4)} biasY=${biasAccY.format(4)} biasZ=${biasAccZ.format(4)} m/s², samples=$calibrationSamples")
                }
            } else {
                return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
            }
        }

        // === Вычитаем bias ===
        val invertX = MeasurementConfig.AXIS_INVERT_X
        val invertY = MeasurementConfig.AXIS_INVERT_Y
        var ax = (axMps2 - biasAccX) * invertX
        var ay = (ayMps2 - biasAccY) * invertY
        val az = azMps2 - biasAccZ

        // === Low-pass для выделения gravity (медленная составляющая) ===
        val gX = lpfGravityX.update(ax, dt)
        val gY = lpfGravityY.update(ay, dt)
        
        // === Линейное ускорение (без gravity) ===
        val linX = ax - gX
        val linY = ay - gY

        // === High-pass для убирания DC/дрейфа ===
        val fx = hpfX.update(linX, dt)
        val fy = hpfY.update(linY, dt)
        
        // === Перевод m/s² → mm/s² для отображения ===
        val fxMm = fx * 1000.0
        val fyMm = fy * 1000.0

        // === ZUPT: обнуляем скорость при покое ===
        val aMag = sqrt(fx * fx + fy * fy)
        if (aMag < ZUPT_THRESHOLD_MPS2) {
            zuptCounter++
            if (zuptCounter >= ZUPT_FRAMES) {
                velX = 0.0
                velY = 0.0
                // Медленный возврат позиции к нулю
                posX *= 0.99
                posY *= 0.99
            }
        } else {
            zuptCounter = 0
        }

        // === Интеграция: ускорение → скорость (в мм/с) ===
        velX += fxMm * dt
        velY += fyMm * dt

        // === Демпфирование скорости (экспоненциальное затухание) ===
        val dampingFactor = exp(-VELOCITY_DAMPING * dt)
        velX *= dampingFactor
        velY *= dampingFactor

        // === Интеграция: скорость → позиция (в мм) ===
        posX += velX * dt
        posY += velY * dt

        // === Масштабирование для отображения ===
        val displayPosX = posX * DISPLAY_SCALE
        val displayPosY = posY * DISPLAY_SCALE

        // === Детекция артефактов (на немасштабированных данных) ===
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val hasArtifact = abs(displayPosX) > limit || abs(displayPosY) > limit

        // === Ограничение ТОЛЬКО для отрисовки, не для расчётов ===
        val clampedPosX = displayPosX.coerceIn(-limit, limit)
        val clampedPosY = displayPosY.coerceIn(-limit, limit)

        if (MeasurementConfig.ENABLE_DEBUG_LOGS && sampleCount % 50 == 0) {
            Log.d(tag, "dt=${dt.format(4)} " +
                "acc=(${ax.format(4)}, ${ay.format(4)}) " +
                "lin=(${linX.format(4)}, ${linY.format(4)}) " +
                "filt=(${fx.format(4)}, ${fy.format(4)}) m/s² " +
                "vel=(${velX.format(1)}, ${velY.format(1)}) " +
                "pos=(${displayPosX.format(1)}, ${displayPosY.format(1)}) mm")
        }

        return MotionState(
            axMm = fxMm,
            ayMm = fyMm,
            vxMm = velX,
            vyMm = velY,
            sxMm = clampedPosX,
            syMm = clampedPosY,
            sxMmRaw = displayPosX,
            syMmRaw = displayPosY,
            hasArtifact = hasArtifact
        )
    }

    private fun Double.format(decimals: Int = 4): String = String.format("%.${decimals}f", this)
    
    // ═══════════════════════════════════════════════════════════════════
    // Вспомогательные классы фильтров
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Low-pass фильтр (1-pole IIR)
     * Используется для выделения gravity (медленной составляющей)
     */
    private class LowPassFilter(private val cutoffHz: Double) {
        private var y = 0.0
        private var initialized = false
        
        fun init(value: Double) {
            y = value
            initialized = true
        }
        
        fun reset() {
            y = 0.0
            initialized = false
        }
        
        fun update(x: Double, dt: Double): Double {
            if (!initialized) {
                y = x
                initialized = true
                return y
            }
            val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
            val alpha = dt / (rc + dt)
            y += alpha * (x - y)
            return y
        }
    }
    
    /**
     * High-pass фильтр (1-pole IIR)
     * Используется для убирания DC/дрейфа
     */
    private class HighPassFilter(private val cutoffHz: Double) {
        private var prevX = 0.0
        private var prevY = 0.0
        private var initialized = false
        
        fun reset() {
            prevX = 0.0
            prevY = 0.0
            initialized = false
        }
        
        fun update(x: Double, dt: Double): Double {
            if (!initialized) {
                prevX = x
                prevY = 0.0
                initialized = true
                return 0.0
            }
            val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
            val alpha = rc / (rc + dt)
            val y = alpha * (prevY + x - prevX)
            prevX = x
            prevY = y
            return y
        }
    }
}
