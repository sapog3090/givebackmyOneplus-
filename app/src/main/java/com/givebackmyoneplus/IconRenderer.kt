package com.givebackmyoneplus

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
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
    NATIVE("Как у приложения"),
    CIRCLE("Круг"),
    SQUIRCLE("Сквиркл"),
    ROUNDED("Скруглённый квадрат"),
}

enum class IconStyle(val title: String) {
    TINT("Покраска"),
    DARK_TINT("Тёмная (как iOS)"),
    ACCENT_BG("Цветная подложка"),
    ORIGINAL("Оригинал"),
}

/** Рисует иконку приложения в выбранной форме и стиле. */
object IconRenderer {

    private const val DARK_BG = 0xFF1E1E21.toInt()

    fun shapePath(shape: IconShape, size: Float): Path = Path().apply {
        when (shape) {
            IconShape.NATIVE, IconShape.SQUIRCLE -> squircle(this, size)
            IconShape.CIRCLE -> addCircle(size / 2, size / 2, size / 2, Path.Direction.CW)
            IconShape.ROUNDED -> addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
        }
    }

    /** Суперэллипс |x|^4 + |y|^4 = 1 */
    private fun squircle(path: Path, size: Float) = path.apply {
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

    /**
     * Перекрашивает картинку в акцентный цвет по яркости:
     * светлые места → яркий акцент, тёмные → тёмный оттенок акцента.
     */
    fun tintFilter(accent: Int): ColorMatrixColorFilter {
        val lift = 0.18f
        val k = 1f - lift
        fun row(c: Int): FloatArray {
            val f = c / 255f
            return floatArrayOf(0.2126f * k * f, 0.7152f * k * f, 0.0722f * k * f, 0f, lift * c)
        }
        val r = row(Color.red(accent))
        val g = row(Color.green(accent))
        val b = row(Color.blue(accent))
        return ColorMatrixColorFilter(ColorMatrix(r + g + b + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
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

    /** Иконка целиком: адаптивная — по своей системной форме (или по clip), старая — как есть. */
    private fun drawWhole(canvas: Canvas, src: Drawable, size: Int, customShape: Boolean, filter: ColorFilter?) {
        if (src is AdaptiveIconDrawable) {
            if (customShape) {
                src.background?.mutate()?.let { it.colorFilter = filter; drawLayer(canvas, it, size) }
                src.foreground?.mutate()?.let { it.colorFilter = filter; drawLayer(canvas, it, size) }
            } else {
                src.colorFilter = filter
                src.setBounds(0, 0, size, size)
                src.draw(canvas)
            }
        } else {
            src.colorFilter = filter
            if (customShape) {
                val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; colorFilter = filter }
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
                drawInset(canvas, src, size, 0.78f)
            } else {
                drawInset(canvas, src, size, 0.92f)
            }
        }
    }

    fun render(icon: Drawable, size: Int, shape: IconShape, style: IconStyle, accent: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val src = icon.constantState?.newDrawable()?.mutate() ?: icon.mutate()
        val customShape = shape != IconShape.NATIVE

        // Контур: выбранная форма или собственная маска адаптивной иконки.
        val outline: Path? = when {
            customShape -> shapePath(shape, size.toFloat())
            src is AdaptiveIconDrawable -> {
                src.setBounds(0, 0, size, size)
                Path(src.iconMask)
            }
            else -> null
        }

        when (style) {
            IconStyle.ORIGINAL, IconStyle.TINT -> {
                if (customShape) canvas.clipPath(outline!!)
                drawWhole(canvas, src, size, customShape, if (style == IconStyle.TINT) tintFilter(accent) else null)
            }
            IconStyle.DARK_TINT -> {
                val g = glyph(src)?.mutate()
                if (outline == null || g == null) {
                    // Старая иконка без слоёв — просто перекрашиваем целиком.
                    outline?.let { canvas.clipPath(it) }
                    drawWhole(canvas, src, size, customShape, tintFilter(accent))
                } else {
                    canvas.clipPath(outline)
                    paint.color = DARK_BG
                    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                    g.colorFilter = if (isMonochrome(src, g)) {
                        PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN)
                    } else {
                        tintFilter(accent)
                    }
                    drawLayer(canvas, g, size)
                }
            }
            IconStyle.ACCENT_BG -> {
                canvas.clipPath(outline ?: shapePath(IconShape.SQUIRCLE, size.toFloat()))
                paint.color = accent
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                val g = glyph(src)?.mutate()
                if (g != null) {
                    g.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    drawLayer(canvas, g, size)
                } else {
                    src.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    drawInset(canvas, src, size, 0.62f)
                }
            }
        }
        return bmp
    }

    private fun isMonochrome(icon: Drawable, layer: Drawable): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            icon is AdaptiveIconDrawable && icon.monochrome === layer

    /** Заливка формы — для icon_background / icon_mask в теме. */
    fun shapeBitmap(size: Int, shape: IconShape, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        Canvas(bmp).drawPath(shapePath(shape, size.toFloat()), paint)
        return bmp
    }
}
