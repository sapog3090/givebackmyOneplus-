package com.givebackmyoneplus

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Выполнение команд с правами ADB через Shizuku. */
object Shell {

    fun shizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    fun granted(): Boolean = shizukuRunning() &&
        !Shizuku.isPreV11() &&
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    /** Блокирующий вызов. Возвращает код выхода и вывод. */
    fun run(cmd: String): Pair<Int, String> {
        // Shizuku.newProcess скрыт в API 13, но остаётся рабочим.
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        return code to (out + err).trim()
    }
}
