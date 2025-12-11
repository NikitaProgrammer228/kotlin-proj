package com.accelerometer.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.accelerometer.app.data.MeasurementResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Сервис для экспорта результатов измерений в PDF
 */
class PdfExportService(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Экспорт в PDF формат
     */
    fun exportToPdf(
        result: MeasurementResult,
        userName: String,
        fileName: String? = null
    ): File? {
        return try {
            val file = createExportFile(fileName ?: "measurement_${dateFormat.format(Date())}", "pdf")
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 в пунктах (72 DPI)
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Заголовок
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Measurement Report", 50f, 50f, paint)

            // Информация о пользователе
            paint.textSize = 12f
            paint.isFakeBoldText = false
            var y = 100f
            canvas.drawText("User: $userName", 50f, y, paint)
            y += 20f
            canvas.drawText("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", 50f, y, paint)
            y += 20f
            canvas.drawText("Duration: ${String.format(Locale.getDefault(), "%.2f", result.durationSec)}s", 50f, y, paint)
            y += 40f

            // Метрики
            paint.isFakeBoldText = true
            canvas.drawText("Metrics:", 50f, y, paint)
            y += 25f
            paint.isFakeBoldText = false
            canvas.drawText("Stability: ${String.format(Locale.getDefault(), "%.2f", result.metrics.stability)}", 50f, y, paint)
            y += 20f
            canvas.drawText("Oscillation Frequency: ${String.format(Locale.getDefault(), "%.4f", result.metrics.oscillationFrequency)} Hz", 50f, y, paint)
            y += 20f
            canvas.drawText("Coordination Factor: ${String.format(Locale.getDefault(), "%.4f", result.metrics.coordinationFactor)}", 50f, y, paint)
            y += 40f

            // Данные (первые 20 строк)
            paint.isFakeBoldText = true
            canvas.drawText("Sample Data (first 20 samples):", 50f, y, paint)
            y += 25f
            paint.isFakeBoldText = false
            paint.textSize = 10f
            canvas.drawText("Time (s) | Pos X (mm) | Pos Y (mm)", 50f, y, paint)
            y += 15f
            result.samples.take(20).forEach { sample ->
                if (y > 800f) return@forEach
                val line = String.format(
                    Locale.getDefault(),
                    "%.3f | %.2f | %.2f",
                    sample.t,
                    sample.sxMm,
                    sample.syMm
                )
                canvas.drawText(line, 50f, y, paint)
                y += 15f
            }

            document.finishPage(page)
            
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createExportFile(baseName: String, extension: String): File {
        val downloadsDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        
        val exportDir = File(downloadsDir, "AccelerometerExports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        return File(exportDir, "$baseName.$extension")
    }
}

