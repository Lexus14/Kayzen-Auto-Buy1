package id.kayzen.autobuy

import android.app.Activity
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
    private lateinit var logView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { refreshDashboard(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 28) }
        val title = TextView(this).apply { text = "KAYZEN AUTO BUY v2.3"; textSize = 25f; gravity = Gravity.CENTER }
        statusView = TextView(this).apply { textSize = 17f; setPadding(0, 28, 0, 16) }
        val start = Button(this).apply {
            text = "MULAI PEMANTAUAN"
            setOnClickListener {
                prefs.edit().putBoolean("monitor_enabled", true).putString("last_status", "Pemantauan dimulai. Buka DANA Deals.")
                    .putString("last_log", "Pemantauan diaktifkan oleh pengguna.").apply()
                refreshDashboard()
            }
        }
        val stop = Button(this).apply {
            text = "HENTIKAN PEMANTAUAN"
            setOnClickListener {
                prefs.edit().putBoolean("monitor_enabled", false).putString("last_status", "Pemantauan dihentikan.")
                    .putString("last_log", "Pemantauan dihentikan oleh pengguna.").apply()
                refreshDashboard()
            }
        }
        val openDana = Button(this).apply {
            text = "BUKA DANA"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("id.dana")
                if (launch != null) startActivity(launch)
                else { prefs.edit().putString("last_status", "DANA tidak ditemukan. Buka DANA secara manual.").apply(); refreshDashboard() }
            }
        }
        val settings = Button(this).apply {
            text = "PENGATURAN AKSESIBILITAS"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val logTitle = TextView(this).apply { text = "AKTIVITAS TERAKHIR"; textSize = 16f; setPadding(0, 26, 0, 8) }
        logView = TextView(this).apply { textSize = 14f; setPadding(14, 14, 14, 14); setBackgroundColor(0xFFEAEFF5.toInt()) }
        val safety = TextView(this).apply {
            text = "Mode aman: hanya membuka kartu voucher. Checkout, pembayaran, PIN, OTP, dan CAPTCHA selalu manual."
            textSize = 13f; setPadding(0, 22, 0, 0)
        }
        listOf(title, statusView, start, stop, openDana, settings, logTitle, logView, safety).forEach { layout.addView(it) }
        setContentView(layout)
        refreshDashboard()
    }

    override fun onResume() { super.onResume(); handler.removeCallbacks(refresh); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }

    private fun refreshDashboard() {
        val enabled = prefs.getBoolean("monitor_enabled", false)
        val status = prefs.getString("last_status", "Siap. Aktifkan Accessibility, lalu tekan Mulai Pemantauan.")
        val last = prefs.getString("last_log", "Belum ada aktivitas.")
        statusView.text = "STATUS: " + if (enabled) "PEMANTAUAN AKTIF\n$status" else "BERHENTI\n$status"
        logView.text = last
    }
}