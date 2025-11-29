package com.accelerometer.app.measurement

import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample

class MeasurementProcessor(
    private val motionProcessor: MicroSwingMotionProcessor = MicroSwingMotionProcessor(
        expectedSampleRateHz = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
    )
) {

    private var measurementStart: Double? = null

    fun reset() {
        measurementStart = null
        motionProcessor.reset()
    }

    fun process(sample: SensorSample): ProcessedSample {
        val timestamp = sample.timestampSec
        if (measurementStart == null) {
            measurementStart = timestamp
        }

        // Передаём ускорение напрямую в g (SDK уже возвращает значения в g)
        val state = motionProcessor.processSample(
            axG = sample.accXg,
            ayG = sample.accYg,
            azG = sample.accZg,
            angleXDeg = sample.angleXDeg,
            angleYDeg = sample.angleYDeg,
            timestampSec = timestamp
        )

        val elapsed = timestamp - (measurementStart ?: timestamp)

        return ProcessedSample(
            t = elapsed,
            axMm = state.axMm,
            ayMm = state.ayMm,
            vxMm = state.vxMm,
            vyMm = state.vyMm,
            sxMm = state.sxMm,
            syMm = state.syMm,
            hasArtifact = state.hasArtifact
        )
    }

}

