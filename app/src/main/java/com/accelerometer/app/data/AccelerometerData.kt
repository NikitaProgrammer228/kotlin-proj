package com.accelerometer.app.data

import java.util.Date

/**
 * Данные с акселерометра по осям X и Y
 */
data class AccelerometerData(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toPoint(): Point = Point(x, y)
}

/**
 * Точка в 2D пространстве
 */
data class Point(
    val x: Float,
    val y: Float
)

/**
 * Результаты измерений
 */
data class MeasurementResult(
    val stability: Float,
    val oscillationFrequency: Float,
    val dataPoints: List<AccelerometerData>,
    val measurementTime: Long // в секундах
)

