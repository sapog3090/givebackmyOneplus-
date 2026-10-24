package com.givebackmyoneplus

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class LauncherApp(val pkg: String, val label: String, val icon: Drawable)

object Apps {
    /** Все приложения с иконкой на рабочем столе (по одному на пакет). Блокирующий вызов. */
    fun launcherApps(context: Context): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }
}
