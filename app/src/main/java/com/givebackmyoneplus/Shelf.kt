package com.givebackmyoneplus

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shelf (Полка) в стиле OxygenOS: тонкий язычок у правого края, по нажатию
 * выезжает панель с батареей, памятью и быстрыми действиями.
 */
object Shelf {

    private var handle: View? = null
    private var panel: View? = null
    private var torchOn = false

    fun enabled(ctx: Context) = Features.enabled(ctx, Features.SHELF)

    fun sync(ctx: Context) {
        if (enabled(ctx) && Overlays.canDraw(ctx)) showHandle(ctx.applicationContext) else hideAll()
    }

    private fun wm(ctx: Context) = ctx.getSystemService(WindowManager::class.java)
    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun showHandle(ctx: Context) {
        if (handle != null) return
        val h = View(ctx).apply {
            background = GradientDrawable().apply {
                setColor(0x55FFFFFF)
                cornerRadius = dp(ctx, 4).toFloat()
            }
            setOnClickListener { openPanel(ctx) }
        }
        val lp = WindowManager.LayoutParams(
            dp(ctx, 6), dp(ctx, 90),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        try {
            wm(ctx).addView(h, lp)
            handle = h
        } catch (_: Exception) {
        }
    }

    private fun hideAll() {
        handle?.let { runCatching { wm(it.context).removeView(it) } }
        panel?.let { runCatching { wm(it.context).removeView(it) } }
        handle = null
        panel = null
    }

    fun closePanel() {
        panel?.let { runCatching { wm(it.context).removeView(it) } }
        panel = null
    }

    private fun sp(ctx: Context, v: Float, w: View): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, ctx.resources.displayMetrics)

    private fun openPanel(ctx: Context) {
        if (panel != null) return
        val accent = Features.accent(ctx)
        val pad = dp(ctx, 16)

        fun card(title: String, value: String): LinearLayout {
            val t = TextView(ctx).apply {
                text = title; setTextColor(0xAAFFFFFF.toInt()); textSize = 12f
            }
            val v = TextView(ctx).apply {
                text = value; setTextColor(Color.WHITE); textSize = 20f
            }
            return LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                background = GradientDrawable().apply {
                    setColor(0xFF1C1C1F.toInt()); cornerRadius = dp(ctx, 20).toFloat()
                }
                addView(t); addView(v)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(ctx, 10) }
                layoutParams = lp
            }
        }

        fun action(text: String, onClick: () -> Unit): TextView = TextView(ctx).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(pad, dp(ctx, 14), pad, dp(ctx, 14))
            background = GradientDrawable().apply {
                setColor(0xFF26262A.toInt()); cornerRadius = dp(ctx, 20).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(ctx, 10) }
            layoutParams = lp
            setOnClickListener { onClick() }
        }

        val bm = ctx.getSystemService(BatteryManager::class.java)
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = stat.availableBytes / 1_000_000_000.0
        val totalGb = stat.totalBytes / 1_000_000_000.0

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(ctx, 48), pad, pad)
            val title = TextView(ctx).apply {
                text = "Полка"; setTextColor(Color.WHITE); textSize = 26f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(title)
            addView(card("Батарея", "$battery%"))
            addView(card("Свободно", "%.1f / %.0f ГБ".format(freeGb, totalGb)))
            addView(action("🔦 Фонарик") { toggleTorch(ctx) })
            addView(action("🧘 Zen 20 минут") {
                closePanel()
                ctx.startActivity(Intent(ctx, ZenActivity::class.java)
                    .putExtra("minutes", 20).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
            addView(action("⚙ Открыть givebackmyOneplus") {
                closePanel()
                ctx.startActivity(Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
        }

        // Полупрозрачный фон на весь экран — тап закрывает.
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xB3000000.toInt())
            gravity = Gravity.END
            setOnClickListener { closePanel() }
            val panelBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { setColor(0xFF0E0E10.toInt()) }
                addView(content)
                isClickable = true // чтобы тап по панели не закрывал
                layoutParams = LinearLayout.LayoutParams(
                    (ctx.resources.displayMetrics.widthPixels * 0.82f).toInt(),
                    LinearLayout.LayoutParams.MATCH_PARENT,
                )
            }
            addView(panelBox)
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        try {
            wm(ctx).addView(root, lp)
            panel = root
            root.alpha = 0f
            root.animate().alpha(1f).setDuration(180).start()
        } catch (_: Exception) {
        }
    }

    private fun toggleTorch(ctx: Context) {
        val cm = ctx.getSystemService(CameraManager::class.java)
        try {
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        } catch (_: Exception) {
        }
    }
}
