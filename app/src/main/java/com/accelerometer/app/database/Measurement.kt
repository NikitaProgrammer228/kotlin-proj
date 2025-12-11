package com.accelerometer.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.accelerometer.app.data.MeasurementMetrics
import com.accelerometer.app.data.ProcessedSample

/**
 * Результат измерения для сохранения в базе данных
 */
@Entity(tableName = "measurements")
@TypeConverters(MeasurementConverters::class)
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,  // Связь с пользователем
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Double,
    val metrics: MeasurementMetrics,
    val samples: List<ProcessedSample>,
    val isValid: Boolean = true,
    val validationMessage: String? = null,
    val foot: FootSide? = null,  // Для PKT-протокола: правая/левая нога
    val testNumber: Int? = null  // Для PKT-протокола: номер теста (1-20)
)

enum class FootSide {
    LEFT,   // Левая нога
    RIGHT   // Правая нога
}

