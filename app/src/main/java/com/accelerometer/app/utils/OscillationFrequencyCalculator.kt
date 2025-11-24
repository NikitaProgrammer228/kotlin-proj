package com.accelerometer.app.utils

import com.accelerometer.app.data.AccelerometerData

/**
 * Калькулятор частоты колебаний на основе формулы из MicroSwing® 6
 * 
 * Формула: Oscillation frequency = (NumberOfAmplitudes X-Direction + NumberOfAmplitudes Y-Direction) / Time × Correction factor
 * 
 * Для упрощения используем подсчет пересечений нулевой линии (zero-crossings) как амплитуд
 */
object OscillationFrequencyCalculator {
    
    private const val CORRECTION_FACTOR = 1.0f // Корректирующий коэффициент (можно настроить)
    
    /**
     * Вычисляет частоту колебаний на основе данных акселерометра
     * @param dataPoints список точек данных
     * @param timeInSeconds время измерения в секундах
     * @return частота колебаний в Гц
     */
    fun calculate(dataPoints: List<AccelerometerData>, timeInSeconds: Float): Float {
        if (dataPoints.isEmpty() || timeInSeconds <= 0) {
            return 0f
        }
        
        // Подсчитываем количество амплитуд (пересечений нулевой линии) для оси X
        val amplitudesX = countZeroCrossings(dataPoints.map { it.x })
        
        // Подсчитываем количество амплитуд для оси Y
        val amplitudesY = countZeroCrossings(dataPoints.map { it.y })
        
        // Вычисляем частоту по формуле
        val totalAmplitudes = amplitudesX + amplitudesY
        val frequency = (totalAmplitudes / timeInSeconds) * CORRECTION_FACTOR
        
        return frequency
    }
    
    /**
     * Подсчитывает количество пересечений нулевой линии (zero-crossings)
     * Это используется как мера количества амплитуд
     */
    private fun countZeroCrossings(values: List<Float>): Int {
        if (values.size < 2) return 0
        
        var crossings = 0
        var previousSign = values[0] >= 0
        
        for (i in 1 until values.size) {
            val currentSign = values[i] >= 0
            if (currentSign != previousSign) {
                crossings++
                previousSign = currentSign
            }
        }
        
        // Количество амплитуд = количество пересечений / 2
        return crossings / 2
    }
    
    /**
     * Альтернативный метод: подсчет амплитуд через локальные экстремумы
     */
    fun calculateByExtremes(dataPoints: List<AccelerometerData>, timeInSeconds: Float): Float {
        if (dataPoints.size < 3 || timeInSeconds <= 0) {
            return 0f
        }
        
        val amplitudesX = countAmplitudesByExtremes(dataPoints.map { it.x })
        val amplitudesY = countAmplitudesByExtremes(dataPoints.map { it.y })
        
        val totalAmplitudes = amplitudesX + amplitudesY
        val frequency = (totalAmplitudes / timeInSeconds) * CORRECTION_FACTOR
        
        return frequency
    }
    
    /**
     * Подсчитывает амплитуды через поиск локальных максимумов и минимумов
     */
    private fun countAmplitudesByExtremes(values: List<Float>): Int {
        if (values.size < 3) return 0
        
        var amplitudes = 0
        var wasIncreasing = values[1] > values[0]
        
        for (i in 2 until values.size) {
            val isIncreasing = values[i] > values[i - 1]
            
            // Обнаружение изменения направления (экстремум)
            if (isIncreasing != wasIncreasing) {
                amplitudes++
                wasIncreasing = isIncreasing
            }
        }
        
        return amplitudes / 2
    }
}

