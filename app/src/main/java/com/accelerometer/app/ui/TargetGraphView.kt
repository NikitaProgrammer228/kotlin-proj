package com.accelerometer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.accelerometer.app.R
import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.measurement.MeasurementConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Улучшенный View для отображения мишени с концентрическими кольцами
 * и траекторией движения. Используется для баланс-теста в режиме "Мишень"
 */
class TargetGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Радиусы зон в мм (от центра к краю) равные по ширине (1/3, 2/3, 3/3 от 40 мм)
    // Зеленая: 0-13.3 мм, Жёлтая: 13.3-26.6 мм, Красная: 26.6-40 мм
    private val greenRadiusMm = 40f / 3f          // ≈13.33
    private val yellowRadiusMm = 40f * 2f / 3f    // ≈26.66
    private val redRadiusMm = 40f                 // 40 мм
    
    // Фиксированный диапазон для масштабирования
    // Соответствует максимальному отклонению данных (40 мм)
    private val baseRangeMm = 40f
    
    // Цвета из Figma макета
    private val zoneGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8FDC47.toInt() // #8FDC47
        style = Paint.Style.FILL
    }
    
    private val zoneYellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFCEA8B.toInt() // #FCEA8B
        style = Paint.Style.FILL
    }
    
    private val zoneRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE37969.toInt() // #E37969
        style = Paint.Style.FILL
    }

    private val trajectoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt() // Черный
        style = Paint.Style.STROKE
        strokeWidth = 1.2f // тоньше линия для более "острых" графиков
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE9E9EF.toInt() // Светло-серый для осей
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val path = Path()
    private var samples: List<ProcessedSample> = emptyList()
    private var rangeMm = baseRangeMm
    private var scale: Float = 1f

    fun setSamples(samples: List<ProcessedSample>) {
        this.samples = samples
        if (samples.isNotEmpty()) {
            val maxSampleRange = samples.maxOfOrNull { max(abs(it.sxMm), abs(it.syMm)) } ?: 0.0
            // Используем больший диапазон для масштабирования, чтобы круги соответствовали макету
            rangeMm = max(baseRangeMm, (maxSampleRange * 1.2).toFloat())
        } else {
            // Для пустого графика используем базовый диапазон
            rangeMm = baseRangeMm
        }
        invalidate()
    }

    fun setScale(scale: Float) {
        this.scale = scale
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

        val centerX = width / 2f
        val centerY = height / 2f
        
        // В макете Figma: красный круг диаметр 531px, контейнер 745x613px
        // Красный круг занимает: 531/745 = 71.3% ширины и 531/613 = 86.6% высоты
        // Используем максимальное значение из расчета по ширине и высоте,
        // чтобы мишень занимала максимально возможное пространство
        val targetDiameterByWidth = width * (531f / 745f) // 71.3% от ширины
        val targetDiameterByHeight = height * (531f / 613f) // 86.6% от высоты
        // Используем максимальное значение, но слегка уменьшаем (на ~10%), чтобы круги
        // не заполняли контейнер целиком и соответствовали макету Figma
        val baseDiameter = max(targetDiameterByWidth, targetDiameterByHeight)
        val targetDiameter = baseDiameter * 0.9f
        val redRadiusPixels = targetDiameter / 2f
        
        // Вычисляем scaleFactor так, чтобы redRadiusMm * scaleFactor = redRadiusPixels
        val scaleFactor = (redRadiusPixels / redRadiusMm) * MeasurementConfig.TARGET_GRAPH_SCALE

        // Рисуем концентрические кольца (от внешнего к внутреннему) согласно MicroSwing
        // Красная зона (внешняя) - от 20мм до 40мм (большие отклонения)
        canvas.drawCircle(centerX, centerY, redRadiusMm * scaleFactor, zoneRedPaint)
        // Желтая зона - от 10мм до 20мм (умеренные отклонения)
        canvas.drawCircle(centerX, centerY, yellowRadiusMm * scaleFactor, zoneYellowPaint)
        // Зеленая зона (внутренняя) - от 0мм до 10мм (хорошая стабильность)
        canvas.drawCircle(centerX, centerY, greenRadiusMm * scaleFactor, zoneGreenPaint)

        // Рисуем оси X и Y (тонкие линии через центр) до краёв контейнера,
        // чтобы "крест" доходил до границ экрана/карточки как в макете.
        // Ось X (горизонтальная)
        canvas.drawLine(0f, centerY, width, centerY, axisPaint)
        // Ось Y (вертикальная)
        canvas.drawLine(centerX, 0f, centerX, height, axisPaint)

        // Рисуем траекторию
        // Масштабируем данные так, чтобы они соответствовали размеру кругов
        // redRadiusMm соответствует redRadiusPixels, поэтому используем тот же scaleFactor
        if (samples.size >= 2) {
            path.reset()
            samples.forEachIndexed { index, sample ->
                // Инвертируем X при отрисовке, чтобы совпасть с немецким ПО (наклон влево → линия вправо)
                val x = centerX - (sample.sxMm * scaleFactor).toFloat()
                // Y остаётся инвертированным для экранных координат
                val y = centerY - (sample.syMm * scaleFactor).toFloat()
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, trajectoryPaint)
        }
    }
}
