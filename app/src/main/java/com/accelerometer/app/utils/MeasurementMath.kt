package com.accelerometer.app.utils

import com.accelerometer.app.data.MeasurementMetrics
import com.accelerometer.app.data.ProcessedSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Набор формул для расчета метрик измерения.
 */
object MeasurementMath {

    const val GRAVITY_MS2 = 9.80665
    const val GRAVITY_MM_S2 = GRAVITY_MS2 * 1000.0  // ускорение свободного падения в мм/с^2

    fun calcStability(samples: List<ProcessedSample>): Double {
        if (samples.size < 2) return 100.0
        
        // Path = сумма длин отрезков траектории (общий пройденный путь в мм)
        // Используем RAW значения (необрезанные) для точного расчёта
        var path = 0.0
        for (i in 1 until samples.size) {
            val dx = samples[i].sxMmRaw - samples[i - 1].sxMmRaw
            val dy = samples[i].syMmRaw - samples[i - 1].syMmRaw
            path += sqrt(dx * dx + dy * dy)
        }
        
        // Формула MicroSwing: Stabilität = (4000 – Path) / 40
        // При Path = 0 → 100%, при Path = 4000 мм → 0%
        // 4000 мм = 4 метра общего пути за тест — это выраженные нарушения баланса
        val stability = (4000.0 - path) / 40.0
        return stability.coerceIn(0.0, 100.0)
    }

    fun calcOscillationFrequency(
        samples: List<ProcessedSample>,
        durationSec: Double,
        amplitudeThresholdMm: Double,
        correctionFactor: Double
    ): Double {
        if (samples.size < 3 || durationSec <= 0.0) return 0.0
        // Используем RAW значения (необрезанные) для точного расчёта
        val ampsX = extractAmplitudes(samples.map { it.sxMmRaw }, amplitudeThresholdMm)
        val ampsY = extractAmplitudes(samples.map { it.syMmRaw }, amplitudeThresholdMm)
        val total = ampsX.size + ampsY.size
        if (total == 0) return 0.0
        return total / durationSec * correctionFactor
    }

    fun calcCoordinationFactor(
        samples: List<ProcessedSample>,
        amplitudeThresholdMm: Double,
        scalingCoefficient: Double
    ): Double {
        if (samples.size < 10) return 0.0
        
        // ═══════════════════════════════════════════════════════════════════
        // АЛГОРИТМ КФ v7 согласно ТЗ MicroSwing:
        // 
        // КФ измеряет ОБЩЕЕ нарушение координации = амплитуды × хаотичность
        // 
        // - Большие ХАОТИЧНЫЕ движения → высокий КФ (плохая координация)
        // - Большие ПЛАВНЫЕ движения → средний КФ
        // - Малые движения → низкий КФ (хорошая координация)
        // 
        // Формула: КФ = Сумма_амплитуд × (базовый_коэф + хаотичность)
        // ═══════════════════════════════════════════════════════════════════
        
        val xValues = samples.map { it.sxMmRaw }
        val yValues = samples.map { it.syMmRaw }
        
        // ─── Извлекаем амплитуды для анализа ───
        val ampsX = extractAmplitudesForKF(xValues, amplitudeThresholdMm)
        val ampsY = extractAmplitudesForKF(yValues, amplitudeThresholdMm)
        val sumAmplitudes = ampsX.sum() + ampsY.sum()
        
        // Если движений практически нет — КФ минимальный
        if (sumAmplitudes < 5.0) return 10.0  // Базовое значение для неподвижного состояния
        
        // ─── Компоненты хаотичности ───
        val irregularityX = calcAmplitudeIrregularity(ampsX)
        val irregularityY = calcAmplitudeIrregularity(ampsY)
        val totalIrregularity = irregularityX + irregularityY
        
        val jitterX = calcJitterRMS(xValues)
        val jitterY = calcJitterRMS(yValues)
        val totalJitter = jitterX + jitterY
        
        val discordance = calcAxisDiscordance(xValues, yValues)
        
        // ─── Нормализованные показатели хаотичности ───
        // irregularityRatio: нерегулярность относительно суммы амплитуд
        val irregularityRatio = totalIrregularity / (sumAmplitudes + 1.0)
        
        // jitterRatio: jitter относительно суммы амплитуд  
        val jitterRatio = totalJitter * 10.0 / (sumAmplitudes + 1.0)
        
        // ─── Коэффициент хаотичности (0..~1.0) ───
        // v7.1: Уменьшены веса для более плавного роста КФ при интенсивных движениях
        // 0 = плавные монотонные движения
        // ~1 = хаотичные разнонаправленные движения
        val chaosCoef = (irregularityRatio * 0.25 + jitterRatio * 0.15 + discordance * 0.15)
            .coerceIn(0.0, 1.0)
        
        // ─── Итоговая формула КФ v7.1 ───
        // КФ = Сумма_амплитуд × (0.4 + хаотичность)
        // При плавных движениях: множитель ≈ 0.4-0.6
        // При хаотичных движениях: множитель до 1.4
        val rawKF = sumAmplitudes * (0.4 + chaosCoef)
        
        return rawKF * scalingCoefficient
    }
    
