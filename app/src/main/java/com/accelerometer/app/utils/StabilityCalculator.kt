package com.accelerometer.app.utils

import com.accelerometer.app.data.AccelerometerData

/**
 * Калькулятор стабильности на основе формулы из MicroSwing® 6
 * 
 * Формула: Stability = ( 4000 - ( Σ_{n=2}^{NumberOfValues} √((x_n - x_{n-1})^2 + (y_n - y_{n-1})^2) / NumberOfValues ) ) / 40
 */
object StabilityCalculator {
    
    /**
     * Вычисляет стабильность на основе данных акселерометра
     * @param dataPoints список точек данных
     * @return значение стабильности в процентах (0-100)
     */
    fun calculate(dataPoints: List<AccelerometerData>): Float {
        if (dataPoints.size < 2) {
            // Если нет данных или только одна точка, возвращаем 100% (idle sensor)
            return 100f
        }
        
        var sum = 0f
        val numberOfValues = dataPoints.size
        
        // Вычисляем сумму √((x_n - x_{n-1})^2 + (y_n - y_{n-1})^2) для n от 2 до NumberOfValues
        for (i in 1 until dataPoints.size) {
            val current = dataPoints[i]
            val previous = dataPoints[i - 1]
            
            val deltaX = current.x - previous.x
            val deltaY = current.y - previous.y
            
            // √((x_n - x_{n-1})^2 + (y_n - y_{n-1})^2)
            val distance = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
            sum += distance
        }
        
        // Σ / NumberOfValues
        val average = sum / numberOfValues
        
        // ( 4000 - average ) / 40
        val stability = (4000f - average) / 40f
        
        // Ограничиваем значение от 0 до 100
        return stability.coerceIn(0f, 100f)
    }
}

