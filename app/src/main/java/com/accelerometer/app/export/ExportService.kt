package com.accelerometer.app.export

import android.content.Context
import android.os.Build
import android.os.Environment
import com.accelerometer.app.data.MeasurementResult
import com.accelerometer.app.data.ProcessedSample
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Сервис для экспорта результатов измерений в различные форматы
 */
class ExportService(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Экспорт в CSV формат
     */
    fun exportToCsv(
        result: MeasurementResult,
        userName: String,
        fileName: String? = null
    ): File? {
        return try {
            val file = createExportFile(fileName ?: "measurement_${dateFormat.format(Date())}", "csv")
            FileWriter(file).use { writer ->
                // Заголовок
                writer.appendLine("Measurement Report")
                writer.appendLine("User: $userName")
                writer.appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.appendLine("Duration: ${String.format(Locale.getDefault(), "%.2f", result.durationSec)}s")
                writer.appendLine("")
                
                // Метрики
                writer.appendLine("Metrics:")
                writer.appendLine("Stability,${String.format(Locale.getDefault(), "%.2f", result.metrics.stability)}")
                writer.appendLine("Oscillation Frequency,${String.format(Locale.getDefault(), "%.4f", result.metrics.oscillationFrequency)} Hz")
                writer.appendLine("Coordination Factor,${String.format(Locale.getDefault(), "%.4f", result.metrics.coordinationFactor)}")
                writer.appendLine("")
                
                // Данные
                writer.appendLine("Time (s),Position X (mm),Position Y (mm),Velocity X (mm/s),Velocity Y (mm/s)")
                result.samples.forEach { sample ->
                    writer.appendLine(
                        "${String.format(Locale.getDefault(), "%.3f", sample.t)}," +
                        "${String.format(Locale.getDefault(), "%.2f", sample.sxMm)}," +
                        "${String.format(Locale.getDefault(), "%.2f", sample.syMm)}," +
                        "${String.format(Locale.getDefault(), "%.2f", sample.vxMm)}," +
                        "${String.format(Locale.getDefault(), "%.2f", sample.vyMm)}"
                    )
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Экспорт в CSV для PKT-протокола (серия тестов)
     */
    fun exportPktToCsv(
        measurements: List<Pair<String, MeasurementResult>>,  // (userName, result)
        footSide: String? = null
    ): File? {
        return try {
            val suffix = if (footSide != null) "_${footSide}" else ""
            val file = createExportFile("pkt_${dateFormat.format(Date())}$suffix", "csv")
            FileWriter(file).use { writer ->
                writer.appendLine("PKT Protocol Report")
                writer.appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                if (footSide != null) {
                    writer.appendLine("Foot: $footSide")
                }
                writer.appendLine("")
                
                // Заголовок таблицы
                writer.appendLine("Test,User,Stability,Oscillation Frequency,Coordination Factor,Duration (s)")
                measurements.forEachIndexed { index, (userName, result) ->
                    writer.appendLine(
                        "${index + 1}," +
                        "$userName," +
                        "${String.format(Locale.getDefault(), "%.2f", result.metrics.stability)}," +
                        "${String.format(Locale.getDefault(), "%.4f", result.metrics.oscillationFrequency)}," +
                        "${String.format(Locale.getDefault(), "%.4f", result.metrics.coordinationFactor)}," +
                        "${String.format(Locale.getDefault(), "%.2f", result.durationSec)}"
                    )
                }
                writer.appendLine("")
                
                // Средние значения
                if (measurements.isNotEmpty()) {
                    val avgStability = measurements.map { it.second.metrics.stability }.average()
                    val avgFrequency = measurements.map { it.second.metrics.oscillationFrequency }.average()
                    val avgCoordination = measurements.map { it.second.metrics.coordinationFactor }.average()
                    writer.appendLine("Average:")
                    writer.appendLine(
                        ",," +
                        "${String.format(Locale.getDefault(), "%.2f", avgStability)}," +
                        "${String.format(Locale.getDefault(), "%.4f", avgFrequency)}," +
                        "${String.format(Locale.getDefault(), "%.4f", avgCoordination)},"
                    )
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createExportFile(baseName: String, extension: String): File {
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        
        val exportDir = File(downloadsDir, "AccelerometerExports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        return File(exportDir, "$baseName.$extension")
    }
}

