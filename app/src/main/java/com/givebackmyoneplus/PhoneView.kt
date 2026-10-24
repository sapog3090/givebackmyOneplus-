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
 * Пасхалка: два телефона, как на промо-фото флагманов — задняя панель с квадратным
 * блоком камер и лицевая сторона с тёплыми «дюнами» и красной «15».
 * Всё нарисовано с нуля, фото OnePlus не используются.
 */
class PhoneView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEB0029.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // тени и свечение
    }

    override fun onDraw(canvas: Canvas) {
        val ph = height * 0.9f
        val pw = ph * 0.47f
        val cx = width / 2f
        val baseTop = (height - ph) / 2

        val back = RectF(cx - pw * 0.98f, baseTop + ph * 0.04f, cx - pw * 0.98f + pw, baseTop + ph * 0.04f + ph)
        val front = RectF(cx - pw * 0.08f, baseTop - ph * 0.02f, cx - pw * 0.08f + pw, baseTop - ph * 0.02f + ph)

        drawBack(canvas, back)
        drawFront(canvas, front)
    }

    private fun shadow(canvas: Canvas, r: RectF, radius: Float) {
        p.shader = null
        p.color = 0xFF000000.toInt()
        p.setShadowLayer(r.width() * 0.12f, 0f, r.width() * 0.06f, 0x99000000.toInt())
        canvas.drawRoundRect(r, radius, radius, p)
        p.clearShadowLayer()
    }

    /** Задняя панель: песочный корпус и квадратный блок камер. */
    private fun drawBack(canvas: Canvas, r: RectF) {
        val w = r.width()
        val radius = w * 0.15f
        shadow(canvas, r, radius)

        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
            intArrayOf(0xFFF7F2EA.toInt(), 0xFFE9E1D3.toInt(), 0xFFD6CAB6.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(r, radius, radius, p)

        // Тонкая металлическая рамка.
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.018f
        p.color = 0xFFC8BCA8.toInt()
        canvas.drawRoundRect(r, radius, radius, p)
        p.style = Paint.Style.FILL

        // Блок камер: внутри корпуса, слегка приподнят.
        val m = w * 0.46f
        val island = RectF(r.left + w * 0.07f, r.top + w * 0.07f, r.left + w * 0.07f + m, r.top + w * 0.07f + m)
        val ir = m * 0.3f
        p.setShadowLayer(w * 0.03f, 0f, w * 0.012f, 0x55000000)
        p.shader = LinearGradient(island.left, island.top, island.right, island.bottom,
            0xFFFBF8F2.toInt(), 0xFFDCD2C2.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(island, ir, ir, p)
        p.clearShadowLayer()
        p.shader = null

        lens(canvas, island.left + m * 0.29f, island.top + m * 0.29f, m * 0.2f)
        lens(canvas, island.left + m * 0.29f, island.top + m * 0.71f, m * 0.2f)
        lens(canvas, island.left + m * 0.71f, island.top + m * 0.29f, m * 0.14f)
        // Вспышка.
        p.color = 0xFFE6DCCB.toInt()
        canvas.drawCircle(island.left + m * 0.71f, island.top + m * 0.71f, m * 0.085f, p)
        p.color = 0xFFFFF1C9.toInt()
        canvas.drawCircle(island.left + m * 0.71f, island.top + m * 0.71f, m * 0.06f, p)

        // Еле заметная гравировка «15» по центру спинки.
        number.textSize = w * 0.16f
        number.clearShadowLayer()
        number.color = 0x22000000
        canvas.drawText("15", r.centerX(), r.top + r.height() * 0.6f, number)
    }

    /** Объектив: металлическое кольцо, чёрное стекло и голубой блик. */
    private fun lens(canvas: Canvas, x: Float, y: Float, rad: Float) {
        p.shader = LinearGradient(x - rad, y - rad, x + rad, y + rad,
            0xFF8C8C92.toInt(), 0xFF2E2E33.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, rad * 1.18f, p)
        p.shader = RadialGradient(x - rad * 0.25f, y - rad * 0.25f, rad,
            intArrayOf(0xFF2A3550.toInt(), 0xFF07070A.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, rad, p)
        p.shader = RadialGradient(x, y, rad * 0.5f,
            intArrayOf(0xFF15203A.toInt(), 0xFF020204.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, rad * 0.5f, p)
        p.shader = null
        p.color = 0x99B9D4FF.toInt()
        canvas.drawCircle(x - rad * 0.35f, y - rad * 0.35f, rad * 0.16f, p)
        p.color = 0x55FFFFFF
        canvas.drawCircle(x + rad * 0.3f, y + rad * 0.32f, rad * 0.07f, p)
    }

    /** Лицевая сторона: узкая рамка, «дюны» на экране и красная «15». */
    private fun drawFront(canvas: Canvas, r: RectF) {
        val w = r.width()
        val radius = w * 0.15f
        shadow(canvas, r, radius)

        // Рамка корпуса.
        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
            0xFFE9E1D3.toInt(), 0xFFC9BDA9.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(r, radius, radius, p)
        p.shader = null
        val bezel = w * 0.035f
        val glass = RectF(r.left + w * 0.012f, r.top + w * 0.012f, r.right - w * 0.012f, r.bottom - w * 0.012f)
        p.color = 0xFF050505.toInt()
        canvas.drawRoundRect(glass, radius * 0.94f, radius * 0.94f, p)

        val screen = RectF(r.left + bezel, r.top + bezel, r.right - bezel, r.bottom - bezel)
        val sr = radius * 0.82f
        val save = canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(screen, sr, sr, Path.Direction.CW) })

        // Небо: тёмное сверху, тёплое к горизонту.
        p.shader = LinearGradient(screen.left, screen.top, screen.left, screen.bottom,
            intArrayOf(0xFF140C08.toInt(), 0xFF3B2213.toInt(), 0xFFB06A2C.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(screen, p)
        // Свечение «солнца».
        p.shader = RadialGradient(screen.centerX() + screen.width() * 0.15f, screen.top + screen.height() * 0.42f,
            screen.width() * 0.8f,
            intArrayOf(0xCCF6B66A.toInt(), 0x55D88A3E, 0x00000000), floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP)
        canvas.drawRect(screen, p)
        p.shader = null

        // Дюны — залитые волны, от дальних светлых к ближним тёмным.
        val dunes = intArrayOf(0xFFD9914C.toInt(), 0xFFB8703A.toInt(), 0xFF8A4F27.toInt(), 0xFF5B3219.toInt(), 0xFF2E180C.toInt())
        dunes.forEachIndexed { i, color ->
            val y = screen.top + screen.height() * (0.5f + i * 0.1f)
            val wave = Path().apply {
                moveTo(screen.left, y)
                cubicTo(
                    screen.left + screen.width() * 0.3f, y - screen.height() * (0.08f - i * 0.01f),
                    screen.left + screen.width() * 0.65f, y + screen.height() * 0.06f,
                    screen.right, y - screen.height() * 0.03f,
                )
                lineTo(screen.right, screen.bottom)
                lineTo(screen.left, screen.bottom)
                close()
            }
            p.color = color
            canvas.drawPath(wave, p)
        }

        // Время сверху, как на экране блокировки.
        val clock = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textSize = w * 0.2f
        }
        val now = java.time.LocalTime.now()
        canvas.drawText("%02d:%02d".format(now.hour, now.minute), screen.centerX(), screen.top + screen.height() * 0.2f, clock)

        // Красная «15» со свечением.
        number.color = 0xFFEB0029.toInt()
        number.textSize = w * 0.36f
        number.setShadowLayer(w * 0.08f, 0f, 0f, 0xCCEB0029.toInt())
        canvas.drawText("15", screen.centerX(), screen.top + screen.height() * 0.45f, number)
        number.clearShadowLayer()

        canvas.restoreToCount(save)

        // Фронтальная камера.
        p.color = 0xFF000000.toInt()
        canvas.drawCircle(screen.centerX(), screen.top + w * 0.06f, w * 0.03f, p)
        p.color = 0xFF1A2235.toInt()
        canvas.drawCircle(screen.centerX(), screen.top + w * 0.06f, w * 0.014f, p)
    }
}
