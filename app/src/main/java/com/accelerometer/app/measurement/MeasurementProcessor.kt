package com.accelerometer.app.measurement

import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.utils.MeasurementMath
import kotlin.math.abs
import kotlin.math.max

class MeasurementProcessor(
    private val calibrationDurationSec: Double = MeasurementConfig.CALIBRATION_DURATION_SEC,
    private val minDt: Double = 0.002,
    private val maxDt: Double = 0.05,
    private val noiseThresholdMmS2: Double = MeasurementConfig.ACC_NOISE_THRESHOLD_MM_S2
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
    private var prevAx = 0.0
    private var prevAy = 0.0
    private var prevVx = 0.0
    private var prevVy = 0.0
    private var hasPreviousSample = false
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
        prevAx = 0.0
        prevAy = 0.0
        prevVx = 0.0
        prevVy = 0.0
        hasPreviousSample = false
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
                finalizeCalibration(timestamp)
            }
            return null
        }

        val dt = computeDt(timestamp)
        val axMm = applyNoiseFloor(MeasurementMath.rawToAccMm(sample.rawAx) - biasX)
        val ayMm = applyNoiseFloor(MeasurementMath.rawToAccMm(sample.rawAy) - biasY)

        if (!hasPreviousSample) {
            prevAx = axMm
            prevAy = ayMm
            prevVx = vx
            prevVy = vy
            hasPreviousSample = true
        }

        val avgAx = 0.5 * (axMm + prevAx)
        val avgAy = 0.5 * (ayMm + prevAy)
        vx += avgAx * dt
        vy += avgAy * dt

        val avgVx = 0.5 * (vx + prevVx)
        val avgVy = 0.5 * (vy + prevVy)
        sx += avgVx * dt
        sy += avgVy * dt

        prevAx = axMm
        prevAy = ayMm
        prevVx = vx
        prevVy = vy

        val elapsed = timestamp - (measurementStart ?: timestamp)
        return ProcessedSample(
            t = elapsed,
            axMm = axMm,
            ayMm = ayMm,
            vxMm = vx,
            vyMm = vy,
            sxMm = sx,
            syMm = sy
        )
    }

    fun isCalibrating(): Boolean = !calibrationFinished

    private fun accumulateBias(sample: SensorSample) {
        biasAccumulatorX += MeasurementMath.rawToAccMm(sample.rawAx)
        biasAccumulatorY += MeasurementMath.rawToAccMm(sample.rawAy)
        calibrationSamples++
    }

    private fun finalizeCalibration(timestamp: Double) {
        if (calibrationSamples == 0) return
        biasX = biasAccumulatorX / max(calibrationSamples, 1)
        biasY = biasAccumulatorY / max(calibrationSamples, 1)
        calibrationFinished = true
        measurementStart = timestamp
        lastTimestamp = timestamp
        vx = 0.0
        vy = 0.0
        sx = 0.0
        sy = 0.0
        prevAx = 0.0
        prevAy = 0.0
        prevVx = 0.0
        prevVy = 0.0
        hasPreviousSample = false
    }

    private fun computeDt(timestamp: Double): Double {
        val prev = lastTimestamp
        lastTimestamp = timestamp
        return if (prev == null) {
            minDt
        } else {
            (timestamp - prev).coerceIn(minDt, maxDt)
        }
    }

    private fun applyNoiseFloor(value: Double): Double =
        if (abs(value) < noiseThresholdMmS2) 0.0 else value
}

