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
        var sum = 0.0
        for (i in 1 until samples.size) {
            val dx = samples[i].sxMm - samples[i - 1].sxMm
            val dy = samples[i].syMm - samples[i - 1].syMm
            sum += sqrt(dx * dx + dy * dy)
        }
        val average = sum / samples.size
        val stability = (4000.0 - average) / 40.0
        return stability.coerceIn(0.0, 100.0)
    }

    fun calcOscillationFrequency(
        samples: List<ProcessedSample>,
        durationSec: Double,
        amplitudeThresholdMm: Double,
        correctionFactor: Double
    ): Double {
        if (samples.size < 3 || durationSec <= 0.0) return 0.0
        val ampsX = extractAmplitudes(samples.map { it.sxMm }, amplitudeThresholdMm)
        val ampsY = extractAmplitudes(samples.map { it.syMm }, amplitudeThresholdMm)
        val total = ampsX.size + ampsY.size
        if (total == 0) return 0.0
        return total / durationSec * correctionFactor
    }

    fun calcCoordinationFactor(
        samples: List<ProcessedSample>,
        amplitudeThresholdMm: Double,
        scalingCoefficient: Double
    ): Double {
        if (samples.size < 4) return 0.0
        val ampsX = extractAmplitudes(samples.map { it.sxMm }, amplitudeThresholdMm)
        val ampsY = extractAmplitudes(samples.map { it.syMm }, amplitudeThresholdMm)
        val valueX = amplitudeIrregularity(ampsX)
        val valueY = amplitudeIrregularity(ampsY)
        return (valueX + valueY) * scalingCoefficient
    }

    fun buildMetrics(
        samples: List<ProcessedSample>,
        durationSec: Double,
        amplitudeThresholdMm: Double,
        correctionFactor: Double,
        scalingCoefficient: Double
    ): MeasurementMetrics {
        if (samples.isEmpty()) return MeasurementMetrics()
        val stability = calcStability(samples)
        val frequency = calcOscillationFrequency(samples, durationSec, amplitudeThresholdMm, correctionFactor)
        val coordination = calcCoordinationFactor(samples, amplitudeThresholdMm, scalingCoefficient)
        return MeasurementMetrics(stability, frequency, coordination)
    }

    private fun amplitudeIrregularity(amplitudes: List<Double>): Double {
        if (amplitudes.size < 2) return 0.0
        val sum = amplitudes.zipWithNext().sumOf { abs(it.first - it.second) }
        val denominator = max(amplitudes.size, 1)
        return sum / denominator
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

