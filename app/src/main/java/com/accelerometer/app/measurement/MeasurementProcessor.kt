package com.accelerometer.app.measurement

import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample
import com.accelerometer.app.utils.MeasurementMath
import kotlin.math.max

class MeasurementProcessor(
    private val calibrationDurationSec: Double = 2.0,
    private val minDt: Double = 0.005,
    private val maxDt: Double = 0.1
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

        val axMm = MeasurementMath.rawToAccMm(sample.rawAx) - biasX
        val ayMm = MeasurementMath.rawToAccMm(sample.rawAy) - biasY

        vx += axMm * dt
        sx += vx * dt

        vy += ayMm * dt
        sy += vy * dt

        val elapsed = timestamp - start
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
}

