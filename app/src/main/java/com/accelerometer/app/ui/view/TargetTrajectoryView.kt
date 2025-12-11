package com.accelerometer.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.measurement.MeasurementConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TargetTrajectoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Радиусы зон равные по ширине (1/3, 2/3, 3/3 от 40 мм)
    // Рисуем от внешнего к внутреннему, поэтому порядок: красный, жёлтый, зелёный
    private val zoneRadiiMm = floatArrayOf(40f, 40f * 2f / 3f, 40f / 3f)
    private val zonePaints = listOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E37969") }, // Красный
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FCEA8B") }, // Жёлтый
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8FDC47") }  // Зелёный
    )
    private val trajectoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121") // Тёмно-серый, контрастный на всех зонах
        style = Paint.Style.STROKE
        strokeWidth = 1.2f // тоньше линия как у немецкого софта
    }
    private val path = Path()
    private var samples: List<ProcessedSample> = emptyList()
    private var rangeMm = 50f

    fun setSamples(samples: List<ProcessedSample>) {
        this.samples = samples
        val maxSampleRange = samples.maxOfOrNull { max(abs(it.sxMm), abs(it.syMm)) } ?: 0.0
        // Максимальный радиус теперь 40 мм (первый элемент массива)
        rangeMm = max(zoneRadiiMm.first(), (maxSampleRange * 1.2).toFloat().coerceAtLeast(40f))
        invalidate()
    }

    fun clear() {
        samples = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width == 0f || height == 0f) return

        val minDim = min(width, height)
        val centerX = width / 2f
        val centerY = height / 2f
        val scale = (minDim / 2f) / rangeMm * MeasurementConfig.TARGET_GRAPH_SCALE

        zoneRadiiMm.forEachIndexed { index, radiusMm ->
            canvas.drawCircle(centerX, centerY, radiusMm * scale, zonePaints.getOrNull(index) ?: zonePaints.last())
        }

        if (samples.size < 2) return
        path.reset()
        samples.forEachIndexed { index, sample ->
            // Инвертируем X при отрисовке, чтобы совпасть с немецким ПО (наклон влево → линия вправо)
            val x = centerX - (sample.sxMm * scale).toFloat()
            // Y инвертируем для экранных координат
            val y = centerY - (sample.syMm * scale).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, trajectoryPaint)
    }
}

