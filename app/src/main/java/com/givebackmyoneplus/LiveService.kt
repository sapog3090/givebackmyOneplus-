package com.givebackmyoneplus

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat

/**
 * Слушает уведомления: питает Live Alerts и Horizon Light,
 * а пока работает — ловит подключение зарядки для анимации.
 */
class LiveService : NotificationListenerService() {

    private val items = LinkedHashMap<String, LiveItem>()
    private val lastAlert = HashMap<String, Long>()
    private var lastHorizon = 0L
    private var pill: LivePill? = null

    private val power = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!Features.enabled(context, Features.CHARGING)) return
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0) Overlays.charging(context, level * 100 / scale)
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    override fun onListenerConnected() {
        pill = LivePill(this)
        ContextCompat.registerReceiver(
            this, power, IntentFilter(Intent.ACTION_POWER_CONNECTED), ContextCompat.RECEIVER_EXPORTED
        )
        Features.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        try {
            activeNotifications?.forEach { consider(it) }
        } catch (_: Exception) {
        }
        refresh()
    }

    override fun onListenerDisconnected() {
        try { unregisterReceiver(power) } catch (_: Exception) {}
        Features.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        pill?.hide()
        pill = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (Features.enabled(this, Features.HORIZON) && shouldLight(sbn)) Overlays.horizon(this)
        consider(sbn)
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        items.remove(sbn.key)
        refresh()
    }

    /** Новое «настоящее» уведомление: не постоянное, не сводка, со звуком/всплыванием. */
    private fun shouldLight(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return false
        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) == true &&
            ranking.importance < NotificationManager.IMPORTANCE_DEFAULT
        ) return false
        val now = System.currentTimeMillis()
        val last = lastAlert[sbn.key]
        lastAlert[sbn.key] = now
        if (last != null && now - last < 10_000) return false // обновление того же уведомления
        if (now - lastHorizon < 1500) return false
        lastHorizon = now
        return true
    }

    private fun consider(sbn: StatusBarNotification) {
        val item = parse(sbn)
        if (item != null) items[sbn.key] = item else items.remove(sbn.key)
    }

    private fun parse(sbn: StatusBarNotification): LiveItem? {
        if (sbn.packageName == packageName) return null
        val n = sbn.notification
        val ex = n.extras
        val ongoing = sbn.isOngoing
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val chrono = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        val icon = n.smallIcon
        val token = if (Build.VERSION.SDK_INT >= 33) {
            ex.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            ex.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        }

        return when {
            n.category == Notification.CATEGORY_CALL && ongoing ->
                LiveItem(sbn.key, 5, icon, title.ifBlank { "Звонок" }, n.contentIntent,
                    chronoWhen = if (chrono) n.`when` else null)

            n.category == Notification.CATEGORY_NAVIGATION && ongoing ->
                LiveItem(sbn.key, 4, icon, title.ifBlank { text }, n.contentIntent)

            ongoing && (chrono || n.category == Notification.CATEGORY_STOPWATCH) ->
                LiveItem(sbn.key, 3, icon, title, n.contentIntent, chronoWhen = n.`when`,
                    countDown = ex.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))

            token != null -> {
                val playing = try {
                    MediaController(this, token).playbackState?.state == PlaybackState.STATE_PLAYING
                } catch (e: Exception) {
                    false
                }
                if (!playing) null
                else LiveItem(sbn.key, 2, icon,
                    listOf(title, text).filter { it.isNotBlank() }.joinToString(" · "), n.contentIntent)
            }

            ongoing && ex.getInt(Notification.EXTRA_PROGRESS_MAX) > 0 &&
                !ex.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE) -> {
                val pct = ex.getInt(Notification.EXTRA_PROGRESS) * 100 / ex.getInt(Notification.EXTRA_PROGRESS_MAX)
                LiveItem(sbn.key, 1, icon, "$pct% · $title", n.contentIntent)
            }

            else -> null
        }
    }

    private fun refresh() {
        val p = pill ?: return
        val top = items.values.maxByOrNull { it.priority }
        if (top == null || !Features.enabled(this, Features.LIVE) || !Overlays.canDraw(this)) {
            p.hide()
        } else {
            p.show(top, Features.accent(this))
        }
    }
}
