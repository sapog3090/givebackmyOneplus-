package com.givebackmyoneplus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** После перезагрузки снова поднимаем службу функций. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FeatureService.sync(context)
        }
    }
}
