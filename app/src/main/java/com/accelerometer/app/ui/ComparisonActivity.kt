package com.accelerometer.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accelerometer.app.database.AppDatabase
import com.accelerometer.app.database.Measurement
import com.accelerometer.app.service.MeasurementRepository
import kotlinx.coroutines.launch
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.view.setPadding
import com.accelerometer.app.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activity для сравнения результатов измерений
 */
class ComparisonActivity : AppCompatActivity() {
    
    private lateinit var repository: MeasurementRepository
    private var userId: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userId = intent.getLongExtra("userId", 0L)
        if (userId == 0L) {
            finish()
            return
        }
        
        repository = MeasurementRepository(AppDatabase.getDatabase(this))
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        
        val title = TextView(this).apply {
            text = "Сравнение тестов"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)
        
        setContentView(layout)
        
        loadAndDisplayComparisons()
    }
    
    private fun loadAndDisplayComparisons() {
        lifecycleScope.launch {
            val measurements = repository.getRecentMeasurements(userId, 10)
            if (measurements.isEmpty()) {
                val emptyText = TextView(this@ComparisonActivity).apply {
                    text = "Нет сохранённых измерений"
                    textSize = 16f
                    setPadding(0, 16, 0, 0)
                }
                (findViewById<LinearLayout>(android.R.id.content) as? LinearLayout)?.addView(emptyText)
                return@launch
            }
            
            displayComparisonTable(measurements)
        }
    }
    
    private fun displayComparisonTable(measurements: List<Measurement>) {
        val layout = findViewById<LinearLayout>(android.R.id.content) as? LinearLayout ?: return
        
        // Заголовок таблицы
        val headerRow = createTableRow(
            listOf("Дата", "Стабильность", "Частота", "Координация", "Длительность"),
            isHeader = true
        )
        layout.addView(headerRow)
        
        // Данные
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        measurements.forEach { measurement ->
            val row = createTableRow(
                listOf(
                    dateFormat.format(java.util.Date(measurement.timestamp)),
                    String.format(Locale.getDefault(), "%.2f", measurement.metrics.stability),
                    String.format(Locale.getDefault(), "%.2f", measurement.metrics.oscillationFrequency),
                    String.format(Locale.getDefault(), "%.2f", measurement.metrics.coordinationFactor),
                    String.format(Locale.getDefault(), "%.1f", measurement.durationSec)
                ),
                isHeader = false,
                isValid = measurement.isValid
            )
            layout.addView(row)
        }
        
        // Средние значения
        if (measurements.isNotEmpty()) {
            val avgStability = measurements.map { it.metrics.stability }.average()
            val avgFrequency = measurements.map { it.metrics.oscillationFrequency }.average()
            val avgCoordination = measurements.map { it.metrics.coordinationFactor }.average()
            
            val avgRow = createTableRow(
                listOf(
                    "Среднее",
                    String.format(Locale.getDefault(), "%.2f", avgStability),
                    String.format(Locale.getDefault(), "%.2f", avgFrequency),
                    String.format(Locale.getDefault(), "%.2f", avgCoordination),
                    ""
                ),
                isHeader = false,
                isAverage = true
            )
            layout.addView(avgRow)
        }
    }
    
    private fun createTableRow(
        values: List<String>,
        isHeader: Boolean = false,
        isValid: Boolean = true,
        isAverage: Boolean = false
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            if (isHeader) {
                setBackgroundColor(0xFF2196F3.toInt())
            } else if (isAverage) {
                setBackgroundColor(0xFFE0E0E0.toInt())
            } else if (!isValid) {
                setBackgroundColor(0xFFFFCDD2.toInt())
            }
        }
        
        values.forEach { value ->
            val textView = TextView(this).apply {
                text = value
                textSize = if (isHeader) 14f else 12f
                setPadding(8, 4, 8, 4)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (isHeader) {
                    setTextColor(0xFFFFFFFF.toInt())
                }
            }
            row.addView(textView)
        }
        
        return row
    }
}

