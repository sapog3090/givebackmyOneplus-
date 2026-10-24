package com.givebackmyoneplus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Постоянная фоновая служба. HyperOS агрессивно убивает фоновые процессы,
 * поэтому держим процесс живым через foreground-уведомление: тогда
 * NotificationListenerService (Live Alerts, Horizon) не выгружается, а
 * здесь же ловим подключение зарядки для анимации.
 */
class FeatureService : Service() {

    companion object {
        private const val CHANNEL = "gbmo_features"
        private const val NOTIF_ID = 1

        @Volatile var running = false
        @Volatile var lastPower = "—"

        /** Запуск по кнопке — независимо от переключателей. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, FeatureService::class.java))
            forceListenerReconnect(context)
        }

        /**
         * После обновления приложения система часто «помнит» доступ к уведомлениям,
         * но не подключает слушатель. Выключение и включение компонента заставляет
         * её подключить заново.
         */
        fun forceListenerReconnect(context: Context) {
            val cn = ComponentName(context, LiveService::class.java)
            val pm = context.packageManager
            try {
                pm.setComponentEnabledSetting(
                    cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {
            }
            requestListenerRebind(context)
        }

        /** Запустить, если включена хоть одна функция; иначе остановить. */
        fun sync(context: Context) {
            val any = Features.enabled(context, Features.LIVE) ||
                Features.enabled(context, Features.HORIZON) ||
                Features.enabled(context, Features.CHARGING) ||
                Features.enabled(context, Features.SHELF)
            Shelf.sync(context)
            val intent = Intent(context, FeatureService::class.java)
            if (any) {
                ContextCompat.startForegroundService(context, intent)
                requestListenerRebind(context)
            } else {
                context.stopService(intent)
            }
        }

        /** Разбудить слушатель уведомлений (Live Alerts/Horizon). */
        fun requestListenerRebind(context: Context) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, LiveService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }

    private val power = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastPower = "зарядка подключена в ${java.time.LocalTime.now().withNano(0)}"
            if (!Features.enabled(context, Features.CHARGING)) return
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0) Overlays.charging(context, level * 100 / scale)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        startForeground()
        ContextCompat.registerReceiver(
            this, power, IntentFilter(Intent.ACTION_POWER_CONNECTED), ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        requestListenerRebind(this)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        try { unregisterReceiver(power) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Фоновая работа", NotificationManager.IMPORTANCE_MIN)
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle("givebackmyOneplus")
            .setContentText("Функции OnePlus активны")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }
}
