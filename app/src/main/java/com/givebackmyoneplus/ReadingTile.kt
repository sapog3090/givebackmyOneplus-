package com.givebackmyoneplus

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Плитка «Режим чтения»: включает чёрно-белый экран (как у OnePlus),
 * через системный монохромный фильтр. Требует Shizuku (права ADB).
 */
class ReadingTile : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() = refresh()

    override fun onClick() {
        if (!Shell.granted()) {
            Toast.makeText(this, "Нужен Shizuku (вкладка «Очистка»)", Toast.LENGTH_LONG).show()
            return
        }
        val turnOn = !ReadingMode.isOn(this)
        scope.launch {
            val ok = withContext(Dispatchers.IO) { ReadingMode.set(turnOn) }
            if (!ok) Toast.makeText(this@ReadingTile, "Не удалось переключить", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = ReadingMode.isOn(this)
        tile.label = "Режим чтения"
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_reading)
        tile.updateTile()
    }
}

/** Чёрно-белый экран через системный монохромный фильтр (secure settings). */
object ReadingMode {
    private const val ENABLED = "accessibility_display_daltonizer_enabled"
    private const val MODE = "accessibility_display_daltonizer"

    fun isOn(ctx: android.content.Context): Boolean =
        android.provider.Settings.Secure.getInt(ctx.contentResolver, ENABLED, 0) == 1 &&
            android.provider.Settings.Secure.getInt(ctx.contentResolver, MODE, -1) == 0

    /** Блокирующий вызов. true — получилось. */
    fun set(on: Boolean): Boolean {
        val cmd = if (on) {
            "settings put secure $MODE 0; settings put secure $ENABLED 1"
        } else {
            "settings put secure $ENABLED 0"
        }
        return runCatching { Shell.run(cmd).first == 0 }.getOrDefault(false)
    }
}
