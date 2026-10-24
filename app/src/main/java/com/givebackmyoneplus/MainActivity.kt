package com.givebackmyoneplus

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHIZUKU_REQUEST = 42
        private const val GITHUB = "https://github.com/sapog3090/givebackmyOneplus-"
        private val ACCENTS = listOf(
            0xFFEB0029, // красный OnePlus
            0xFFFF6A00, // оранжевый
            0xFFF5C400, // жёлтый
            0xFF34C759, // зелёный
            0xFF00B8D4, // бирюзовый
            0xFF2F7CF6, // синий
            0xFF8E5CF7, // фиолетовый
            0xFFFF4D8D, // розовый
            0xFFB0B0B8, // серебро
        ).map { it.toInt() }
    }

    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var accent = ACCENTS.first()
    private var shape = IconShape.NATIVE
    private var style = IconStyle.TINT

    private lateinit var preview: IconPreviewView
    private lateinit var nav: BottomNavigationView
    private val checks = mutableMapOf<String, CheckBox>()
    private lateinit var features: FeaturesSection

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        runOnUiThread { refreshShizuku() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accent = prefs.getInt("accent", accent)
        shape = IconShape.entries.getOrElse(prefs.getInt("shape2", 0)) { IconShape.NATIVE }
        style = IconStyle.entries.getOrElse(prefs.getInt("style2", 0)) { IconStyle.TINT }

        setupNav()
        setupTheme()
        features = FeaturesSection(this)
        setupClean()
        setupAbout()
        applyAccent()
    }

    override fun onResume() {
        super.onResume()
        refreshShizuku()
        refreshBloat()
        features.refresh()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    // ---------- Навигация ----------

    private fun setupNav() {
        nav = findViewById(R.id.nav)
        val sections = mapOf(
            R.id.navTheme to findViewById<View>(R.id.sectionTheme),
            R.id.navFeatures to findViewById<View>(R.id.sectionFeatures),
            R.id.navClean to findViewById<View>(R.id.sectionClean),
            R.id.navAbout to findViewById<View>(R.id.sectionAbout),
        )
        nav.setOnItemSelectedListener { item ->
            sections.forEach { (id, view) -> view.visibility = if (id == item.itemId) View.VISIBLE else View.GONE }
            true
        }
    }

    private fun applyAccent() {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val tint = ColorStateList(states, intArrayOf(accent, 0xFF9A9AA0.toInt()))
        nav.itemIconTintList = tint
        nav.itemTextColor = tint
        nav.itemActiveIndicatorColor = ColorStateList.valueOf(withAlpha(accent, 50))
        val accentList = ColorStateList.valueOf(accent)
        listOf(R.id.btnBuild, R.id.btnRemove).forEach {
            findViewById<MaterialButton>(it).backgroundTintList = accentList
        }
        preview.update(shape, style, accent)
    }

    // ---------- Тема ----------

    private fun setupTheme() {
        preview = findViewById(R.id.preview)
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { Apps.launcherApps(this@MainActivity) }
            preview.setApps(apps)
            preview.update(shape, style, accent)
        }

        val swatches = findViewById<LinearLayout>(R.id.swatches)
        val size = dp(44)
        ACCENTS.forEach { color ->
            val v = View(this)
            v.layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(12) }
            v.background = swatch(color, color == accent)
            v.setOnClickListener {
                accent = color
                prefs.edit().putInt("accent", color).apply()
                for (i in 0 until swatches.childCount) {
                    swatches.getChildAt(i).background = swatch(ACCENTS[i], ACCENTS[i] == accent)
                }
                applyAccent()
            }
            swatches.addView(v)
        }

        fillChips(findViewById(R.id.shapeGroup), IconShape.entries.map { it.title }, shape.ordinal) {
            shape = IconShape.entries[it]
            prefs.edit().putInt("shape2", it).apply()
            preview.update(shape, style, accent)
        }
        fillChips(findViewById(R.id.styleGroup), IconStyle.entries.map { it.title }, style.ordinal) {
            style = IconStyle.entries[it]
            prefs.edit().putInt("style2", it).apply()
            preview.update(shape, style, accent)
        }

        val status = findViewById<TextView>(R.id.themeStatus)
        val build = findViewById<MaterialButton>(R.id.btnBuild)
        build.setOnClickListener {
            build.isEnabled = false
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.Default) {
                        MtzBuilder.build(this@MainActivity, accent, shape, style) { msg ->
                            runOnUiThread { status.text = msg }
                        }
                    }
                    status.text = "✅ Готово: Загрузки/givebackmyOneplus/${result.fileName}\n" +
                        "Иконок: ${result.iconCount}. Откройте файл через приложение «Темы»."
                    openTheme(result.uri)
                } catch (e: Exception) {
                    status.text = "Ошибка сборки: ${e.message}"
                } finally {
                    build.isEnabled = true
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnThemeDiag).setOnClickListener { themeDiagnostics() }
        findViewById<MaterialButton>(R.id.btnOpenThemes).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.android.thememanager")
            if (intent != null) startActivity(intent) else toast("Приложение «Темы» не найдено")
        }
    }

    /** Только чтение: версия «Тем», их экраны и папки — чтобы найти способ применить тему. */
    private fun themeDiagnostics() {
        if (!Shell.granted()) {
            toast("Нужен доступ Shizuku (вкладка «Очистка»)")
            return
        }
        val tm = "com.android.thememanager"
        val cmd = listOf(
            "echo '== version'",
            "dumpsys package $tm | grep -E 'versionName|versionCode' | head -4",
            "echo '== activities'",
            "dumpsys package $tm | grep -oE '$tm/[A-Za-z0-9_.\$]+' | sort -u | head -200",
            "echo '== data dir'",
            "ls -la /sdcard/Android/data/$tm/files 2>&1 | head -40",
            "echo '== MIUI dirs'",
            "find /sdcard/Android/data/$tm/files -maxdepth 4 -type d 2>&1 | head -80",
            "echo '== /sdcard/MIUI/theme'",
            "ls -la /sdcard/MIUI/theme 2>&1 | head -20",
            "echo '== mtz files'",
            "find /sdcard/Android/data/$tm /sdcard/MIUI -name '*.mtz' 2>/dev/null | head -20",
        ).joinToString("; ")
        toast("Собираю данные…")
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching { Shell.run(cmd).second }.getOrElse { "Ошибка: ${it.message}" }
            }
            val text = TextView(this@MainActivity).apply {
                this.text = out
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
                textSize = 11f
                setPadding(dp(16), dp(8), dp(16), dp(8))
            }
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Диагностика «Тем»")
                .setView(android.widget.ScrollView(this@MainActivity).apply { addView(text) })
                .setPositiveButton("Копировать") { _, _ ->
                    val cm = getSystemService(android.content.ClipboardManager::class.java)
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("diag", out))
                    toast("Скопировано — вставьте в чат")
                }
                .setNegativeButton("Закрыть", null)
                .show()
        }
    }

    private fun openTheme(uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/octet-stream")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val themes = Intent(view).setPackage("com.android.thememanager")
        try {
            startActivity(themes)
        } catch (e: Exception) {
            try {
                startActivity(Intent.createChooser(view, "Открыть тему"))
            } catch (_: Exception) {
            }
        }
    }

    private fun fillChips(group: ChipGroup, titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        titles.forEachIndexed { i, title ->
            val chip = Chip(this).apply {
                text = title
                isCheckable = true
                id = View.generateViewId()
            }
            group.addView(chip)
            if (i == selected) group.check(chip.id)
        }
        group.setOnCheckedStateChangeListener { g, ids ->
            val idx = (0 until g.childCount).indexOfFirst { g.getChildAt(it).id == ids.firstOrNull() }
            if (idx >= 0) onSelect(idx)
        }
    }

    private fun swatch(color: Int, selected: Boolean) = GradientDrawable().apply {
        setShape(GradientDrawable.OVAL)
        setColor(color)
        if (selected) setStroke(dp(3), Color.WHITE)
    }

    // ---------- Очистка ----------

    private fun setupClean() {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        findViewById<MaterialButton>(R.id.btnShizuku).setOnClickListener {
            when {
                !Shell.shizukuRunning() -> {
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                    startActivity(market)
                }
                !Shell.granted() -> Shizuku.requestPermission(SHIZUKU_REQUEST)
            }
        }
        findViewById<MaterialButton>(R.id.btnRemove).setOnClickListener {
            runForSelected("Удаление") { "pm uninstall -k --user 0 $it" }
        }
        findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener {
            runForSelected("Возврат") { "cmd package install-existing --user 0 $it" }
        }

        val group = findViewById<MaterialButtonToggleGroup>(R.id.animGroup)
        val current = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        group.check(when (current) { 0.5f -> R.id.anim05; 0.75f -> R.id.anim075; else -> R.id.anim1 })
        group.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val scale = when (id) { R.id.anim05 -> "0.5"; R.id.anim075 -> "0.75"; else -> "1" }
            shell(
                "settings put global window_animation_scale $scale; " +
                    "settings put global transition_animation_scale $scale; " +
                    "settings put global animator_duration_scale $scale"
            ) { log("Анимации: ${scale}x") }
        }
    }

    private fun refreshShizuku() {
        val status = findViewById<TextView>(R.id.shizukuStatus)
        val btn = findViewById<MaterialButton>(R.id.btnShizuku)
        when {
            !Shell.shizukuRunning() -> {
                status.text = "Shizuku не запущен. Установите Shizuku и запустите через «Беспроводную отладку»."
                btn.text = "Скачать Shizuku"
                btn.visibility = View.VISIBLE
            }
            !Shell.granted() -> {
                status.text = "Shizuku запущен, но доступ не выдан."
                btn.text = "Дать доступ"
                btn.visibility = View.VISIBLE
            }
            else -> {
                status.text = "✅ Доступ есть"
                btn.visibility = View.GONE
            }
        }
    }

    private fun refreshBloat() {
        val list = findViewById<LinearLayout>(R.id.bloatList)
        val keep = checks.filterValues { it.isChecked }.keys
        list.removeAllViews()
        checks.clear()
        Bloat.list.forEach { app ->
            val info = try {
                packageManager.getApplicationInfo(app.pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } ?: return@forEach
            val installed = (info.flags and android.content.pm.ApplicationInfo.FLAG_INSTALLED) != 0
            val cb = CheckBox(this).apply {
                text = "${app.title}\n${app.note} · ${if (installed) "установлено" else "удалено"}"
                setTextColor(if (installed) Color.WHITE else 0xFF9A9AA0.toInt())
                isChecked = app.pkg in keep
                buttonTintList = ColorStateList.valueOf(accent)
                setPadding(0, dp(6), 0, dp(6))
            }
            checks[app.pkg] = cb
            list.addView(cb)
        }
        if (checks.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Лишних приложений из списка не найдено 🎉"
                setTextColor(0xFF9A9AA0.toInt())
            })
        }
    }

    private fun runForSelected(action: String, command: (String) -> String) {
        val selected = checks.filterValues { it.isChecked }.keys.toList()
        if (selected.isEmpty()) {
            toast("Отметьте приложения")
            return
        }
        lifecycleScope.launch {
            selected.forEach { pkg ->
                val res = withContext(Dispatchers.IO) { runCatching { Shell.run(command(pkg)) } }
                res.onSuccess { (code, out) -> log("$action $pkg: ${if (code == 0) "OK" else "ошибка $code"} $out") }
                    .onFailure { log("$action $pkg: ${it.message}") }
            }
            checks.values.forEach { it.isChecked = false }
            refreshBloat()
        }
    }

    private fun shell(cmd: String, onOk: () -> Unit) {
        if (!Shell.granted()) {
            toast("Нужен доступ Shizuku")
            refreshShizuku()
            return
        }
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) { runCatching { Shell.run(cmd) } }
            res.onSuccess { (code, out) -> if (code == 0) onOk() else log("Ошибка $code: $out") }
                .onFailure { log("Ошибка: ${it.message}") }
        }
    }

    private fun log(msg: String) {
        val tv = findViewById<TextView>(R.id.cleanLog)
        tv.text = "$msg\n${tv.text}".take(4000)
    }

    // ---------- О приложении ----------

    private fun setupAbout() {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.aboutVersion).text = "Версия $version"
        findViewById<MaterialButton>(R.id.btnGithub).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB)))
        }
        findViewById<MaterialButton>(R.id.btnWatermelon).setOnClickListener { showWatermelon() }
    }

    private fun showWatermelon() {
        val phone = PhoneView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(300))
        }
        val text = TextView(this).apply {
            text = "OnePlus глобалка"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(8))
            addView(phone)
            addView(text)
        }
        MaterialAlertDialogBuilder(this)
            .setView(box)
            .setPositiveButton("🍉") { _, _ -> }
            .show()

        phone.scaleX = 0.3f
        phone.scaleY = 0.3f
        phone.rotation = -20f
        phone.animate().scaleX(1f).scaleY(1f).rotation(0f)
            .setDuration(700).setInterpolator(OvershootInterpolator()).start()
        text.alpha = 0f
        text.animate().alpha(1f).setStartDelay(400).setDuration(400).start()
    }

    // ---------- Утилиты ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun withAlpha(color: Int, alpha: Int) =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
