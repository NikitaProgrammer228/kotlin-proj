package com.accelerometer.app.database

import androidx.room.TypeConverter
import com.accelerometer.app.data.MeasurementMetrics
import com.accelerometer.app.data.ProcessedSample
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Конвертеры для Room Database для сохранения сложных типов
 */
class MeasurementConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromMeasurementMetrics(metrics: MeasurementMetrics): String {
        return gson.toJson(metrics)
    }

    @TypeConverter
    fun toMeasurementMetrics(json: String): MeasurementMetrics {
        return gson.fromJson(json, MeasurementMetrics::class.java)
    }

    @TypeConverter
    fun fromProcessedSampleList(samples: List<ProcessedSample>): String {
        return gson.toJson(samples)
    }

    @TypeConverter
    fun toProcessedSampleList(json: String): List<ProcessedSample> {
        val listType = object : TypeToken<List<ProcessedSample>>() {}.type
        return gson.fromJson(json, listType)
    }

    @TypeConverter
    fun fromFootSide(foot: FootSide?): String? {
        return foot?.name
    }

    @TypeConverter
    fun toFootSide(name: String?): FootSide? {
        return name?.let { FootSide.valueOf(it) }
    }
}

