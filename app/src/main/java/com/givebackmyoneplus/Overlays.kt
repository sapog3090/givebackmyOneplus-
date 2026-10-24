package com.givebackmyoneplus

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator

/** Полноэкранные оверлеи поверх всех окон. Касания проходят насквозь. */
object Overlays {

    private val main = Handler(Looper.getMainLooper())
    private var horizonShowing = false
    private var chargingShowing = false

    fun canDraw(ctx: Context) = Settings.canDrawOverlays(ctx)

    private fun fullscreen() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Android 12+ пропускает касания сквозь чужие окна, только если они прозрачнее 0.8.
        alpha = 0.8f
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    private fun show(ctx: Context, view: AnimatedOverlay, onDone: () -> Unit) {
        val wm = ctx.getSystemService(WindowManager::class.java)
        try {
            wm.addView(view, fullscreen())
        } catch (e: Exception) {
            onDone()
            return
        }
        view.play {
            try { wm.removeView(view) } catch (_: Exception) {}
            onDone()
        }
    }

    fun horizon(ctx: Context) = main.post {
        if (horizonShowing || !canDraw(ctx)) return@post
        horizonShowing = true
        val app = ctx.applicationContext
        show(app, HorizonView(app, Features.accent(app))) { horizonShowing = false }
    }

    fun charging(ctx: Context, level: Int) = main.post {
        if (chargingShowing || !canDraw(ctx)) return@post
        chargingShowing = true
        val app = ctx.applicationContext
        show(app, ChargingView(app, Features.accent(app), level)) { chargingShowing = false }
    }
}

/** Оверлей с анимацией: progress идёт 0→1 за duration, затем окно убирается. */
abstract class AnimatedOverlay(ctx: Context, private val duration: Long) : View(ctx) {
    protected var progress = 0f
    protected val density = resources.displayMetrics.density

    fun play(onEnd: () -> Unit) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = this@AnimatedOverlay.duration
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) = onEnd()
            })
            start()
        }
    }

    /** Плавное появление и исчезновение на краях анимации. */
    protected fun envelope(fadeIn: Float, fadeOut: Float): Float = when {
        progress < fadeIn -> progress / fadeIn
        progress > 1 - fadeOut -> (1 - progress) / fadeOut
        else -> 1f
    }
}

/** Horizon Light: светящаяся рамка по краю экрана, два «вдоха». */
class HorizonView(ctx: Context, private val accent: Int) : AnimatedOverlay(ctx, 2400) {

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val rotate = Matrix()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // для размытия свечения
        glow.strokeWidth = 14 * density
        glow.maskFilter = BlurMaskFilter(16 * density, BlurMaskFilter.Blur.NORMAL)
        core.strokeWidth = 3 * density
    }

    override fun onDraw(canvas: Canvas) {
        // Два импульса яркости
        val pulse = kotlin.math.sin(progress * Math.PI * 2).let { (it * it).toFloat() }
        val alpha = (255 * pulse).toInt()
        val light = blend(accent, Color.WHITE, 0.45f)
        val shader = SweepGradient(width / 2f, height / 2f,
            intArrayOf(accent, light, accent, light, accent), null)
        rotate.setRotate(progress * 360f, width / 2f, height / 2f)
        shader.setLocalMatrix(rotate)

        val r = 40 * density
        rect.set(core.strokeWidth / 2, core.strokeWidth / 2, width - core.strokeWidth / 2, height - core.strokeWidth / 2)
        glow.shader = shader
        glow.alpha = alpha
        canvas.drawRoundRect(rect, r, r, glow)
        core.shader = shader
        core.alpha = alpha
        canvas.drawRoundRect(rect, r, r, core)
    }

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )
}

/** Анимация зарядки: кольцо заполняется до текущего процента. */
enum class ChargeStyle(val title: String) { RING("Кольцо"), BOLT("Молния"), WAVE("Волна") }

class ChargingView(ctx: Context, private val accent: Int, private val level: Int) : AnimatedOverlay(ctx, 3200) {

