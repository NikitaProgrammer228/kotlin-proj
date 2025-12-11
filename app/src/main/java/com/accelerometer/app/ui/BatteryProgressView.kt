package com.accelerometer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BatteryProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Светлый фон как в макете Figma (#EDEDF6)
        color = 0xFFEDEDF6.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Тёмно-синий прогресс как в макете Figma (#64748B)
        color = 0xFF64748B.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        
        // В SVG: диаметр круга = 100 единиц, толщина кольца = 10 единиц (10% от диаметра)
        // Для текущего размера view рассчитываем толщину пропорционально
        val viewSize = minOf(width, height)
        // Используем немного меньшую толщину для лучшего визуального соответствия кнопке подключения
        val strokeWidth = viewSize * 0.0875f // ~8.75% от размера view (немного меньше для визуального соответствия)
        
        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        
        val strokeHalf = strokeWidth / 2f
        // Используем весь доступный размер для максимального визуального соответствия
        val radius = viewSize / 2f - strokeHalf

        // Рисуем фоновое кольцо
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Рисуем прогресс-дугу
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        val sweepAngle = (progress / 100f) * 360f
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
    }
}
