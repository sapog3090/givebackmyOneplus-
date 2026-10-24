package com.givebackmyoneplus

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** Плитка «Alert Slider» в шторке: по нажатию звук → вибрация → тишина. */
class AlertSliderTile : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val mode = AlertSlider.next(this)
        if (AlertSlider.set(this, mode)) {
            refresh()
        } else {
            Toast.makeText(this, "Дайте доступ к «Не беспокоить»", Toast.LENGTH_LONG).show()
            openSettings(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openSettings(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val mode = AlertSlider.current(this)
        tile.label = "Alert Slider"
        tile.subtitle = mode.title
        tile.icon = Icon.createWithResource(this, mode.icon)
        tile.state = if (mode == AlertSlider.Mode.RING) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
