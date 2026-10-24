package com.givebackmyoneplus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Сетка 4×2 из реальных иконок телефона в выбранном стиле. */
class IconPreviewView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val columns = 4
    private val rows = 2
    private var apps: List<LauncherApp> = emptyList()
    private var bitmaps: List<Bitmap> = emptyList()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9A9AA0.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 11f * resources.displayMetrics.scaledDensity
    }

    private var shape = IconShape.CIRCLE
    private var style = IconStyle.ACCENT_BG
    private var accent = 0

    fun setApps(list: List<LauncherApp>) {
        apps = list.take(columns * rows)
        rerender()
    }

    fun update(shape: IconShape, style: IconStyle, accent: Int) {
        this.shape = shape
        this.style = style
        this.accent = accent
        rerender()
    }

    private fun rerender() {
        val px = (56 * resources.displayMetrics.density).toInt()
        bitmaps = apps.map { IconRenderer.render(it.icon, px, shape, style, accent) }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val cellH = (88 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(w, cellH * rows)
    }

    override fun onDraw(canvas: Canvas) {
        val cellW = width / columns.toFloat()
        val cellH = height / rows.toFloat()
        bitmaps.forEachIndexed { i, bmp ->
            val cx = cellW * (i % columns) + cellW / 2
            val top = cellH * (i / columns) + 4 * resources.displayMetrics.density
            canvas.drawBitmap(bmp, cx - bmp.width / 2f, top, paint)
            val label = apps[i].label.let { if (it.length > 10) it.take(9) + "…" else it }
            canvas.drawText(label, cx, top + bmp.height + labelPaint.textSize * 1.4f, labelPaint)
        }
    }
}
