package com.givebackmyoneplus

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

/** Раздел «Функции»: разрешения, Alert Slider, Live Alerts, Horizon Light, зарядка. */
class FeaturesSection(private val activity: AppCompatActivity) {

    private val btnNotif: MaterialButton = activity.findViewById(R.id.btnPermNotif)
    private val btnOverlay: MaterialButton = activity.findViewById(R.id.btnPermOverlay)
    private val btnDnd: MaterialButton = activity.findViewById(R.id.btnPermDnd)
    private val slider: MaterialButtonToggleGroup = activity.findViewById(R.id.sliderGroup)
    private val status: android.widget.TextView = activity.findViewById(R.id.serviceStatus)
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

        activity.findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            // На Android 13+ без этого разрешения уведомление службы не видно.
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 77)
            }
            try {
                FeatureService.start(activity)
                toast("Служба запускается…")
            } catch (e: Exception) {
                toast("Не запустилась: ${e.message}")
            }
            // Переподключение слушателя занимает несколько секунд.
            status.postDelayed({ updateStatus() }, 2000)
            status.postDelayed({ updateStatus() }, 6000)
        }

        setupPillOffset()

        bindSwitch(R.id.swLive, Features.LIVE)
        bindSwitch(R.id.swHorizon, Features.HORIZON)
        bindSwitch(R.id.swCharging, Features.CHARGING)

        activity.findViewById<MaterialButton>(R.id.btnTestHorizon).setOnClickListener {
            if (requireOverlay()) Overlays.horizon(activity)
        }
        activity.findViewById<MaterialButton>(R.id.btnTestCharging).setOnClickListener {
            if (requireOverlay()) Overlays.charging(activity, 76)
        }

        // Zen Mode
        mapOf(R.id.zen20 to 20, R.id.zen30 to 30, R.id.zen60 to 60).forEach { (id, min) ->
            activity.findViewById<MaterialButton>(id).setOnClickListener {
                activity.startActivity(
                    Intent(activity, ZenActivity::class.java).putExtra("minutes", min)
                )
            }
        }

        // Режим чтения (ч/б) через Shizuku
        activity.findViewById<MaterialButton>(R.id.btnReading).setOnClickListener { toggleReading() }
    }

    fun refresh() {
        val notif = NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.packageName)
        btnNotif.text = mark(notif, "Доступ к уведомлениям")
        btnOverlay.text = mark(Overlays.canDraw(activity), "Поверх других окон")
        btnDnd.text = mark(AlertSlider.hasDndAccess(activity), "Режим «Не беспокоить»")
        syncSlider()
        // На случай, если службу убили — поднять снова, когда открыт экран.
        FeatureService.sync(activity)
        updateStatus()
        updateReadingButton()
    }

    private var testPill: LivePill? = null

    private fun toggleReading() {
        if (!Shell.granted()) {
            toast("Нужен Shizuku (вкладка «Очистка»)")
            return
        }
        val turnOn = !ReadingMode.isOn(activity)
        activity.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { ReadingMode.set(turnOn) }
            if (!ok) toast("Не удалось переключить")
            updateReadingButton()
        }
    }

    private fun updateReadingButton() {
        activity.findViewById<MaterialButton>(R.id.btnReading).text =
            if (ReadingMode.isOn(activity)) "Выключить ч/б" else "Включить ч/б"
    }

    private fun setupPillOffset() {
        val label = activity.findViewById<android.widget.TextView>(R.id.pillOffsetLabel)
        val slider = activity.findViewById<com.google.android.material.slider.Slider>(R.id.pillOffset)
        val density = activity.resources.displayMetrics.density
        val id = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        val autoDp = ((if (id > 0) activity.resources.getDimensionPixelSize(id) else (28 * density).toInt()) / density).toInt() + 2
        val saved = Features.pillOffsetDp(activity)
        slider.value = (if (saved >= 0) saved else autoDp).coerceIn(0, 120).toFloat()
        label.text = "Высота пилюли: ${slider.value.toInt()} dp"
        slider.addOnChangeListener { _, value, fromUser ->
            label.text = "Высота пилюли: ${value.toInt()} dp"
            if (fromUser) {
                Features.prefs(activity).edit().putInt(Features.PILL_Y, value.toInt()).apply()
                testPill?.show(sampleItem(), Features.accent(activity))
            }
        }
        activity.findViewById<MaterialButton>(R.id.btnTestPill).setOnClickListener {
            if (!requireOverlay()) return@setOnClickListener
            val pill = testPill ?: LivePill(activity.applicationContext).also { testPill = it }
            pill.show(sampleItem(), Features.accent(activity))
            status.postDelayed({ pill.hide() }, 5000)
        }
    }

    private fun sampleItem() = LiveItem(
        key = "gbmo_test", priority = 0,
        icon = android.graphics.drawable.Icon.createWithResource(activity, R.drawable.ic_slider_ring),
        text = "Тест Live Alerts 🍉", intent = null,
    )

    private fun updateStatus() {
        status.text = "Служба: " + (if (FeatureService.running) "✅ работает" else "❌ не работает") +
            "\nСлушатель уведомлений: " + (if (LiveService.connected) "✅ подключён" else "❌ не подключён") +
            "\nПоследнее: ${LiveService.lastEvent}" +
            "\nЗарядка: ${FeatureService.lastPower}"
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
            // Поднять/остановить постоянную службу под текущий набор функций.
            FeatureService.sync(activity)
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
