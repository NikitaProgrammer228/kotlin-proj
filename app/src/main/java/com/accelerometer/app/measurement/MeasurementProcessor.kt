package com.accelerometer.app.measurement

import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.utils.MeasurementMath
import kotlin.math.max

class MeasurementProcessor(
    private val calibrationDurationSec: Double = MeasurementConfig.CALIBRATION_DURATION_SEC,
    private val minDt: Double = 0.002,
    private val maxDt: Double = 0.05
) {

    private var calibrationStart: Double? = null
    private var measurementStart: Double? = null
    private var lastTimestamp: Double? = null
    private var calibrationSamples = 0
    private var biasAccumulatorX = 0.0
    private var biasAccumulatorY = 0.0
    private var biasX = 0.0
    private var biasY = 0.0
    private var vx = 0.0
    private var vy = 0.0
    private var sx = 0.0
    private var sy = 0.0
    private var avgDt = 0.01
    private var prevRawAx = 0.0
    private var prevRawAy = 0.0
    private var prevHpAx = 0.0
    private var prevHpAy = 0.0
    private var prevLpAx = 0.0
    private var prevLpAy = 0.0
    private var velocityBiasX = 0.0
    private var velocityBiasY = 0.0
    private var calibrationFinished = false

    fun reset() {
        calibrationStart = null
        measurementStart = null
        lastTimestamp = null
        calibrationSamples = 0
        biasAccumulatorX = 0.0
        biasAccumulatorY = 0.0
        biasX = 0.0
        biasY = 0.0
        vx = 0.0
        vy = 0.0
        sx = 0.0
        sy = 0.0
        avgDt = 0.01
        prevRawAx = 0.0
        prevRawAy = 0.0
        prevHpAx = 0.0
        prevHpAy = 0.0
        prevLpAx = 0.0
        prevLpAy = 0.0
        velocityBiasX = 0.0
        velocityBiasY = 0.0
        calibrationFinished = false
    }

    fun process(sample: SensorSample): ProcessedSample? {
        val timestamp = sample.timestampSec
        if (calibrationStart == null) {
            calibrationStart = timestamp
        }

        if (!calibrationFinished) {
            accumulateBias(sample)
            val elapsed = timestamp - (calibrationStart ?: timestamp)
            if (elapsed >= calibrationDurationSec) {
                finalizeCalibration()
                measurementStart = timestamp
                lastTimestamp = timestamp
            }
            return null
        }

        val start = measurementStart ?: run {
            measurementStart = timestamp
            lastTimestamp = timestamp
            timestamp
        }

        val prevTimestamp = lastTimestamp ?: timestamp
        var dt = timestamp - prevTimestamp
        if (dt <= 0) dt = minDt
        if (dt > maxDt) dt = maxDt
        lastTimestamp = timestamp

        avgDt = avgDt + (dt - avgDt) * MeasurementConfig.DT_SMOOTHING
        val normalizedDt = avgDt.coerceIn(minDt, maxDt)

        val axMmRaw = MeasurementMath.rawToAccMm(sample.rawAx) - biasX
        val ayMmRaw = MeasurementMath.rawToAccMm(sample.rawAy) - biasY

        val filteredAx = applyFilters(axMmRaw, Axis.X, normalizedDt)
        val filteredAy = applyFilters(ayMmRaw, Axis.Y, normalizedDt)

        vx += filteredAx * normalizedDt
        vy += filteredAy * normalizedDt

        velocityBiasX = velocityBiasX + (vx - velocityBiasX) * 0.001
        velocityBiasY = velocityBiasY + (vy - velocityBiasY) * 0.001
        vx -= velocityBiasX
        vy -= velocityBiasY

        sx += vx * normalizedDt
        sy += vy * normalizedDt

        val elapsed = timestamp - start
        return ProcessedSample(
            t = elapsed,
            axMm = filteredAx,
            ayMm = filteredAy,
            vxMm = vx,
            vyMm = vy,
            sxMm = sx,
            syMm = sy
        )
    }

    fun isCalibrating(): Boolean = !calibrationFinished

    private fun accumulateBias(sample: SensorSample) {
        val ax = MeasurementMath.rawToAccMm(sample.rawAx)
        val ay = MeasurementMath.rawToAccMm(sample.rawAy)
        biasAccumulatorX += ax
        biasAccumulatorY += ay
        calibrationSamples++
    }

    private fun finalizeCalibration() {
        if (calibrationSamples == 0) return
        biasX = biasAccumulatorX / max(calibrationSamples, 1)
        biasY = biasAccumulatorY / max(calibrationSamples, 1)
        calibrationFinished = true
        vx = 0.0
        vy = 0.0
        sx = 0.0
        sy = 0.0
    }

    private fun applyFilters(value: Double, axis: Axis, dt: Double): Double {
        val hp = when (axis) {
            Axis.X -> {
                val rc = 1.0 / (2.0 * Math.PI * MeasurementConfig.HIGH_PASS_CUTOFF_HZ)
                val alpha = rc / (rc + dt)
                val output = alpha * (prevHpAx + value - prevRawAx)
                prevRawAx = value
                prevHpAx = output
                output
            }
            Axis.Y -> {
                val rc = 1.0 / (2.0 * Math.PI * MeasurementConfig.HIGH_PASS_CUTOFF_HZ)
                val alpha = rc / (rc + dt)
                val output = alpha * (prevHpAy + value - prevRawAy)
                prevRawAy = value
                prevHpAy = output
                output
            }
        }

        val rc = 1.0 / (2.0 * Math.PI * MeasurementConfig.LOW_PASS_CUTOFF_HZ)
        val alpha = dt / (rc + dt)

        return when (axis) {
            Axis.X -> {
                prevLpAx += alpha * (hp - prevLpAx)
                prevLpAx
            }
            Axis.Y -> {
                prevLpAy += alpha * (hp - prevLpAy)
                prevLpAy
            }
        }
    }

    private enum class Axis { X, Y }
}

