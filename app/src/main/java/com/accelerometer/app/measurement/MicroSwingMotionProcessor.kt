package com.accelerometer.app.measurement

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Процессор движения по алгоритму MicroSwing.
 * 
 * ✅ ОПТИМИЗИРОВАНО для 50 Hz: После настройки RSW=ACC_ONLY и RRATE=50Hz
 * датчик выдаёт стабильно ~50 Hz (как немецкий MicroSwing).
 * 
 * Используем ДВОЙНУЮ ИНТЕГРАЦИЮ ускорения для получения позиции
 * (как в оригинальном MicroSwing) - это обеспечивает четкое отображение
 * движения вперед-назад, вправо-влево.
 * 
 * Алгоритм:
 * 1. Калибровка bias (первые 10 сэмплов = 0.2 сек при 50 Hz)
 * 2. Фильтрация: ТОЛЬКО LPF (шум, 15 Hz) - HPF ОТКЛЮЧЕН (убирал реальное движение)
 * 3. Интеграция: acc → vel → pos (только двойная интеграция, без углов)
 * 4. Drift correction: velocity damping (0.1) + ZUPT (10 mg, 20 кадров) - минимальное затухание
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
        // При 50 Hz: 10 сэмплов = 0.2 сек (достаточно для стабильной калибровки)
        private const val CALIBRATION_SAMPLES = 10
        
        // ===========================================
        // РЕЖИМ ОБРАБОТКИ
        // ===========================================
        // true = использовать углы (стабильно, но менее чувствительно к плоскому движению)
        // false = использовать двойную интеграцию ускорения (чувствительнее, но дрейфует)
        private const val USE_ANGLE_MODE = false
        
        // ===========================================
        // КОНВЕРТАЦИЯ УГОЛ → ММ
        // ===========================================
        // Коэффициент преобразования угла в мм
        // При высоте платформы ~400мм: 1° ≈ 7мм смещения
        private const val ANGLE_TO_MM = 7.0
        
        // ===========================================
        // ФИЗИКА (для режима ускорения)
        // ===========================================
        private const val G_TO_MM_S2 = 9806.65  // 1g = 9806.65 mm/s²
        
        // Velocity damping: каждый кадр скорость *= (1 - DAMPING * dt)
        // УМЕНЬШЕНО для четкого отображения движения (как в немецком MicroSwing)
        // Немецкое устройство использует минимальное damping или вообще без него
        private const val VELOCITY_DAMPING = 0.1  // Минимальное затухание
        
        // ZUPT: если |accel| < порог в течение N кадров, скорость = 0
        // УВЕЛИЧЕН порог - иначе ZUPT сбрасывает скорость слишком часто и убирает движение
        // Немецкое MicroSwing использует ZUPT очень редко или только для коррекции дрейфа
        private const val ZUPT_THRESHOLD_G = 0.01  // 10 mg (увеличен, чтобы не сбрасывать реальное движение)
        private const val ZUPT_FRAMES = 20  // 20 кадров при 50Hz = 400ms (только для длительного покоя)
        
        // ===========================================
        // ФИЛЬТРЫ (оптимизированы для 50 Hz и четкого отображения движения)
        // ===========================================
        // ⚠️ HPF ОТКЛЮЧЕН для ускорения!
        // HPF убирал реальное движение (вперед-назад, вправо-влево)
        // Вместо HPF используем только вычитание bias (калибровка) + LPF (убирание шума)
        // Немецкое MicroSwing использует только вычитание bias, без HPF на ускорение
        private const val ACC_HPF_CUTOFF_HZ = 0.01  // НЕ ИСПОЛЬЗУЕТСЯ (HPF отключен)
        
        // LPF на ускорение: убираем только высокочастотный шум
        // УВЕЛИЧЕН cutoff - пропускаем весь полезный сигнал движения (до 15 Hz)
        private const val ACC_LPF_CUTOFF_HZ = 15.0  // 15 Hz для 50 Hz входа (пропускаем почти весь сигнал движения)
        
        // HPF на угол: убираем медленный дрейф
        private const val ANGLE_HPF_CUTOFF_HZ = 0.05
        
        // ===========================================
        // МАСШТАБ И ЧУВСТВИТЕЛЬНОСТЬ
        // ===========================================
        // Множитель для визуализации
        // УВЕЛИЧЕН для лучшей видимости движения на графике
        private const val DISPLAY_SCALE = 1.5  // Увеличено с 1.0 для лучшей видимости
        
        // Усиление для мелких движений
        // УВЕЛИЧЕНО - нужно четко видеть все движения
        private const val SENSITIVITY_BOOST = 2.5  // Увеличено для лучшей видимости движения
        
        // Порог для активации усиления (мелкие движения усиливаются больше)
        private const val SMALL_MOTION_THRESHOLD_MM = 10.0  // Увеличено - усиление применяется к более широкому диапазону
    }

    // Калибровка
    private var calibrated = false
    private var calibrationSamples = 0
    private var biasAccX = 0.0
    private var biasAccY = 0.0
    private var biasAngleX = 0.0
    private var biasAngleY = 0.0

    // Фильтры для ускорения
    private val hpfAccX = HighPassFilter(ACC_HPF_CUTOFF_HZ)
    private val hpfAccY = HighPassFilter(ACC_HPF_CUTOFF_HZ)
    private val lpfAccX = LowPassFilter(ACC_LPF_CUTOFF_HZ)
    private val lpfAccY = LowPassFilter(ACC_LPF_CUTOFF_HZ)
    
    // Фильтры для углов
    private val hpfAngleX = HighPassFilter(ANGLE_HPF_CUTOFF_HZ)
    private val hpfAngleY = HighPassFilter(ANGLE_HPF_CUTOFF_HZ)
    
    // Интеграция (для режима ускорения)
    private var velocityX = 0.0  // mm/s
    private var velocityY = 0.0
    private var positionX = 0.0  // mm (интегрированная)
    private var positionY = 0.0
    
    // ZUPT
    private var lowAccelFramesX = 0
    private var lowAccelFramesY = 0
    
    // Время
    private var lastTimestamp: Double? = null
    private var sampleCount = 0
    
    // Реальная частота
    var realSampleRateHz: Double = 10.0
        private set

    fun reset() {
        calibrated = false
        calibrationSamples = 0
        biasAccX = 0.0
        biasAccY = 0.0
        biasAngleX = 0.0
        biasAngleY = 0.0
        // HPF не используется для ускорения (отключен для сохранения движения)
        // hpfAccX.reset()  // Не используется
        // hpfAccY.reset()  // Не используется
        lpfAccX.reset()
        lpfAccY.reset()
        hpfAngleX.reset()
        hpfAngleY.reset()
        velocityX = 0.0
        velocityY = 0.0
        positionX = 0.0
        positionY = 0.0
        lowAccelFramesX = 0
        lowAccelFramesY = 0
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
        val invertX = MeasurementConfig.AXIS_INVERT_X
        val invertY = MeasurementConfig.AXIS_INVERT_Y

        // === Позиция из УГЛОВ ===
        val rawAngleX = (angleXDeg - biasAngleX) * invertX
        val rawAngleY = (angleYDeg - biasAngleY) * invertY
        
        // HPF на углы - убираем медленный дрейф
        val filtAngleX = hpfAngleX.update(rawAngleX, dt)
        val filtAngleY = hpfAngleY.update(rawAngleY, dt)
        
        // Конвертируем угол в мм
        val posFromAngleX = filtAngleX * ANGLE_TO_MM
        val posFromAngleY = filtAngleY * ANGLE_TO_MM

        // === Позиция из УСКОРЕНИЯ (двойная интеграция) ===
        // Вычитаем bias (калибровочное смещение) - это убирает постоянную составляющую
        val rawAccX = (axG - biasAccX) * invertX
        val rawAccY = (ayG - biasAccY) * invertY
        
        // Фильтрация: ТОЛЬКО LPF для убирания шума
        // HPF ОТКЛЮЧЕН - он убирал реальное движение (вперед-назад, вправо-влево)
        // Bias уже убран через вычитание, поэтому HPF не нужен
        // Немецкое MicroSwing использует только вычитание bias + легкий LPF
        val smoothAccX = lpfAccX.update(rawAccX, dt)
        val smoothAccY = lpfAccY.update(rawAccY, dt)
        
        // Используем только LPF (без HPF) для сохранения всех движений
        val finalAccX = smoothAccX
        val finalAccY = smoothAccY
        
        // Ускорение в mm/s²
        val accelMmX = finalAccX * G_TO_MM_S2
        val accelMmY = finalAccY * G_TO_MM_S2

        // ZUPT (используем finalAcc для проверки)
        if (abs(finalAccX) < ZUPT_THRESHOLD_G) {
            lowAccelFramesX++
            if (lowAccelFramesX >= ZUPT_FRAMES) velocityX = 0.0
        } else {
            lowAccelFramesX = 0
        }
        
        if (abs(finalAccY) < ZUPT_THRESHOLD_G) {
            lowAccelFramesY++
            if (lowAccelFramesY >= ZUPT_FRAMES) velocityY = 0.0
        } else {
            lowAccelFramesY = 0
        }

        // Интеграция: acc → vel → pos
        velocityX += accelMmX * dt
        velocityY += accelMmY * dt
        
        // Velocity damping
        val dampFactor = 1.0 - VELOCITY_DAMPING * dt
        velocityX *= dampFactor.coerceIn(0.0, 1.0)
        velocityY *= dampFactor.coerceIn(0.0, 1.0)

        positionX += velocityX * dt
        positionY += velocityY * dt
        
        // Position damping (возврат к центру)
        // УБРАНО для четкого отображения движения (как в немецком MicroSwing)
        // Немецкое устройство не использует position damping - движение отображается напрямую
        // positionX *= (1.0 - 0.05 * dt)  // Отключено
        // positionY *= (1.0 - 0.05 * dt)  // Отключено

        // === Выбор источника позиции ===
        // При 50 Hz используем ТОЛЬКО двойную интеграцию ускорения
        // (как в немецком MicroSwing) для четкого отображения движения
        val (finalPosX, finalPosY) = if (USE_ANGLE_MODE) {
            // Режим углов - стабильнее, но менее чувствителен к плоскому движению
            Pair(posFromAngleX, posFromAngleY)
        } else {
            // Режим ускорения - используем ТОЛЬКО двойную интеграцию
            // При 50 Hz дрейф минимален, комбинация с углами не нужна
            // Это обеспечивает четкое отображение движения вперед-назад, вправо-влево
            Pair(positionX, positionY)
        }

        // === Усиление чувствительности для мелких движений ===
        val magnitude = sqrt(finalPosX * finalPosX + finalPosY * finalPosY)
        val boost = if (magnitude < SMALL_MOTION_THRESHOLD_MM && magnitude > 0.1) {
            // Для мелких движений увеличиваем масштаб
            val t = magnitude / SMALL_MOTION_THRESHOLD_MM  // 0..1
            val boostFactor = SENSITIVITY_BOOST * (1.0 - t) + 1.0 * t  // плавный переход
            boostFactor
        } else {
            1.0
        }

        val displayX = finalPosX * DISPLAY_SCALE * boost
        val displayY = finalPosY * DISPLAY_SCALE * boost

        // === Артефакты ===
        val limit = MeasurementConfig.MOTION_POSITION_LIMIT_MM
        val hasArtifact = abs(displayX) > limit || abs(displayY) > limit
        val clampedX = displayX.coerceIn(-limit, limit)
        val clampedY = displayY.coerceIn(-limit, limit)

        // === Логи ===
        // При 50 Hz логируем каждые 50 кадров (раз в секунду)
        if (sampleCount % 50 == 0) {
            Log.d(tag, "#$sampleCount dt=${(dt*1000).toInt()}ms " +
                "rawAcc=(${f4(rawAccX)}g,${f4(rawAccY)}g) " +
                "finalAcc=(${f4(finalAccX)}g,${f4(finalAccY)}g) " +
                "vel=(${f1(velocityX)}, ${f1(velocityY)})mm/s " +
                "pos=(${f1(displayX)}, ${f1(displayY)})mm boost=${f2(boost)}")
        }

        return MotionState(
            axMm = accelMmX,
            ayMm = accelMmY,
            vxMm = velocityX,
            vyMm = velocityY,
            sxMm = clampedX,
            syMm = clampedY,
            sxMmRaw = displayX,
            syMmRaw = displayY,
            hasArtifact = hasArtifact
        )
    }
    
    private fun f1(v: Double) = String.format("%.1f", v)
    private fun f2(v: Double) = String.format("%.2f", v)
    private fun f4(v: Double) = String.format("%.4f", v)
    
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
