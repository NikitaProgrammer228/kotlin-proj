package com.accelerometer.app.measurement

object MeasurementConfig {
    const val MEASUREMENT_DURATION_SEC = 10.0
    const val CALIBRATION_DURATION_SEC = 2.0

    const val HIGH_PASS_CUTOFF_HZ = 0.2
    const val LOW_PASS_CUTOFF_HZ = 5.0

    const val DT_SMOOTHING = 0.05

    const val AMPLITUDE_THRESHOLD_MM = 5.0
    const val OSCILLATION_CORRECTION = 1.0
    const val COORDINATION_SCALE = 1.0
}

