package com.accelerometer.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Модель пользователя для локального хранения
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

