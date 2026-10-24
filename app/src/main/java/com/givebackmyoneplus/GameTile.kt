package com.givebackmyoneplus

import android.app.NotificationManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Плитка «Игровой режим»: включает «Не беспокоить», чтобы уведомления и
 * звонки не мешали в игре. Повторное нажатие выключает.
 */
class GameTile : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Дайте доступ к «Не беспокоить»", Toast.LENGTH_LONG).show()
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0,
                    android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            return
        }
        val on = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
        Toast.makeText(this, if (on) "Игровой режим выключен" else "Игровой режим включён", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val nm = getSystemService(NotificationManager::class.java)
        val on = nm.isNotificationPolicyAccessGranted &&
            nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
        tile.label = "Игровой режим"
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_game)
        tile.updateTile()
    }
}
