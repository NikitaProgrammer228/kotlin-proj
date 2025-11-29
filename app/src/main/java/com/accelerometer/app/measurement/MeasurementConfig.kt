package com.accelerometer.app.measurement

object MeasurementConfig {
    const val MEASUREMENT_DURATION_SEC = 10.0
    const val CHART_AXIS_RANGE_MM = 50.0  // Диапазон графика в мм (±50)

    const val ENABLE_DEBUG_LOGS = true

    // Частота опроса датчика (MicroSwing использует 50 Гц)
    const val EXPECTED_SAMPLE_RATE_HZ = 50.0
    
    // Калибровка — время для определения базового положения
    const val MOTION_CALIBRATION_DURATION_SEC = 0.5
    
    // High-pass filter alpha: убирает медленный дрейф углов
    // 0.98 ≈ cutoff ~0.32 Hz при 50 Hz
    const val HIGH_PASS_ALPHA = 0.98
    
    // Коэффициент: градусы → миллиметры
    // Подбирается эмпирически для соответствия MicroSwing
    // Уменьшено, чтобы большие углы не давали артефакты
    const val ANGLE_TO_MM_SCALE = 1.5
    
    // Максимальное смещение в мм (согласно ТЗ: артефакт при > ±40 мм)
    const val MOTION_POSITION_LIMIT_MM = 40.0

    // Параметры для расчёта метрик
    const val AMPLITUDE_THRESHOLD_MM = 2.0
    const val OSCILLATION_CORRECTION = 1.0
    const val COORDINATION_SCALE = 1.0
}

