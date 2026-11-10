package id.kayzen.autobuy

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("kayzen_settings", MODE_PRIVATE) }
    private lateinit var statusView: TextView
    private lateinit var accessView: TextView
    private lateinit var logView: TextView
    private lateinit var monitorButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private val bg = Color.rgb(12, 18, 34)
    private val panel = Color.rgb(25, 35, 57)
    private val muted = Color.rgb(165, 179, 202)
    private val cyan = Color.rgb(54, 214, 220)
    private val green = Color.rgb(92, 220, 155)

    private val refresh = object : Runnable {
        override fun run() { refreshDashboard(); handler.postDelayed(this, 800) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)
        requestNotificationPermission()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
        }
        root.addView(TextView(this).apply {
            text = "K A Y Z E N"
            textSize = 13f
            letterSpacing = .24f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "AUTO BUY"
            textSize = 31f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "SMART VOUCHER MONITOR  •  VERSION 2.6.1"
            textSize = 10f
            letterSpacing = .08f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(24))
        })

        val statusCard = card()
        statusCard.addView(label("MONITORING STATUS"))
        statusView = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            setPadding(0, dp(8), 0, dp(5))
        }
        accessView = TextView(this).apply {
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(5), 0, 0)
        }
        statusCard.addView(statusView)
        statusCard.addView(accessView)
        root.addView(statusCard, marginParams(bottom = 14))

        val controls = card()
        controls.addView(label("QUICK CONTROL"))
        monitorButton = actionButton("MULAI PEMANTAUAN", cyan, bg)
        monitorButton.setOnClickListener {
            val next = !prefs.getBoolean("monitor_enabled", false)
            prefs.edit().putBoolean("monitor_enabled", next)
                .putString("last_status", if (next) "Pemantauan aktif. Buka halaman DANA Deals." else "Pemantauan dijeda.")
                .putString("last_log", if (next) "Pemantauan dimulai oleh pengguna." else "Pemantauan dijeda oleh pengguna.")
                .apply()
            refreshDashboard()
        }
        val danaButton = actionButton("BUKA APLIKASI DANA", Color.rgb(42, 57, 83), Color.WHITE)
        danaButton.setOnClickListener {
            val launch = packageManager.getLaunchIntentForPackage("id.dana")
            if (launch != null) {
                startActivity(launch)
                prefs.edit().putString("last_log", "DANA dibuka dari Kayzen Auto Buy.").apply()
            } else {
                prefs.edit().putString("last_status", "DANA tidak terdeteksi. Pastikan aplikasi DANA terpasang.")
                    .putString("last_log", "Aplikasi DANA tidak ditemukan pada perangkat.").apply()
            }
            refreshDashboard()
        }
        controls.addView(monitorButton, marginParams(top = 12, bottom = 9))
        controls.addView(danaButton)
        val testButton = actionButton("UJI NOTIFIKASI + POP-UP", Color.rgb(35, 104, 116), Color.WHITE)
        testButton.setOnClickListener {
            val ok = KayzenAccessibilityService.testAlert()
            if (!ok) {
                prefs.edit().putString("last_status", "Layanan Accessibility belum terhubung.").putString("last_log", "Tes gagal: aktifkan ulang Accessibility Kayzen.").apply()
            }
            refreshDashboard()
        }
        controls.addView(testButton, marginParams(top = 9))
        val settings = actionButton("PENGATURAN AKSESIBILITAS", Color.rgb(42, 57, 83), Color.WHITE)
        settings.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        controls.addView(settings, marginParams(top = 9))
        root.addView(controls, marginParams(bottom = 14))

        val activityCard = card()
        activityCard.addView(label("AKTIVITAS TERAKHIR"))
        logView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(2))
        }
        activityCard.addView(logView)
        root.addView(activityCard, marginParams(bottom = 14))

        root.addView(TextView(this).apply {
            text = "SAFE MODE  •  Kayzen hanya mendeteksi dan membuka kartu voucher yang cocok. Checkout, pembayaran, PIN, OTP, dan CAPTCHA tetap manual."
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
        })
        setContentView(root)
        refreshDashboard()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 250)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun refreshDashboard() {
        val enabled = prefs.getBoolean("monitor_enabled", false)
        val serviceEnabled = isAccessibilityEnabled()
        statusView.text = if (enabled) "●  PEMANTAUAN AKTIF" else "Ⅱ  PEMANTAUAN DIJEDA"
        statusView.setTextColor(if (enabled) green else muted)
        accessView.text = "Aksesibilitas: " + if (serviceEnabled) "AKTIF ✓" else "BELUM AKTIF — aktifkan di pengaturan"
        monitorButton.text = if (enabled) "JEDA PEMANTAUAN" else "MULAI PEMANTAUAN"
        monitorButton.isEnabled = serviceEnabled
        monitorButton.alpha = if (serviceEnabled) 1f else .45f
        if (!serviceEnabled && enabled) {
            prefs.edit().putBoolean("monitor_enabled", false)
                .putString("last_status", "Pemantauan dihentikan karena Accessibility tidak aktif.").apply()
        }
        logView.text = prefs.getString("last_log", "Belum ada aktivitas.") ?: "Belum ada aktivitas."
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = ComponentName(this, KayzenAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(16), dp(17), dp(16))
        background = rounded(panel, 18)
    }
    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 11f; letterSpacing = .12f
        typeface = Typeface.DEFAULT_BOLD; setTextColor(muted)
    }
    private fun actionButton(value: String, color: Int, textColor: Int) = Button(this).apply {
        text = value; textSize = 13f; isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD; setTextColor(textColor)
        background = rounded(color, 13); minHeight = dp(52); stateListAnimator = null
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun marginParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
