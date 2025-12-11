package com.accelerometer.app.service

import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.database.FootSide
import com.accelerometer.app.database.Measurement
import com.accelerometer.app.data.MeasurementResult
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с измерениями в базе данных
 */
class MeasurementRepository(private val database: AppDatabase) {
    
    suspend fun saveMeasurement(
        userId: Long,
        result: MeasurementResult,
        isValid: Boolean,
        validationMessage: String?,
        foot: FootSide? = null,
        testNumber: Int? = null
    ): Long {
        val measurement = Measurement(
            userId = userId,
            durationSec = result.durationSec,
            metrics = result.metrics,
            samples = result.samples,
            isValid = isValid,
            validationMessage = validationMessage,
            foot = foot,
            testNumber = testNumber
        )
        return database.measurementDao().insertMeasurement(measurement)
    }
    
    fun getMeasurementsByUser(userId: Long): Flow<List<Measurement>> {
        return database.measurementDao().getMeasurementsByUser(userId)
    }
    
    suspend fun getMeasurementById(id: Long): Measurement? {
        return database.measurementDao().getMeasurementById(id)
    }
    
    suspend fun getMeasurementsByUserAndFoot(userId: Long, foot: FootSide): List<Measurement> {
        return database.measurementDao().getMeasurementsByUserAndFoot(userId, foot)
    }
    
    suspend fun getRecentMeasurements(userId: Long, limit: Int = 10): List<Measurement> {
        return database.measurementDao().getRecentMeasurements(userId, limit)
    }
    
    suspend fun deleteMeasurement(id: Long) {
        database.measurementDao().deleteMeasurementById(id)
    }
}

