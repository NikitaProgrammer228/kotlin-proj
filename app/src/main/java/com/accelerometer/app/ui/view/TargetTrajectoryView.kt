package com.accelerometer.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.accelerometer.app.data.ProcessedSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TargetTrajectoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val zoneRadiiMm = floatArrayOf(10f, 20f, 30f, 40f)
    private val zonePaints = listOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8BC34A") },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFEB3B") },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFC107") },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7043") }
    )
    private val trajectoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val path = Path()
    private var samples: List<ProcessedSample> = emptyList()
    private var rangeMm = 50f

    fun setSamples(samples: List<ProcessedSample>) {
        this.samples = samples
        val maxSampleRange = samples.maxOfOrNull { max(abs(it.sxMm), abs(it.syMm)) } ?: 0.0
        rangeMm = max(zoneRadiiMm.last(), (maxSampleRange * 1.2).toFloat().coerceAtLeast(50f))
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
        val scale = (minDim / 2f) / rangeMm

        zoneRadiiMm.forEachIndexed { index, radiusMm ->
            canvas.drawCircle(centerX, centerY, radiusMm * scale, zonePaints.getOrNull(index) ?: zonePaints.last())
        }

        if (samples.size < 2) return
        path.reset()
        samples.forEachIndexed { index, sample ->
            val x = centerX + (sample.sxMm * scale).toFloat()
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

