package com.accelerometer.app.utils

import com.accelerometer.app.data.MeasurementMetrics
import com.accelerometer.app.data.ProcessedSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Набор формул из разделов 8.1 и 8.3 руководства MicroSwing.
 */
object MeasurementMath {

    private const val RAW_SCALE = 16384.0            // делитель из формулы raw/16384
    private const val GRAVITY_MM = 9.80665 * 1000.0  // ускорение свободного падения в мм/с^2

    fun rawToAccMm(raw: Int): Double = raw / RAW_SCALE * GRAVITY_MM

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
        val extrema = mutableListOf<Pair<Double, Boolean>>() // value + true если максимум
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
                extrema += curr to isPeak
            }
            prev = curr
            curr = next
            prevSlope = slope
        }

        if (extrema.size < 2) return emptyList()
        val amplitudes = mutableListOf<Double>()
        for (i in 0 until extrema.size - 1) {
            val (value1, isPeak1) = extrema[i]
            val (value2, isPeak2) = extrema[i + 1]
            if (isPeak1 == isPeak2) continue
            val amplitude = abs(value1 - value2)
            if (amplitude >= thresholdMm) {
                amplitudes += amplitude
            }
        }
        return amplitudes
    }
}

