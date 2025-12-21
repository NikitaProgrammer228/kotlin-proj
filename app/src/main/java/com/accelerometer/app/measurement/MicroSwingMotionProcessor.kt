package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Процессор движения по алгоритму MicroSwing.
 * 
 * ✅ ОПТИМИЗИРОВАНО для 50 Hz: После настройки RSW=ACC_ONLY и RRATE=50Hz
 * датчик выдаёт стабильно ~50 Hz.
 * 
 * ⚠️ НОВЫЙ ПОДХОД: "Resonant Spring-Mass" модель (ВТОРОЙ ПОРЯДОК)
 * 
 * Проблема:
 * - Washout/Leaky Integrator (первый порядок) НЕ создаёт осцилляции
 * - Немецкий MicroSwing показывает чёткие ОСЦИЛЛЯЦИИ (overshoot) после толчка
 * 
 * Решение: Используем резонансную систему (пружина-масса, второй порядок):
 * 
 *   system_acceleration = -omega^2 * position - 2*zeta*omega * velocity + input_acceleration
 *   velocity += system_acceleration * dt
 *   position += velocity * dt
 * 
 * Где:
 * - omega = натуральная частота (определяет скорость осцилляций)
 * - zeta = коэффициент демпфирования (zeta < 1 = осцилляции, zeta >= 1 = без осцилляций)
 * 
 * Это создаёт затухающие колебания как на немецком графике!
 */
