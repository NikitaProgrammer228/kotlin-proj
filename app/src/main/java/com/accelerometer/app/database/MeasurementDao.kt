package com.accelerometer.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMeasurementsByUser(userId: Long): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): Measurement?

    @Query("SELECT * FROM measurements WHERE userId = :userId AND foot = :foot ORDER BY testNumber ASC")
    suspend fun getMeasurementsByUserAndFoot(userId: Long, foot: FootSide): List<Measurement>

    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMeasurements(userId: Long, limit: Int = 10): List<Measurement>

    @Insert
    suspend fun insertMeasurement(measurement: Measurement): Long

    @Delete
    suspend fun deleteMeasurement(measurement: Measurement)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)

    @Query("DELETE FROM measurements WHERE userId = :userId")
    suspend fun deleteMeasurementsByUser(userId: Long)
}

