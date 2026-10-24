package com.givebackmyoneplus

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

/** Аналог трёхпозиционного переключателя OnePlus: звук / вибрация / тишина. */
object AlertSlider {

    enum class Mode(val title: String, val icon: Int, val ringer: Int) {
        RING("Звук", R.drawable.ic_slider_ring, AudioManager.RINGER_MODE_NORMAL),
        VIBRATE("Вибрация", R.drawable.ic_slider_vibrate, AudioManager.RINGER_MODE_VIBRATE),
        SILENT("Без звука", R.drawable.ic_slider_silent, AudioManager.RINGER_MODE_SILENT),
    }

    fun current(ctx: Context): Mode {
        val am = ctx.getSystemService(AudioManager::class.java)
        return Mode.entries.firstOrNull { it.ringer == am.ringerMode } ?: Mode.RING
    }

    fun hasDndAccess(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /** false — нужен доступ к «Не беспокоить». */
    fun set(ctx: Context, mode: Mode): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java)
        val touchesDnd = mode == Mode.SILENT || current(ctx) == Mode.SILENT
        if (touchesDnd && !hasDndAccess(ctx)) return false
        return try {
            am.ringerMode = mode.ringer
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun next(ctx: Context): Mode = Mode.entries[(current(ctx).ordinal + 1) % Mode.entries.size]
}
