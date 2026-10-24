package com.givebackmyoneplus

import android.content.Context
import android.content.SharedPreferences

/** Общие настройки приложения (тот же файл, что и у темы). */
object Features {
    const val LIVE = "live"
    const val HORIZON = "horizon"
    const val CHARGING = "charging"
    const val PILL_Y = "pill_y"
    const val CHARGE_STYLE = "charge_style"
    private const val DEFAULT_ACCENT = 0xFFEB0029.toInt()

    fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun enabled(ctx: Context, key: String) = prefs(ctx).getBoolean(key, false)

    /** Отступ пилюли Live Alerts в dp; -1 — автоматически под строкой состояния. */
    fun pillOffsetDp(ctx: Context) = prefs(ctx).getInt(PILL_Y, -1)

    /** Стиль анимации зарядки: 0 кольцо, 1 молния, 2 волна. */
    fun chargeStyle(ctx: Context) = prefs(ctx).getInt(CHARGE_STYLE, 0)

    fun accent(ctx: Context) = prefs(ctx).getInt("accent", DEFAULT_ACCENT)
}
