package com.givebackmyoneplus

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

data class LiveItem(
    val key: String,
    val priority: Int,
    val icon: Icon?,
    val text: String,
    val intent: PendingIntent?,
    /** Время в System.currentTimeMillis() для секундомера/таймера, или null. */
    val chronoWhen: Long? = null,
    val countDown: Boolean = false,
)

/** «Пилюля» Live Alerts у фронтальной камеры. */
class LivePill(private val ctx: Context) {

    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val density = ctx.resources.displayMetrics.density
    private var root: LinearLayout? = null
    private lateinit var icon: ImageView
    private lateinit var text: TextView
    private lateinit var chrono: Chronometer
    private var current: LiveItem? = null

    /** Ключ+текст элемента, скрытого долгим нажатием (до его изменения). */
    var dismissed: String? = null
        private set

    private fun dp(v: Int) = (v * density).toInt()

    private fun create(): LinearLayout {
        icon = ImageView(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)) }
        text = TextView(ctx).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(190)
            setPadding(dp(8), 0, 0, 0)
        }
        chrono = Chronometer(ctx).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(8), 0, 0, 0)
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(30)
            setPadding(dp(12), dp(5), dp(14), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(100).toFloat()
            }
            addView(icon)
            addView(text)
            addView(chrono)
            setOnClickListener {
                try { current?.intent?.send() } catch (_: Exception) {}
            }
            setOnLongClickListener {
                dismissed = current?.let { it.key + it.text }
                hide()
                true
            }
        }
    }

    private fun params() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = offsetY()
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    /** Отступ сверху: из ползунка или чуть ниже строки состояния (под камерой). */
    private fun offsetY(): Int {
        val custom = Features.pillOffsetDp(ctx)
        if (custom >= 0) return dp(custom)
        val id = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
        val bar = if (id > 0) ctx.resources.getDimensionPixelSize(id) else dp(28)
        return bar + dp(2)
    }

    fun show(item: LiveItem, accent: Int) {
        if (dismissed == item.key + item.text) return
        dismissed = null
        root?.let { existing ->
            // Высоту могли поменять ползунком — двигаем уже показанную пилюлю.
            val lp = existing.layoutParams as? WindowManager.LayoutParams
            if (lp != null && lp.y != offsetY()) {
                lp.y = offsetY()
                try { wm.updateViewLayout(existing, lp) } catch (_: Exception) {}
            }
        }
        val view = root ?: create().also {
            try {
                wm.addView(it, params())
            } catch (e: Exception) {
                return
            }
            root = it
            it.scaleX = 0.5f
            it.alpha = 0f
            it.animate().scaleX(1f).alpha(1f).setDuration(250).start()
        }
        current = item

        val drawable = item.icon?.loadDrawable(ctx)
        icon.setImageDrawable(drawable)
        icon.setColorFilter(accent, PorterDuff.Mode.SRC_IN)
        icon.visibility = if (drawable != null) View.VISIBLE else View.GONE
        text.text = item.text
        text.visibility = if (item.text.isNotBlank()) View.VISIBLE else View.GONE

        if (item.chronoWhen != null) {
            chrono.base = SystemClock.elapsedRealtime() + (item.chronoWhen - System.currentTimeMillis())
            chrono.isCountDown = item.countDown
            chrono.visibility = View.VISIBLE
            chrono.start()
        } else {
            chrono.stop()
            chrono.visibility = View.GONE
        }
        view.requestLayout()
    }

    fun hide() {
        val view = root ?: return
        root = null
        current = null
        chrono.stop()
        try { wm.removeView(view) } catch (_: Exception) {}
    }
}
