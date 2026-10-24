package com.givebackmyoneplus

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Пасхалка: стилизованный флагман в светлом корпусе с квадратным блоком камер
 * и тёплыми «дюнами» на экране. Нарисован с нуля, фото OnePlus не используются.
 */
class PhoneView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEB0029.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    override fun onDraw(canvas: Canvas) {
        val ph = height * 0.96f
        val pw = ph * 0.47f
        val left = (width - pw) / 2
        val top = (height - ph) / 2
        val r = pw * 0.14f
        val body = RectF(left, top, left + pw, top + ph)

        // Корпус — песочно-белый с мягким бликом.
        p.shader = LinearGradient(left, top, left + pw, top + ph,
            intArrayOf(0xFFF4EFE6.toInt(), 0xFFE7DFD1.toInt(), 0xFFD8CDBB.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(body, r, r, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.015f
        p.color = 0x33FFFFFF
        canvas.drawRoundRect(body, r, r, p)
        p.style = Paint.Style.FILL

        // Экран с тёплыми дюнами.
        val sm = pw * 0.05f
        val screen = RectF(left + sm, top + sm, left + pw - sm, top + ph - sm)
        val sr = r * 0.8f
        val save = canvas.save()
        val clip = Path().apply { addRoundRect(screen, sr, sr, Path.Direction.CW) }
        canvas.clipPath(clip)
        p.shader = LinearGradient(screen.left, screen.top, screen.left, screen.bottom,
            intArrayOf(0xFF2A1C12.toInt(), 0xFF5A3A1E.toInt(), 0xFF0E0A07.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(screen, p)
        // Тёплое «солнце»-дюна.
        p.shader = RadialGradient(screen.centerX(), screen.centerY() + screen.height() * 0.12f,
            screen.width() * 0.75f,
            intArrayOf(0xFFF3A85A.toInt(), 0xFFC9712E.toInt(), 0x00000000),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(screen, p)
        // Изгибы дюн.
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = screen.width() * 0.018f
        for (i in 0..4) {
            p.color = (0x40FFFFFF + i * 0x08000000)
            val cy = screen.top + screen.height() * (0.35f + i * 0.13f)
            val arc = Path().apply {
                moveTo(screen.left, cy)
                quadTo(screen.centerX(), cy - screen.height() * 0.12f, screen.right, cy + screen.height() * 0.03f)
            }
            canvas.drawPath(arc, p)
        }
        p.style = Paint.Style.FILL
        p.shader = null
        canvas.restoreToCount(save)

        // Вырез фронтальной камеры.
        p.color = 0xFF0A0A0A.toInt()
        canvas.drawCircle(screen.centerX(), screen.top + sm * 1.6f, pw * 0.028f, p)

        // Квадратный блок камер в левом верхнем углу (как на флагманах OnePlus).
        val m = pw * 0.46f
        val mx = left - pw * 0.02f
        val my = top - ph * 0.015f
        val island = RectF(mx, my, mx + m, my + m)
        p.shader = LinearGradient(mx, my, mx + m, my + m,
            0xFFF0EAE0.toInt(), 0xFFCFC4B2.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(island, m * 0.32f, m * 0.32f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = m * 0.015f
        p.color = 0x22000000
        canvas.drawRoundRect(island, m * 0.32f, m * 0.32f, p)
        p.style = Paint.Style.FILL

        // Две крупные линзы + вспышка.
        val lenses = listOf(0.34f to 0.32f, 0.34f to 0.68f)
        lenses.forEach { (fx, fy) ->
            val cx = mx + m * fx
            val cy = my + m * fy
            val lr = m * 0.19f
            p.color = 0xFF2B2B2E.toInt()
            canvas.drawCircle(cx, cy, lr * 1.18f, p)
            p.shader = RadialGradient(cx - lr * 0.3f, cy - lr * 0.3f, lr,
                intArrayOf(0xFF3A4A66.toInt(), 0xFF0B0B10.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, lr, p)
            p.shader = null
            p.color = 0x66AECBFF
            canvas.drawCircle(cx - lr * 0.3f, cy - lr * 0.3f, lr * 0.22f, p)
        }
        // Третья линза и вспышка справа.
        p.color = 0xFF2B2B2E.toInt()
        canvas.drawCircle(mx + m * 0.68f, my + m * 0.5f, m * 0.11f, p)
        p.shader = RadialGradient(mx + m * 0.68f, my + m * 0.5f, m * 0.1f,
            intArrayOf(0xFF3A4A66.toInt(), 0xFF0B0B10.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(mx + m * 0.68f, my + m * 0.5f, m * 0.1f, p)
        p.shader = null
        p.color = 0xFFFFE9B0.toInt()
        canvas.drawCircle(mx + m * 0.82f, my + m * 0.78f, m * 0.05f, p)

        // Крупная красная «15» внизу экрана.
        number.textSize = pw * 0.44f
        number.setShadowLayer(pw * 0.09f, 0f, 0f, 0xBBEB0029.toInt())
        canvas.drawText("15", screen.centerX(), top + ph * 0.84f, number)
    }
}
