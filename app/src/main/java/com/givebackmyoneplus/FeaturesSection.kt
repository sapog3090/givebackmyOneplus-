package com.givebackmyoneplus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

/** Раздел «Функции»: разрешения, Alert Slider, Live Alerts, Horizon Light, зарядка. */
class FeaturesSection(private val activity: Activity) {

    private val btnNotif: MaterialButton = activity.findViewById(R.id.btnPermNotif)
    private val btnOverlay: MaterialButton = activity.findViewById(R.id.btnPermOverlay)
    private val btnDnd: MaterialButton = activity.findViewById(R.id.btnPermDnd)
    private val slider: MaterialButtonToggleGroup = activity.findViewById(R.id.sliderGroup)
    private var syncing = false

    private val sliderIds = mapOf(
        AlertSlider.Mode.RING to R.id.slRing,
        AlertSlider.Mode.VIBRATE to R.id.slVibrate,
        AlertSlider.Mode.SILENT to R.id.slSilent,
    )

    init {
        btnNotif.setOnClickListener { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btnOverlay.setOnClickListener {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}")))
        }
        btnDnd.setOnClickListener { open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        activity.findViewById<MaterialButton>(R.id.btnAppSettings).setOnClickListener {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))
        }

        slider.addOnButtonCheckedListener { _, id, checked ->
            if (!checked || syncing) return@addOnButtonCheckedListener
            val mode = sliderIds.entries.first { it.value == id }.key
            if (!AlertSlider.set(activity, mode)) {
                toast("Дайте доступ к «Не беспокоить»")
                open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                syncSlider()
            } else {
                toast(mode.title)
            }
        }

        bindSwitch(R.id.swLive, Features.LIVE)
        bindSwitch(R.id.swHorizon, Features.HORIZON)
        bindSwitch(R.id.swCharging, Features.CHARGING)

        activity.findViewById<MaterialButton>(R.id.btnTestHorizon).setOnClickListener {
            if (requireOverlay()) Overlays.horizon(activity)
        }
        activity.findViewById<MaterialButton>(R.id.btnTestCharging).setOnClickListener {
            if (requireOverlay()) Overlays.charging(activity, 76)
        }
    }

    fun refresh() {
        val notif = NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.packageName)
        btnNotif.text = mark(notif, "Доступ к уведомлениям")
        btnOverlay.text = mark(Overlays.canDraw(activity), "Поверх других окон")
        btnDnd.text = mark(AlertSlider.hasDndAccess(activity), "Режим «Не беспокоить»")
        syncSlider()
    }

    private fun syncSlider() {
        syncing = true
        slider.check(sliderIds.getValue(AlertSlider.current(activity)))
        syncing = false
    }

    private fun bindSwitch(id: Int, key: String) {
        val sw = activity.findViewById<MaterialSwitch>(id)
        sw.isChecked = Features.enabled(activity, key)
        sw.setOnCheckedChangeListener { _, on ->
            Features.prefs(activity).edit().putBoolean(key, on).apply()
            if (on) requireOverlay()
        }
    }

    /** Проверяет нужные разрешения; если чего-то нет — подсказывает. */
    private fun requireOverlay(): Boolean {
        if (!Overlays.canDraw(activity)) {
            toast("Разрешите «Поверх других окон»")
            return false
        }
        val notif = NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.packageName)
        if (!notif) toast("Для работы в фоне включите «Доступ к уведомлениям»")
        return true
    }

    private fun mark(ok: Boolean, title: String) = if (ok) "✅ $title" else "⚠ $title — дать доступ"

    private fun open(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            toast("Не удалось открыть настройки")
        }
    }

    private fun toast(s: String) = Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
}
