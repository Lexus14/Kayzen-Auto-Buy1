package id.kayzen.autobuy

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
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
    private val refresh = object : Runnable {
        override fun run() { refreshDashboard(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
        }
        val title = TextView(this).apply {
            text = "KAYZEN AUTO BUY v2.4"
            textSize = 25f
            gravity = Gravity.CENTER
        }
        accessView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        statusView = TextView(this).apply {
            textSize = 17f
            setPadding(0, 8, 0, 16)
        }
        val accessSettings = Button(this).apply {
            text = "PENGATURAN AKSESIBILITAS"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        monitorButton = Button(this).apply {
            setOnClickListener {
                val currentlyEnabled = prefs.getBoolean("monitor_enabled", false)
                val next = !currentlyEnabled
                prefs.edit()
                    .putBoolean("monitor_enabled", next)
                    .putString("last_status", if (next) "Pemantauan dilanjutkan. Buka DANA Deals." else "Pemantauan dijeda.")
                    .putString("last_log", if (next) "Pemantauan dilanjutkan oleh pengguna." else "Pemantauan dijeda oleh pengguna.")
                    .apply()
                refreshDashboard()
            }
        }
        val openDana = Button(this).apply {
            text = "BUKA DANA"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("id.dana")
                if (launch != null) {
                    startActivity(launch)
                    prefs.edit().putString("last_log", "DANA dibuka dari Kayzen Auto Buy.").apply()
                } else {
                    prefs.edit().putString("last_status", "DANA tidak ditemukan. Instal atau buka DANA secara manual.")
                        .putString("last_log", "Aplikasi DANA tidak ditemukan di perangkat.").apply()
                }
                refreshDashboard()
            }
        }
        val logTitle = TextView(this).apply {
            text = "AKTIVITAS TERAKHIR"
            textSize = 16f
            setPadding(0, 26, 0, 8)
        }
        logView = TextView(this).apply {
            textSize = 14f
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFEAEFF5.toInt())
        }
        val safety = TextView(this).apply {
            text = "Mode aman: hanya mendeteksi dan membuka kartu voucher. Checkout, pembayaran, PIN, OTP, dan CAPTCHA tetap manual."
            textSize = 13f
            setPadding(0, 22, 0, 0)
        }
        listOf(title, accessView, accessSettings, statusView, monitorButton, openDana, logTitle, logView, safety)
            .forEach { layout.addView(it) }
        setContentView(layout)
        refreshDashboard()
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

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = ComponentName(this, KayzenAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun refreshDashboard() {
        val enabled = prefs.getBoolean("monitor_enabled", false)
        val serviceEnabled = isAccessibilityEnabled()
        accessView.text = "AKSESIBILITAS: " + if (serviceEnabled) "AKTIF" else "BELUM AKTIF — aktifkan melalui Pengaturan"
        statusView.text = "STATUS PEMANTAUAN: " + if (enabled) "BERJALAN" else "DIJEDA / BERHENTI" +
            "\n" + (prefs.getString("last_status", "Siap.") ?: "Siap.")
        monitorButton.text = if (enabled) "JEDA PEMANTAUAN" else "LANJUTKAN PEMANTAUAN"
        monitorButton.isEnabled = serviceEnabled
        if (!serviceEnabled && enabled) {
            prefs.edit().putBoolean("monitor_enabled", false)
                .putString("last_status", "Pemantauan dihentikan karena Accessibility tidak aktif.").apply()
        }
        logView.text = prefs.getString("last_log", "Belum ada aktivitas.") ?: "Belum ada aktivitas."
    }
}