    private val style = ChargeStyle.entries.getOrElse(Features.chargeStyle(ctx)) { ChargeStyle.RING }
    private val dim = Paint().apply { color = Color.BLACK }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0x33FFFFFF
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val percent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val oval = RectF()
    private val ease = DecelerateInterpolator(2f)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        val a = envelope(0.12f, 0.2f)
        dim.alpha = (200 * a).toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)

        val cx = width / 2f
        val cy = height / 2f
        val fill = ease.getInterpolation((progress / 0.45f).coerceAtMost(1f))
        val shown = (level * fill).toInt()

        when (style) {
            ChargeStyle.RING -> drawRing(canvas, cx, cy, a, fill, shown)
            ChargeStyle.BOLT -> drawBolt(canvas, cx, cy, a, fill, shown)
            ChargeStyle.WAVE -> drawWave(canvas, cx, cy, a, fill, shown)
        }
    }

    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, a: Float, fill: Float, shown: Int) {
        val radius = minOf(width, height) * 0.28f
        val stroke = 12 * density
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)
        track.strokeWidth = stroke
        track.alpha = (0x33 * a).toInt()
        canvas.drawArc(oval, 0f, 360f, false, track)
        arc.strokeWidth = stroke
        arc.color = accent
        arc.alpha = (255 * a).toInt()
        arc.setShadowLayer(14 * density, 0f, 0f, accent)
        canvas.drawArc(oval, -90f, 360f * level / 100f * fill, false, arc)
        percent.textSize = radius * 0.55f
        percent.alpha = (255 * a).toInt()
        canvas.drawText("$shown%", cx, cy + percent.textSize * 0.35f, percent)
        drawLabel(canvas, cx, cy + radius + 48 * density, a)
    }

    private fun boltPath(cx: Float, cy: Float, h: Float): Path {
        val w = h * 0.5f
        return Path().apply {
            moveTo(cx + w * 0.1f, cy - h / 2)
            lineTo(cx - w * 0.5f, cy + h * 0.1f)
            lineTo(cx - w * 0.02f, cy + h * 0.1f)
            lineTo(cx - w * 0.1f, cy + h / 2)
            lineTo(cx + w * 0.5f, cy - h * 0.15f)
            lineTo(cx + w * 0.02f, cy - h * 0.15f)
            close()
        }
    }

    private fun drawBolt(canvas: Canvas, cx: Float, cy: Float, a: Float, fill: Float, shown: Int) {
        val h = minOf(width, height) * 0.5f
        val path = boltPath(cx, cy - h * 0.05f, h)
        // Контур молнии.
        arc.style = Paint.Style.STROKE
        arc.strokeWidth = 4 * density
        arc.color = 0x55FFFFFF
        arc.alpha = (0x55 * a).toInt()
        arc.clearShadowLayer()
        canvas.drawPath(path, arc)
        // Заливка снизу вверх по проценту.
        val save = canvas.save()
        canvas.clipPath(path)
        arc.style = Paint.Style.FILL
        arc.color = accent
        arc.alpha = (255 * a).toInt()
        arc.setShadowLayer(24 * density, 0f, 0f, accent)
        val top = cy - h * 0.55f + h * 1.1f * (1 - level / 100f * fill)
        canvas.drawRect(cx - h, top, cx + h, cy + h, arc)
        canvas.restoreToCount(save)
        arc.clearShadowLayer()
        percent.textSize = h * 0.28f
        percent.alpha = (255 * a).toInt()
        canvas.drawText("$shown%", cx, cy + h * 0.78f, percent)
        drawLabel(canvas, cx, cy + h * 0.95f, a)
    }

    private fun drawWave(canvas: Canvas, cx: Float, cy: Float, a: Float, fill: Float, shown: Int) {
        val radius = minOf(width, height) * 0.3f
        val circle = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        arc.style = Paint.Style.STROKE
        arc.strokeWidth = 3 * density
        arc.color = accent
        arc.alpha = (0xAA * a).toInt()
        arc.clearShadowLayer()
        canvas.drawPath(circle, arc)
        val save = canvas.save()
        canvas.clipPath(circle)
        // Поднимающаяся «жидкость» с волной.
        val waterTop = cy + radius - radius * 2 * (level / 100f * fill)
        val wave = Path().apply {
            moveTo(cx - radius, waterTop)
            val amp = radius * 0.06f
            var x = cx - radius
            while (x <= cx + radius) {
                val y = waterTop + amp * kotlin.math.sin((x / radius * 3 + progress * 12).toDouble()).toFloat()
                lineTo(x, y)
                x += radius * 0.1f
            }
            lineTo(cx + radius, cy + radius)
            lineTo(cx - radius, cy + radius)
            close()
        }
        arc.style = Paint.Style.FILL
        arc.color = accent
        arc.alpha = (220 * a).toInt()
        canvas.drawPath(wave, arc)
        canvas.restoreToCount(save)
        percent.textSize = radius * 0.5f
        percent.alpha = (255 * a).toInt()
        canvas.drawText("$shown%", cx, cy + percent.textSize * 0.35f, percent)
        drawLabel(canvas, cx, cy + radius + 48 * density, a)
    }

    private fun drawLabel(canvas: Canvas, cx: Float, y: Float, a: Float) {
        label.textSize = 16 * density
        label.alpha = (0xCC * a).toInt()
        canvas.drawText("⚡ Зарядка", cx, y, label)
    }
}
