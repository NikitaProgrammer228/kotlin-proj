package com.accelerometer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.accelerometer.app.R
import kotlin.math.max

/**
 * Кастомный View для отображения графиков с цветными зонами (красный, желтый, зеленый)
 * Используется для отображения данных акселерометра по осям X и Y
 */
class WaveformGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val zoneRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.graph_zone_red)
        style = Paint.Style.FILL
    }

    private val zoneYellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.graph_zone_yellow)
        style = Paint.Style.FILL
    }

    private val zoneGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.graph_zone_green)
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.graph_line)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f  // ещё тоньше линия, как в немецком софте
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 100
    }

    private val path = Path()
    private var dataPoints: List<Float> = emptyList()
    private var timePoints: List<Float> = emptyList() // Временные метки для каждой точки
    private var minValue: Float = -40f
    private var maxValue: Float = 40f
    private var timeScale: Float = 1f // Масштаб времени (x1, x2, etc.)

    // Параметры для отображения в реальном времени
    private var testDurationSec: Float = 10f // Длительность теста

    // Пороги для цветных зон, как в немецком ПО (равные полосы 1/3,1/3,1/3):
    // Зеленая зона: 0-13.3 мм (0-33% от диапазона ±40 мм)
    // Желтая зона: 13.3-26.6 мм (33-66%)
    // Красная зона: 26.6-40 мм (66-100%)
    private val greenZoneThreshold = 1f / 3f
    private val yellowZoneThreshold = 2f / 3f

    /**
     * Устанавливает данные для отображения на графике.
     * @param data Значения Y (позиция в мм)
     * @param times Временные метки для каждого значения (в секундах от начала теста)
     * @param min Минимальное значение Y (-40 мм)
     * @param max Максимальное значение Y (+40 мм)
     */
    fun setData(data: List<Float>, times: List<Float> = emptyList(), min: Float = -10f, max: Float = 10f) {
        dataPoints = data
        timePoints = times
        minValue = min
        maxValue = max
        invalidate()
    }
    
    fun setTestDuration(durationSec: Float) {
        testDurationSec = durationSec
        invalidate()
    }

    fun setTimeScale(scale: Float) {
        timeScale = scale
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f

        // Рисуем цветные зоны согласно MicroSwing
        val range = maxValue - minValue
        val halfRange = range / 2f
        
        // Вычисляем позиции зон относительно центра (равные полосы по высоте)
        // Зеленая зона: 0-33% (±13.3 мм из ±40 мм)
        val greenZoneSize = halfRange * greenZoneThreshold
        val greenZoneTop = centerY - (greenZoneSize * height / range)
        val greenZoneBottom = centerY + (greenZoneSize * height / range)
        
        // Желтая зона: 33-66% (±13.3-26.6 мм)
        val yellowZoneSize = halfRange * yellowZoneThreshold
        val yellowZoneTop = centerY - (yellowZoneSize * height / range)
        val yellowZoneBottom = centerY + (yellowZoneSize * height / range)

        // Красные зоны (от 66% до 100%, т.е. от ±26.6 мм до ±40 мм)
        canvas.drawRect(0f, 0f, width, yellowZoneTop, zoneRedPaint)
        canvas.drawRect(0f, yellowZoneBottom, width, height, zoneRedPaint)

        // Желтые зоны (от 25% до 50%, т.е. от ±10 мм до ±20 мм)
        canvas.drawRect(0f, yellowZoneTop, width, greenZoneTop, zoneYellowPaint)
        canvas.drawRect(0f, greenZoneBottom, width, yellowZoneBottom, zoneYellowPaint)

        // Зеленая зона (от 0% до 25%, т.е. от 0 до ±10 мм) - центр
        canvas.drawRect(0f, greenZoneTop, width, greenZoneBottom, zoneGreenPaint)

        // Рисуем сетку (центральная линия)
        canvas.drawLine(0f, centerY, width, centerY, gridPaint)

        // Рисуем график данных
        // X-координата вычисляется на основе ВРЕМЕНИ, а не индекса сэмпла
        // Это позволяет графику корректно заполнять всю ширину за testDurationSec
        if (dataPoints.isNotEmpty()) {
            path.reset()
            
            // Пиксели на секунду
            val pixelsPerSecond = width / testDurationSec

            for (i in dataPoints.indices) {
                // Вычисляем X на основе времени (если есть) или индекса
                val timeSec = if (timePoints.isNotEmpty() && i < timePoints.size) {
                    timePoints[i]
                } else {
                    // Fallback: равномерно распределяем по testDurationSec
                    (i.toFloat() / max(dataPoints.size - 1, 1)) * testDurationSec
                }
                
                val x = timeSec * pixelsPerSecond * timeScale
                
                // Ограничиваем X шириной графика
                if (x > width) break
                
                val normalizedValue = (dataPoints[i] - minValue) / range
                val y = height - (normalizedValue * height)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            canvas.drawPath(path, linePaint)
        }
    }
}