class MicroSwingMotionProcessor(
    private val expectedSampleRateHz: Double = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
) {
    private val tag = "MotionProcessor"

    data class MotionState(
        val axMm: Double,      // Ускорение X (mm/s²)
        val ayMm: Double,      // Ускорение Y (mm/s²)
        val vxMm: Double,      // Скорость X (mm/s)
        val vyMm: Double,      // Скорость Y (mm/s)
        val sxMm: Double,      // Позиция X (mm) - для отображения
        val syMm: Double,      // Позиция Y (mm) - для отображения
        val sxMmRaw: Double,   // Позиция X сырая (для метрик)
        val syMmRaw: Double,   // Позиция Y сырая (для метрик)
        val hasArtifact: Boolean = false
    )

    companion object {
        // ===========================================
        // КАЛИБРОВКА
        // ===========================================
        private const val CALIBRATION_SAMPLES = 10
        
        // ===========================================
        // КОНВЕРТАЦИЯ УГОЛ → ММ (для резервного режима)
        // ===========================================
        private const val ANGLE_TO_MM = 7.0
        
        // ===========================================
        // ФИЗИКА
        // ===========================================
        private const val G_TO_MM_S2 = 9806.65  // 1g = 9806.65 mm/s²
        
        // ===========================================
        // ПОРОГ ШУМА (DEAD-ZONE)
        // ===========================================
        // Ускорения ниже этого порога считаются шумом и игнорируются.
        // Типичный шум смартфона: 0.002-0.005g
        // Устанавливаем 0.003g как порог (3 mg)
        private const val NOISE_THRESHOLD_G = 0.003
        
        // ===========================================
        // РЕЗОНАНСНАЯ СИСТЕМА (второй порядок)
        // ===========================================
        // 3.0 Hz - более плавные колебания, менее резкая реакция на шум
        private const val RESONANT_FREQ_HZ = 3.0
        
        // 0.7 - увеличенное демпфирование, меньше "звона" при шуме
        private const val DAMPING_RATIO = 0.7
        
        // "Живость" линии - значительно уменьшена для подавления шума
        private const val ACCEL_BYPASS_FACTOR = 30.0
        
        // ===========================================
        // НАСТРОЙКА ОСЕЙ
        // ===========================================
        private const val SWAP_XY_AXES = true
        private const val INVERT_X = 1.0
        private const val INVERT_Y = 1.0  // Возвращаем как было, раз Y работал верно
        
        // ===========================================
        // ПОДАВЛЕНИЕ И ДЕКОРРЕЛЯЦИЯ (УМНЫЙ РЕЖИМ)
        // ===========================================
        // Убираем глухое подавление, возвращаем 1.0 (полная чувствительность)
        private const val X_AXIS_SUPPRESSION = 1.0
        
        // Декорреляция: 0.5 - умеренное вычитание осей друг из друга
        // Это поможет убрать "эхо" одной оси на другой
        private const val X_Y_DECORRELATION = 0.5
        
        // ===========================================
        // ФИЛЬТРЫ
        // ===========================================
        private const val ACC_HPF_CUTOFF_HZ = 0.3
        
        // Снижаем LPF до 5.0 Hz для более агрессивного подавления шума
        private const val ACC_LPF_CUTOFF_HZ = 5.0
        
        private const val X_AXIS_EXTRA_LPF_HZ = 3.0
        private const val ANGLE_HPF_CUTOFF_HZ = 0.1
        
        // ===========================================
        // МАСШТАБ
        // ===========================================
        // 80.0 - уменьшено чтобы шум не превращался в огромные скачки
        private const val DISPLAY_SCALE = 80.0
        
        // Отключаем boost для малых движений - он усиливал шум!
        private const val SENSITIVITY_BOOST = 1.0
        private const val SMALL_MOTION_THRESHOLD_MM = 10.0
    }

    // Калибровка
    private var calibrated = false
    private var calibrationSamples = 0
    private var biasAccX = 0.0
    private var biasAccY = 0.0
    private var biasAngleX = 0.0
    private var biasAngleY = 0.0

    // Фильтры для ускорения (band-pass = HPF + LPF)
    private val hpfAccX = HighPassFilter(ACC_HPF_CUTOFF_HZ)
    private val hpfAccY = HighPassFilter(ACC_HPF_CUTOFF_HZ)
    private val lpfAccX = LowPassFilter(ACC_LPF_CUTOFF_HZ)
    private val lpfAccY = LowPassFilter(ACC_LPF_CUTOFF_HZ)

    // Дополнительный LPF для X оси (убираем "зубцы")
    private val extraLpfX = LowPassFilter(X_AXIS_EXTRA_LPF_HZ)

    // Фильтры для углов
    private val hpfAngleX = HighPassFilter(ANGLE_HPF_CUTOFF_HZ)
    private val hpfAngleY = HighPassFilter(ANGLE_HPF_CUTOFF_HZ)
    
    // Washout / Leaky Integrator модель
    private var velocityX = 0.0  // mm/s
    private var velocityY = 0.0
    private var positionX = 0.0  // mm (с washout - возвращается к центру)
    private var positionY = 0.0
    
    // Для метрик (реальная интегрированная позиция - для расчета пути)
    private var rawPositionX = 0.0
    private var rawPositionY = 0.0

    // Время
    private var lastTimestamp: Double? = null
    private var sampleCount = 0
    
    // Реальная частота
    var realSampleRateHz: Double = 50.0
        private set

    fun reset() {
        calibrated = false
        calibrationSamples = 0
        biasAccX = 0.0
        biasAccY = 0.0
        biasAngleX = 0.0
        biasAngleY = 0.0
        hpfAccX.reset()
        hpfAccY.reset()
        lpfAccX.reset()
        lpfAccY.reset()
        extraLpfX.reset()
        hpfAngleX.reset()
        hpfAngleY.reset()
        velocityX = 0.0
        velocityY = 0.0
        positionX = 0.0
        positionY = 0.0
        rawPositionX = 0.0
        rawPositionY = 0.0
        lastTimestamp = null
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
        
        // === dt ===
        val dt = if (lastTimestamp != null) {
            val rawDt = timestampSec - lastTimestamp!!
            if (rawDt > 0) {
                realSampleRateHz = realSampleRateHz * 0.9 + (1.0 / rawDt) * 0.1
            }
            // При 50 Hz ожидаем dt ≈ 0.02 сек
            rawDt.coerceIn(0.01, 0.1)  // Обновлено для 50 Hz
        } else {
            0.02  // Ожидаем 50 Hz = 0.02 сек между кадрами
        }
        lastTimestamp = timestampSec

        // === Калибровка ===
        if (!calibrated) {
            biasAccX += axG
            biasAccY += ayG
            biasAngleX += angleXDeg
            biasAngleY += angleYDeg
            calibrationSamples++

            if (sampleCount % 2 == 0) {
                Log.d(tag, "Calibrating: $calibrationSamples/$CALIBRATION_SAMPLES, " +
                    "acc=(${f4(axG)}g, ${f4(ayG)}g), angle=(${f2(angleXDeg)}°, ${f2(angleYDeg)}°)")
            }

            if (calibrationSamples >= CALIBRATION_SAMPLES) {
                biasAccX /= calibrationSamples
                biasAccY /= calibrationSamples
                biasAngleX /= calibrationSamples
                biasAngleY /= calibrationSamples
                calibrated = true
                Log.d(tag, "✓ Calibration DONE: " +
                    "biasAcc=(${f4(biasAccX)}g, ${f4(biasAccY)}g), " +
                    "biasAngle=(${f2(biasAngleX)}°, ${f2(biasAngleY)}°)")
            } else {
                return MotionState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
            }
        }

        // === Применяем инверсию осей ===
        val rawAccXTemp = (axG - biasAccX) * INVERT_X
        val rawAccYTemp = (ayG - biasAccY) * INVERT_Y
        
        // === Позиция из УГЛОВ (альтернативный режим) ===
        val rawAngleX = (angleXDeg - biasAngleX) * INVERT_X
        val rawAngleY = (angleYDeg - biasAngleY) * INVERT_Y
        
        val filtAngleX = hpfAngleX.update(rawAngleX, dt)
        val filtAngleY = hpfAngleY.update(rawAngleY, dt)
        
        val posFromAngleX = filtAngleX * ANGLE_TO_MM
        val posFromAngleY = filtAngleY * ANGLE_TO_MM

        // === НОВЫЙ АЛГОРИТМ: Spring-Damper модель ===
        // Вычитаем bias (инверсия уже применена выше к rawAccXTemp/rawAccYTemp)
        
        // Опционально: обмен осей X и Y
        // Если SWAP_XY_AXES = true, то ось X сенсора становится нашей Y и наоборот
        val rawAccX = if (SWAP_XY_AXES) rawAccYTemp else rawAccXTemp
        val rawAccY = if (SWAP_XY_AXES) rawAccXTemp else rawAccYTemp
        
        // Band-pass фильтрация:
        // 1. LPF - убираем высокочастотный шум
        val smoothAccX = lpfAccX.update(rawAccX, dt)
        val smoothAccY = lpfAccY.update(rawAccY, dt)
        
        // 4. ДЕКОРРЕЛЯЦИЯ ВЫКЛЮЧЕНА для ДЕМО
        val decorrelatedAccX = smoothAccX 
        val decorrelatedAccY = smoothAccY 
        
        // 2. HPF - убираем медленный дрейф и DC offset
        val hpfAccXVal = hpfAccX.update(decorrelatedAccX, dt)
        val hpfAccYVal = hpfAccY.update(decorrelatedAccY, dt)
        
        // 3. Дополнительный LPF только для X оси (убираем "зубцы")
        val filteredAccX = extraLpfX.update(hpfAccXVal, dt)
        
        // 5. Итоговые ускорения (X_AXIS_SUPPRESSION теперь 1.0)
        val preDeadZoneX = filteredAccX * X_AXIS_SUPPRESSION
        val preDeadZoneY = hpfAccYVal
        
        // 6. DEAD-ZONE: Игнорируем ускорения ниже порога шума
        // Это критически важно для подавления "дрожания" когда телефон неподвижен
        val finalAccX = applyDeadZone(preDeadZoneX, NOISE_THRESHOLD_G)
        val finalAccY = applyDeadZone(preDeadZoneY, NOISE_THRESHOLD_G)
        
        // Ускорение в mm/s²
        val accelMmX = finalAccX * G_TO_MM_S2
        val accelMmY = finalAccY * G_TO_MM_S2

        // ===========================================
        // РЕЗОНАНСНАЯ СИСТЕМА (пружина-масса) - создаёт ОСЦИЛЛЯЦИИ!
        // ===========================================
        val omega = 2.0 * Math.PI * RESONANT_FREQ_HZ  // Натуральная частота в рад/сек
        val zeta = DAMPING_RATIO
        
        // X ось
        val systemAccelX = -omega * omega * positionX - 2.0 * zeta * omega * velocityX + accelMmX
        velocityX += systemAccelX * dt
        positionX += velocityX * dt
        
        // Y ось
        val systemAccelY = -omega * omega * positionY - 2.0 * zeta * omega * velocityY + accelMmY
        velocityY += systemAccelY * dt
        positionY += velocityY * dt
        
        // Для метрик: сохраняем реальную интегрированную позицию
        rawPositionX += velocityX * dt
        rawPositionY += velocityY * dt

        // === Финальные позиции для отображения ===
        val finalPosX = positionX
        val finalPosY = positionY

        // === Усиление чувствительности для мелких движений ===
        val magnitude = sqrt(finalPosX * finalPosX + finalPosY * finalPosY)
        val boost = if (magnitude < SMALL_MOTION_THRESHOLD_MM && magnitude > 0.01) {
            val t = magnitude / SMALL_MOTION_THRESHOLD_MM  // 0..1
            val boostFactor = SENSITIVITY_BOOST * (1.0 - t) + 1.0 * t
            boostFactor
        } else {
            1.0
        }

        val displayX = finalPosX * DISPLAY_SCALE * boost + (finalAccX * ACCEL_BYPASS_FACTOR * (DISPLAY_SCALE / 100.0))
        val displayY = finalPosY * DISPLAY_SCALE * boost + (finalAccY * ACCEL_BYPASS_FACTOR * (DISPLAY_SCALE / 100.0))

        // === Артефакты ===
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val hasArtifact = abs(displayX) > limit || abs(displayY) > limit
        val clampedX = displayX.coerceIn(-limit, limit)
        val clampedY = displayY.coerceIn(-limit, limit)
        
        // === Логи ===
        val significantMotion = abs(rawAccX) > 0.005 || abs(rawAccY) > 0.005
        
        if (sampleCount % 50 == 0 || significantMotion) {
            val marker = if (significantMotion) "⚡" else ""
            Log.d(tag, "$marker#$sampleCount dt=${(dt*1000).toInt()}ms " +
                "rawAcc=(${f4(rawAccX)}g,${f4(rawAccY)}g) " +
                "filtAcc=(${f4(finalAccX)}g,${f4(finalAccY)}g) " +
                "vel=(${f1(velocityX)}, ${f1(velocityY)})mm/s " +
                "pos=(${f1(positionX)}, ${f1(positionY)})mm " +
                "display=(${f1(displayX)}, ${f1(displayY)})mm")
        }

        return MotionState(
            axMm = accelMmX,
            ayMm = accelMmY,
            vxMm = velocityX,
            vyMm = velocityY,
            sxMm = clampedX,
            syMm = clampedY,
            sxMmRaw = rawPositionX,
            syMmRaw = rawPositionY,
            hasArtifact = hasArtifact
        )
    }

    private fun f1(v: Double) = String.format("%.1f", v)
    private fun f2(v: Double) = String.format("%.2f", v)
    private fun f4(v: Double) = String.format("%.4f", v)
    
    /**
     * Dead-zone фильтр: игнорирует значения ниже порога шума.
     * Это критично для подавления дрейфа когда сенсор неподвижен.
     * 
     * Используется "soft" dead-zone с плавным переходом для избежания
     * резких скачков на границе порога.
     */
    private fun applyDeadZone(value: Double, threshold: Double): Double {
        val absValue = abs(value)
        return when {
            absValue < threshold -> 0.0  // Полное подавление шума
            absValue < threshold * 2 -> {
                // Плавный переход от 0 до полного значения
                val t = (absValue - threshold) / threshold  // 0..1
                val sign = if (value >= 0) 1.0 else -1.0
                sign * (absValue - threshold) * t
            }
            else -> value  // Полное значение
        }
    }
    
    /**
     * Low-pass фильтр (IIR 1-го порядка)
     */
    private class LowPassFilter(private val cutoffHz: Double) {
        private var prevY = 0.0
        private var initialized = false
        
        fun reset() {
            prevY = 0.0
            initialized = false
        }
        
        fun update(x: Double, dt: Double): Double {
            if (cutoffHz <= 0) return x
            val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
            val alpha = dt / (rc + dt)
            if (!initialized) {
                prevY = x
                initialized = true
                return x
            }
            prevY += alpha * (x - prevY)
            return prevY
        }
    }
    
    /**
     * High-pass фильтр (IIR 1-го порядка)
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
            if (cutoffHz <= 0) return x
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
