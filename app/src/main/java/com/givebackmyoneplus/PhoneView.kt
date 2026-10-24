package com.givebackmyoneplus

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/** Пасхалка: стилизованный телефон с крупной «15» (нарисован с нуля, не фото OnePlus). */
class PhoneView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF4A4A50.toInt()
    }
    private val module = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1A1D.toInt() }
    private val lens = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF050506.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF55555C.toInt()
    }
    private val flash = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE9B0.toInt() }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEB0029.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    override fun onDraw(canvas: Canvas) {
        val ph = height * 0.94f
        val pw = ph * 0.48f
        val left = (width - pw) / 2
        val top = (height - ph) / 2
        val r = pw * 0.16f
        val rect = RectF(left, top, left + pw, top + ph)

        body.shader = LinearGradient(left, top, left + pw, top + ph,
            0xFF2E2E33.toInt(), 0xFF0E0E10.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, body)
        stroke.strokeWidth = pw * 0.02f
        canvas.drawRoundRect(rect, r, r, stroke)

        // Блок камер
        val m = pw * 0.44f
        val mx = left + pw * 0.09f
        val my = top + pw * 0.09f
        canvas.drawRoundRect(RectF(mx, my, mx + m, my + m), m * 0.28f, m * 0.28f, module)
        ring.strokeWidth = m * 0.04f
        val lr = m * 0.17f
        listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f).forEach { (fx, fy) ->
            canvas.drawCircle(mx + m * fx, my + m * fy, lr, lens)
            canvas.drawCircle(mx + m * fx, my + m * fy, lr, ring)
        }
        canvas.drawCircle(mx + m * 0.7f, my + m * 0.7f, m * 0.07f, flash)

        // «15»
        number.textSize = pw * 0.5f
        number.setShadowLayer(pw * 0.08f, 0f, 0f, 0x99EB0029.toInt())
        canvas.drawText("15", left + pw / 2, top + ph * 0.72f, number)
    }
}