    /**
     * RMS Jitter — среднеквадратичное отклонение изменений позиции
     */
    private fun calcJitterRMS(values: List<Double>): Double {
        if (values.size < 3) return 0.0
        val deltas = values.zipWithNext().map { (a, b) -> b - a }
        val squaredSum = deltas.sumOf { it * it }
        return sqrt(squaredSum / deltas.size)
    }
    
    /**
     * Нерегулярность амплитуд по формуле MicroSwing: Σ|A(n) - A(n+1)|
     * Монотонные движения (одинаковые амплитуды) → низкое значение
     * Хаотичные движения (разные амплитуды) → высокое значение
     */
    private fun calcAmplitudeIrregularity(amplitudes: List<Double>): Double {
        if (amplitudes.size < 2) return 0.0
        return amplitudes.zipWithNext().sumOf { (a, b) -> abs(a - b) }
    }
    
    /**
     * Нормализованный Jerk — резкость изменений позиции
     * Возвращает значение 0-1, где 1 = максимальная резкость
     */
    private fun calcNormalizedJerk(values: List<Double>): Double {
        if (values.size < 4) return 0.0
        
        // Вычисляем "ускорение" (вторую производную)
        val velocities = values.zipWithNext().map { (a, b) -> b - a }
        val accelerations = velocities.zipWithNext().map { (a, b) -> abs(b - a) }
        
        if (accelerations.isEmpty()) return 0.0
        
        val meanAcc = accelerations.average()
        val maxAcc = accelerations.maxOrNull() ?: 0.0
        
        // Нормализуем — отношение среднего к максимальному
        // Высокое значение = много резких изменений
        return if (maxAcc > 0.1) (meanAcc / maxAcc).coerceIn(0.0, 1.0) else 0.0
    }
    
    /**
     * Несогласованность осей X и Y
     * Если движения согласованы (например, по диагонали) → низкий discordance
     * Если оси "пляшут" независимо → высокий discordance
     */
    private fun calcAxisDiscordance(xValues: List<Double>, yValues: List<Double>): Double {
        if (xValues.size < 3 || yValues.size < 3) return 0.0
        
        // Изменения по каждой оси
        val deltaX = xValues.zipWithNext().map { (a, b) -> b - a }
        val deltaY = yValues.zipWithNext().map { (a, b) -> b - a }
        
        if (deltaX.isEmpty() || deltaY.isEmpty()) return 0.0
        
        // Корреляция изменений
        val meanDx = deltaX.average()
        val meanDy = deltaY.average()
        
        var covXY = 0.0
        var varX = 0.0
        var varY = 0.0
        
        for (i in deltaX.indices) {
            val dx = deltaX[i] - meanDx
            val dy = deltaY[i] - meanDy
            covXY += dx * dy
            varX += dx * dx
            varY += dy * dy
        }
        
        // Если мало вариации — движения слишком маленькие, считаем согласованными
        if (varX < 1.0 || varY < 1.0) return 0.0
        
        val correlation = covXY / (sqrt(varX) * sqrt(varY))
        
        // Discordance = 1 - |correlation|
        // Высокая корреляция (согласованные движения) → низкий discordance
        // Низкая корреляция (хаотичные движения) → высокий discordance
        return 1.0 - abs(correlation).coerceIn(0.0, 1.0)
    }

    fun buildMetrics(
        samples: List<ProcessedSample>,
        durationSec: Double,
        amplitudeThresholdMmFreq: Double,
        amplitudeThresholdMmCoord: Double,
        correctionFactor: Double,
        scalingCoefficient: Double
    ): MeasurementMetrics {
        if (samples.isEmpty()) return MeasurementMetrics()
        val stability = calcStability(samples)
        val frequency = calcOscillationFrequency(samples, durationSec, amplitudeThresholdMmFreq, correctionFactor)
        val coordination = calcCoordinationFactor(samples, amplitudeThresholdMmCoord, scalingCoefficient)
        return MeasurementMetrics(stability, frequency, coordination)
    }

