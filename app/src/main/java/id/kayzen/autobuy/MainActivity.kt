package id.kayzen.autobuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
        }
        val title = TextView(this).apply {
            text = "KAYZEN AUTO BUY v2.2"
            textSize = 24f
        }
        val info = TextView(this).apply {
            text = """
                Mode: Accessibility Click Assist
                Prioritas: 7290, 355, 100 Diamond
                Bot hanya mengetuk kartu jika target terbaca, stok tersedia,
                ada aksi Beli/Klaim/Ambil, dan kondisi sama terdeteksi 3 kali.
                Bot tidak menekan checkout, pembayaran, PIN, OTP, CAPTCHA,
                atau mengelola saldo. Aktifkan layanan di Pengaturan Aksesibilitas.
            """.trimIndent()
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        val settings = Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val openDana = Button(this).apply {
            text = "Buka DANA"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("id.dana")
                if (launch != null) startActivity(launch)
                else info.text = "Aplikasi DANA tidak ditemukan. Buka DANA secara manual."
            }
        }
        val caution = TextView(this).apply {
            text = "Fitur klik otomatis bergantung pada struktur tampilan DANA. Uji dahulu."
            textSize = 13f
            setPadding(0, 24, 0, 0)
        }
        layout.addView(title)
        layout.addView(info)
        layout.addView(settings)
        layout.addView(openDana)
        layout.addView(caution)
        setContentView(layout)
    }
}