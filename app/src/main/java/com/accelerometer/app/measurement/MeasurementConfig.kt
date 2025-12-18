package com.accelerometer.app.measurement

/**
 * Конфигурация измерений.
 * 
 * ✅ ОПТИМИЗИРОВАНО: После настройки RSW=ACC_ONLY и RRATE=50Hz
 * датчик WT901BLE выдаёт стабильно ~50 Hz (вместо 10 Hz).
 * 
 * Немецкий MicroSwing работает на 50 Hz через проводное USB/Serial подключение.
 * Теперь наша система работает на той же частоте для точного соответствия.
 */
object MeasurementConfig {
    const val MEASUREMENT_DURATION_SEC = 10.0
    const val CHART_AXIS_RANGE_MM = 50.0  // Диапазон графика в мм (±50)

    const val ENABLE_DEBUG_LOGS = true

    // ═══════════════════════════════════════════════════════════════════
    // ЧАСТОТА И КАЛИБРОВКА
    // ═══════════════════════════════════════════════════════════════════
    // ✅ ОПТИМИЗИРОВАНО: После настройки RSW=ACC_ONLY и RRATE=50Hz
    // датчик выдаёт стабильно ~50 Hz (вместо 10 Hz)
    // Немецкий MicroSwing использует 50 Hz через ПРОВОДНОЕ подключение.
    const val EXPECTED_SAMPLE_RATE_HZ = 50.0
    
    // Калибровка bias (среднее ускорение в покое)
    // При ENABLE_CALIBRATION = true: накапливаем N секунд для усреднения
    // При ENABLE_CALIBRATION = false: используем первый сэмпл как bias
    const val MOTION_CALIBRATION_DURATION_SEC = 0.2  // 0.2 сек = 10 сэмплов при 50Hz (достаточно)
    const val ENABLE_CALIBRATION = false  // Для быстрого старта
    
    // Стабилизация после калибровки (прогрев фильтров)
    const val STABILIZATION_DURATION_SEC = 0.0  // Отключена

    // ═══════════════════════════════════════════════════════════════════
    // ИНВЕРСИЯ ОСЕЙ
    // ═══════════════════════════════════════════════════════════════════
    // 1.0 = как есть, -1.0 = переворот знака
    const val AXIS_INVERT_X = 1.0
    const val AXIS_INVERT_Y = -1.0  // Инвертируем Y чтобы совпасть с немецким ПО

    // ═══════════════════════════════════════════════════════════════════
    // ОГРАНИЧЕНИЯ И МАСШТАБИРОВАНИЕ
    // ═══════════════════════════════════════════════════════════════════
    // Максимальное смещение в мм (артефакт при превышении)
    const val MOTION_POSITION_LIMIT_MM = 40.0

    // Дополнительный визуальный множитель только для мишени (target graph)
    const val TARGET_GRAPH_SCALE = 1.18f
    
    // High-pass фильтр (НЕ ИСПОЛЬЗУЕТСЯ в новом алгоритме, оставлен для совместимости)
    const val HIGH_PASS_ALPHA = 0.86
    
    // Коэффициенты угол→мм (НЕ ИСПОЛЬЗУЮТСЯ в новом алгоритме, оставлены для совместимости)
    const val ANGLE_TO_MM_SCALE_X = 4.3
    const val ANGLE_TO_MM_SCALE_Y = 4.3

    // ═══════════════════════════════════════════════════════════════════
    // ПАРАМЕТРЫ ДЛЯ РАСЧЁТА МЕТРИК
    // ═══════════════════════════════════════════════════════════════════
    // Частота: порог для детекции крупных колебаний
    const val AMPLITUDE_THRESHOLD_MM_FREQ = 3.0
    // Координация: порог для детекции амплитуд
    const val AMPLITUDE_THRESHOLD_MM_COORD = 1.5
    const val AMPLITUDE_THRESHOLD_MM = AMPLITUDE_THRESHOLD_MM_COORD
    // Коррекция частоты
    const val OSCILLATION_CORRECTION = 0.30
    
    // Масштаб координационного фактора
    const val COORDINATION_SCALE_TIME = 0.60
    const val COORDINATION_SCALE_TARGET = 0.60
    const val COORDINATION_SCALE = COORDINATION_SCALE_TIME

    // ═══════════════════════════════════════════════════════════════════
    // АВТОСТАРТ
    // ═══════════════════════════════════════════════════════════════════
    // Порог движения для автостарта (в мм)
    const val AUTOSTART_THRESHOLD_MM = 5.0
}
