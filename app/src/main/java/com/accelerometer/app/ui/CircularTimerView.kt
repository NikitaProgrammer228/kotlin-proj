package com.accelerometer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Кастомный View для отображения кругового таймера подготовки к тесту.
 * Показывает красную дугу прогресса на сером фоне.
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt() // Светло-серый фон
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC433C.toInt() // Красный цвет прогресса
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val bounds = RectF()
    
    // Прогресс от 0.0 до 1.0 (1.0 = полный круг)
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = backgroundPaint.strokeWidth / 2 + 4
        bounds.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Рисуем фоновый круг (полный)
        canvas.drawArc(bounds, 0f, 360f, false, backgroundPaint)
        
        // Рисуем прогресс (дугу)
        // Начинаем с -90 градусов (верх), идём по часовой стрелке
        val sweepAngle = 360f * progress
        canvas.drawArc(bounds, -90f, sweepAngle, false, progressPaint)
    }
}

