package com.givebackmyoneplus

import android.content.Context
import android.content.SharedPreferences

/** Общие настройки приложения (тот же файл, что и у темы). */
object Features {
    const val LIVE = "live"
    const val HORIZON = "horizon"
    const val CHARGING = "charging"
    private const val DEFAULT_ACCENT = 0xFFEB0029.toInt()

    fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun enabled(ctx: Context, key: String) = prefs(ctx).getBoolean(key, false)

    fun accent(ctx: Context) = prefs(ctx).getInt("accent", DEFAULT_ACCENT)
}
