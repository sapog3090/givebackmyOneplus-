package com.givebackmyoneplus

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

enum class IconShape(val title: String) {
    CIRCLE("Круг"),
    SQUIRCLE("Сквиркл"),
    ROUNDED("Скруглённый квадрат"),
}

enum class IconStyle(val title: String) {
    ACCENT_BG("Цветная подложка"),
    MONO("Монохром"),
    ORIGINAL("Оригинал"),
}

/** Рисует иконку приложения в выбранной форме и стиле. */
object IconRenderer {

    private const val DARK_BG = 0xFF1E1E21.toInt()

    fun shapePath(shape: IconShape, size: Float): Path = Path().apply {
        when (shape) {
            IconShape.CIRCLE -> addCircle(size / 2, size / 2, size / 2, Path.Direction.CW)
            IconShape.ROUNDED -> addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
            IconShape.SQUIRCLE -> {
                // Суперэллипс |x|^4 + |y|^4 = 1
                val r = size / 2
                for (i in 0..180) {
                    val t = Math.toRadians(i * 2.0)
                    val c = cos(t)
                    val s = sin(t)
                    val x = r + r * sign(c) * abs(c).pow(0.5)
                    val y = r + r * sign(s) * abs(s).pow(0.5)
                    if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
                }
                close()
            }
        }
    }

    /** Глиф иконки: монохромный слой (Android 13+) или передний слой адаптивной иконки. */
    private fun glyph(icon: Drawable): Drawable? {
        if (icon !is AdaptiveIconDrawable) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            icon.monochrome?.let { return it }
        }
        return icon.foreground
    }

    private fun drawLayer(canvas: Canvas, layer: Drawable, size: Int) {
        // Слои адаптивной иконки на 50% больше видимой области.
        val pad = size / 4
        layer.setBounds(-pad, -pad, size + pad, size + pad)
        layer.draw(canvas)
    }

    private fun drawInset(canvas: Canvas, d: Drawable, size: Int, fraction: Float) {
        val inset = ((1 - fraction) / 2 * size).toInt()
        d.setBounds(inset, inset, size - inset, size - inset)
        d.draw(canvas)
    }

    fun render(icon: Drawable, size: Int, shape: IconShape, style: IconStyle, accent: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.clipPath(shapePath(shape, size.toFloat()))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val src = icon.constantState?.newDrawable()?.mutate() ?: icon.mutate()

        when (style) {
            IconStyle.ORIGINAL -> {
                if (src is AdaptiveIconDrawable) {
                    src.background?.let { drawLayer(canvas, it, size) }
                    src.foreground?.let { drawLayer(canvas, it, size) }
                } else {
                    canvas.drawColor(Color.WHITE)
                    drawInset(canvas, src, size, 0.78f)
                }
            }
            IconStyle.ACCENT_BG, IconStyle.MONO -> {
                val bg = if (style == IconStyle.ACCENT_BG) accent else DARK_BG
                val fg = if (style == IconStyle.ACCENT_BG) Color.WHITE else accent
                paint.color = bg
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                val g = glyph(src)?.mutate()
                if (g != null) {
                    g.colorFilter = PorterDuffColorFilter(fg, PorterDuff.Mode.SRC_IN)
                    drawLayer(canvas, g, size)
                } else {
                    // Старая (не адаптивная) иконка: обесцвечиваем, чтобы не спорила с акцентом.
                    src.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    drawInset(canvas, src, size, 0.62f)
                }
            }
        }
        return bmp
    }

    /** Заливка формы — для icon_background / icon_mask в теме. */
    fun shapeBitmap(size: Int, shape: IconShape, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        Canvas(bmp).drawPath(shapePath(shape, size.toFloat()), paint)
        return bmp
    }
}
