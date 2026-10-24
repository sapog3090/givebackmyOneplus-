package com.givebackmyoneplus

import android.app.NotificationManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Zen Mode: экран фокуса на заданное время. Включает «Не беспокоить»,
 * держит экран и перехватывает «Назад». Выход — долгое нажатие (экстренно)
 * или по окончании таймера.
 */
class ZenActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private var dndChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val minutes = intent.getIntExtra("minutes", 20)
        val accent = Features.accent(this)

        val time = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 64f
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "Режим фокуса\nУдерживайте, чтобы выйти"
            setTextColor(0xAAFFFFFF.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            addView(time)
            addView(TextView(this@ZenActivity).apply { text = "\n" })
            addView(hint)
            setOnLongClickListener { finish(); true }
        }
        setContentView(root)

        enableDnd()
        timer = object : CountDownTimer(minutes * 60_000L, 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                time.text = "%d:%02d".format(s / 60, s % 60)
            }
            override fun onFinish() {
                time.setTextColor(accent)
                time.text = "Готово"
                root.postDelayed({ finish() }, 1500)
            }
        }.start()
    }

    private fun enableDnd() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted &&
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY
        ) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            dndChanged = true
        }
    }

    @Deprecated("intentional: trap Back during focus")
    override fun onBackPressed() {
        // Во время фокуса «Назад» не выходит — только долгое нажатие.
    }

    override fun onDestroy() {
        timer?.cancel()
        if (dndChanged) {
            getSystemService(NotificationManager::class.java)
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        super.onDestroy()
    }
}