    /**
     * Извлекает амплитуды для расчёта КФ с улучшенной фильтрацией шума.
     * 
     * Отличия от extractAmplitudes для частоты:
     * 1. Более строгая фильтрация мелких колебаний (шума)
     * 2. Минимальный размах между пиком и впадиной должен быть значительным
     * 3. Фильтрация по времени между экстремумами (минимум 3 сэмпла)
     */
    private fun extractAmplitudesForKF(values: List<Double>, thresholdMm: Double): List<Double> {
        if (values.size < 5) return emptyList()
        
        // Шаг 1: Найти все экстремумы (пики и впадины)
        val extrema = mutableListOf<Triple<Double, Int, Boolean>>() // value, index, isPeak
        var prev = values[0]
        var curr = values[1]
        var prevSlope = curr - prev
        
        for (i in 2 until values.size) {
            val next = values[i]
            val slope = next - curr
            val isPeak = prevSlope > 0 && slope <= 0
            val isValley = prevSlope < 0 && slope >= 0
            
            if (isPeak || isValley) {
                extrema += Triple(curr, i - 1, isPeak)
            }
            prev = curr
            curr = next
            prevSlope = slope
        }
        
        if (extrema.size < 2) return emptyList()
        
        // Шаг 2: Фильтрация - оставляем только значительные экстремумы
        // Минимум 3 сэмпла (60 мс при 50 Гц) между экстремумами
        val minInterval = 3
        // Минимальный размах между пиком и впадиной
        val minSwing = thresholdMm * 1.5  // 1.5x порог для баланса между шумом и чувствительностью
        // Максимальная амплитуда, которая учитывается (ограничение сверху)
        // Большие амплитуды "обрезаются" - это соответствует поведению немецкой системы
        val maxAmplitude = 30.0  // мм - амплитуды больше этого учитываются как 30 мм
        
        val filteredExtrema = mutableListOf<Triple<Double, Int, Boolean>>()
        filteredExtrema += extrema[0]
        
        for (i in 1 until extrema.size) {
            val (prevValue, prevIndex, prevIsPeak) = filteredExtrema.last()
            val (currValue, currIndex, currIsPeak) = extrema[i]
            
            // Проверяем интервал по времени
            if (currIndex - prevIndex < minInterval) continue
            
            // Проверяем, что это чередование пик-впадина
            if (prevIsPeak == currIsPeak) {
                // Два пика или две впадины подряд - оставляем более экстремальную
                val shouldReplace = if (prevIsPeak) currValue > prevValue else currValue < prevValue
                if (shouldReplace) {
                    filteredExtrema[filteredExtrema.lastIndex] = extrema[i]
                }
                continue
            }
            
            // Проверяем минимальный размах
            val swing = abs(currValue - prevValue)
            if (swing < minSwing) continue
            
            filteredExtrema += extrema[i]
        }
        
        if (filteredExtrema.size < 2) return emptyList()
        
        // Шаг 3: Вычисляем амплитуды (размахи между пиками и впадинами)
        // Применяем ограничение сверху (cap) для больших амплитуд
        val amplitudes = mutableListOf<Double>()
        for (i in 0 until filteredExtrema.size - 1) {
            val (value1, _, _) = filteredExtrema[i]
            val (value2, _, _) = filteredExtrema[i + 1]
            val amplitude = abs(value1 - value2)
            if (amplitude >= thresholdMm) {
                // Ограничиваем большие амплитуды - это соответствует поведению MicroSwing
                amplitudes += amplitude.coerceAtMost(maxAmplitude)
            }
        }
        
        return amplitudes
    }

    private fun extractAmplitudes(values: List<Double>, thresholdMm: Double): List<Double> {
        if (values.size < 3) return emptyList()
        val extrema = mutableListOf<Pair<Double, Int>>() // value + индекс в массиве
        var prev = values[0]
        var curr = values[1]
        var prevSlope = curr - prev
        for (i in 2 until values.size) {
            val next = values[i]
            val slope = next - curr
            val isPeak = prevSlope > 0 && slope <= 0
            val isValley = prevSlope < 0 && slope >= 0
            val magnitude = abs(curr)
            if ((isPeak || isValley) && magnitude >= thresholdMm) {
                extrema += curr to i
            }
            prev = curr
            curr = next
            prevSlope = slope
        }

        if (extrema.size < 2) return emptyList()
        
        // Фильтруем по минимальному интервалу (2-3 отсчёта = 40-60 мс при 50 Гц)
        // Согласно ТЗ: между двумя амплитудами должно пройти минимум 2-3 отсчёта
        val minInterval = 2 // минимум 2 отсчёта (40 мс)
        val filteredExtrema = mutableListOf<Pair<Double, Int>>()
        filteredExtrema += extrema[0]
        
        for (i in 1 until extrema.size) {
            val (_, prevIndex) = filteredExtrema.last()
            val (value, currIndex) = extrema[i]
            if (currIndex - prevIndex >= minInterval) {
                filteredExtrema += value to currIndex
            }
        }
        
        if (filteredExtrema.size < 2) return emptyList()
        
        // Вычисляем амплитуды (чередование максимумов и минимумов)
        val amplitudes = mutableListOf<Double>()
        for (i in 0 until filteredExtrema.size - 1) {
            val (value1, _) = filteredExtrema[i]
            val (value2, _) = filteredExtrema[i + 1]
            val amplitude = abs(value1 - value2)
            if (amplitude >= thresholdMm) {
                amplitudes += amplitude
            }
        }
        return amplitudes
    }
}

