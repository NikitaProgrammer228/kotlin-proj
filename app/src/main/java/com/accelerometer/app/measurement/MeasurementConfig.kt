package com.accelerometer.app.measurement

object MeasurementConfig {
    const val MEASUREMENT_DURATION_SEC = 10.0
    const val CHART_AXIS_RANGE_MM = 30.0  // Диапазон графика в мм

    const val ENABLE_DEBUG_LOGS = true

    const val EXPECTED_SAMPLE_RATE_HZ = 50.0
    
    // Калибровка — время для определения "нулевого" положения
    const val MOTION_CALIBRATION_DURATION_SEC = 0.5
    
    // High-pass filter alpha: убирает медленный дрейф
    // Чем ближе к 1, тем ниже частота среза
    // 0.995 ≈ cutoff 0.16 Hz при 50 Hz sample rate
    const val HIGH_PASS_ALPHA = 0.995
    
    // Коэффициент перевода угла в мм
    // Подбирается эмпирически: 1 градус ≈ X мм смещения
    // При типичных колебаниях платформы ±5° это даст ±15-25 мм
    const val ANGLE_TO_MM_SCALE = 5.0
    
    // Максимальное смещение на графике
    const val MOTION_POSITION_LIMIT_MM = 50.0

    const val AMPLITUDE_THRESHOLD_MM = 2.0
    const val OSCILLATION_CORRECTION = 1.0
    const val COORDINATION_SCALE = 1.0
}

