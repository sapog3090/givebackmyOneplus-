package com.givebackmyoneplus

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

/** Виджет в стиле OxygenOS: крупные тонкие часы, дата и заряд акцентом. */
class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        // Обновляем заряд при изменении батареи.
        if (intent.action == Intent.ACTION_BATTERY_CHANGED || intent.action == Intent.ACTION_POWER_CONNECTED ||
            intent.action == Intent.ACTION_POWER_DISCONNECTED
        ) {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, ClockWidget::class.java)).forEach { render(ctx, mgr, it) }
        }
    }

    private fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_clock)
        val battery = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val charging = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val pct = if (level >= 0) level * 100 / scale else 0
        views.setTextViewText(R.id.wBattery, (if (charging) "⚡ " else "🔋 ") + "$pct%")
        views.setTextColor(R.id.wBattery, Features.accent(ctx))
        // Открыть приложение по нажатию на виджет.
        val pi = android.app.PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.wTime, pi)
        mgr.updateAppWidget(id, views)
    }
}
