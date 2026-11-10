package id.kayzen.autobuy

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KayzenAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var activeService: KayzenAccessibilityService? = null
        fun testAlert(): Boolean {
            val service = activeService ?: return false
            service.showPopup("UJI NOTIFIKASI", "Notifikasi dan floating pop-up Kayzen berfungsi.")
            service.notifyUser("UJI NOTIFIKASI", "Notifikasi Kayzen berhasil dikirim.")
            service.setStatus("Uji notifikasi berhasil dikirim.", "Tes notifikasi manual pada " + service.timestamp() + ".")
            return true
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("kayzen_settings", MODE_PRIVATE) }
    private var lastScanAt = 0L
    private var lastEventScanAt = 0L
    private var candidateAmount: Int? = null
    private var consecutive = 0
    private var clickedThisSession = false
    private var popupView: View? = null
    private var popupTimeout: Runnable? = null
    private var windowManager: WindowManager? = null

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring() && !clickedThisSession) scanScreen()
            handler.postDelayed(this, 700)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        setStatus("Accessibility aktif. Pemantauan siap.", "Layanan aksesibilitas terhubung.")
        handler.removeCallbacks(scanRunnable)
        handler.post(scanRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isMonitoring() || clickedThisSession) return
        if (event?.packageName?.toString() != "id.dana") return
        val now = System.currentTimeMillis()
        if (now - lastEventScanAt < 250) return
        lastEventScanAt = now
        scanScreen()
    }

    override fun onInterrupt() {
        handler.removeCallbacks(scanRunnable)
        setStatus("Layanan terinterupsi.", "Android menghentikan sementara layanan aksesibilitas.")
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        hidePopup()
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun isMonitoring() = prefs.getBoolean("monitor_enabled", false)

    private fun scanScreen() {
        if (!isMonitoring()) return
        val now = System.currentTimeMillis()
        if (now - lastScanAt < 250) return
        lastScanAt = now
        val root = rootInActiveWindow
        if (root == null) {
            setStatus("Menunggu tampilan DANA…", "Layar aktif belum menyediakan accessibility root.")
            return
        }
        if (root.packageName?.toString() != "id.dana") {
            setStatus("Menunggu halaman DANA Deals…", "Aplikasi aktif bukan DANA (" + (root.packageName?.toString() ?: "paket tidak diketahui") + ").")
            candidateAmount = null
            consecutive = 0
            return
        }

        var match: Pair<Int, AccessibilityNodeInfo>? = null
        for (amount in listOf(7290, 355, 100)) {
            val card = findTargetContainer(root, amount) ?: continue
            val text = normalize(collectText(card))
            if (text.contains("stok habis") || text.contains("habis")) continue
            if (!hasExplicitAction(text)) continue
            match = amount to card
            break
        }

        if (match == null) {
            candidateAmount = null
            consecutive = 0
            setStatus("DANA terbaca — belum menemukan target voucher.", "Halaman DANA terbaca pada " + timestamp() + ", tetapi teks kartu belum cocok dengan filter.")
            return
        }

        val (amount, card) = match
        if (candidateAmount == amount) consecutive++ else {
            candidateAmount = amount
            consecutive = 1
        }
        setStatus("Target $amount Diamond terdeteksi ($consecutive/3).", "Target $amount Diamond terbaca pada " + timestamp() + "; memverifikasi kecocokan.")
        if (consecutive < 3) return

        val clickable = findClickableAncestor(card, 4)
        val clickableText = clickable?.let { normalize(collectText(it)) } ?: ""
        val message = "$amount Diamond tersedia. Periksa kartu voucher; pembayaran tetap manual."
        if (clickable != null && clickable.isEnabled &&
            containsAmount(clickableText, amount) && hasExplicitAction(clickableText)) {
            val success = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                clickedThisSession = true
                prefs.edit().putBoolean("monitor_enabled", false).apply()
                setStatus("Kartu $amount Diamond berhasil dibuka. Periksa detail secara manual.", "Klik kartu $amount Diamond berhasil pada " + timestamp() + ". Pemantauan dihentikan.")
                showPopup("VOUCHER TERDETEKSI", "$amount Diamond — kartu dibuka. Periksa detail secara manual.")
                notifyUser("VOUCHER TERDETEKSI", "$amount Diamond — kartu dibuka. Periksa detail secara manual.")
            } else {
                setStatus("Target $amount Diamond ditemukan. Ketuk kartu secara manual.", "Target cocok, tetapi aksi klik tidak berhasil.")
                showPopup("TARGET DITEMUKAN", message)
                notifyUser("TARGET DITEMUKAN", message)
            }
        } else {
            setStatus("Target $amount Diamond ditemukan. Ketuk kartu secara manual.", "Target cocok tetapi area klik tidak aman; tidak ada klik dilakukan.")
            showPopup("TARGET DITEMUKAN", message)
            notifyUser("TARGET DITEMUKAN", message)
        }
        candidateAmount = null
        consecutive = 0
    }

    private fun showPopup(title: String, message: String) {
        handler.post {
            hidePopup()
            val density = resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(15), dp(18), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(20, 31, 52))
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.rgb(54, 214, 220))
                }
                elevation = dp(12).toFloat()
                isClickable = true
            }
            val heading = TextView(this).apply {
                text = "◆  $title"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(54, 214, 220))
            }
            val body = TextView(this).apply {
                text = message
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(0, dp(8), 0, dp(10))
            }
            val close = TextView(this).apply {
                text = "TUTUP"
                textSize = 12f
                gravity = Gravity.END
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(92, 220, 155))
                setPadding(dp(8), dp(4), 0, 0)
                setOnClickListener { hidePopup() }
            }
            container.addView(heading)
            container.addView(body)
            container.addView(close)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(42)
                width = resources.displayMetrics.widthPixels - dp(28)
            }
            try {
                val wm = windowManager ?: getSystemService(WINDOW_SERVICE) as WindowManager
                wm.addView(container, params)
                popupView = container
                val timeout = Runnable { hidePopup() }
                popupTimeout = timeout
                handler.postDelayed(timeout, 9000)
            } catch (e: Exception) {
                setStatus("Floating pop-up gagal ditampilkan.", "Overlay error: " + (e.message ?: "tidak diketahui"))
                popupView = null
            }
        }
    }

    private fun hidePopup() {
        popupTimeout?.let { handler.removeCallbacks(it) }
        popupTimeout = null
        val current = popupView
        popupView = null
        if (current != null) {
            try { windowManager?.removeView(current) } catch (_: Exception) { }
        }
    }

    private fun findTargetContainer(root: AccessibilityNodeInfo, amount: Int): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 18) return
            val own = normalize(listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" "))
            if (containsAmount(own, amount) || own.contains("diamond")) candidates.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        for (textNode in candidates) {
            var current: AccessibilityNodeInfo? = textNode
            repeat(9) {
                val candidate = current ?: return@repeat
                val text = normalize(collectText(candidate))
                if (text.length in 1..700 && containsAmount(text, amount) &&
                    text.contains("diamond") && (hasExplicitAction(text) || text.contains("habis"))) {
                    return candidate
                }
                current = candidate.parent
            }
        }
        return null
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).replace(".", "").replace(",", "").replace("\\s+".toRegex(), " ")

    private fun containsAmount(text: String, amount: Int): Boolean {
        val compact = text.replace(" ", "")
        return Regex("(?<![0-9])$amount(?![0-9])").containsMatchIn(compact)
    }

    private fun hasExplicitAction(text: String): Boolean =
        listOf("beli", "klaim", "ambil", "redeem").any {
            Regex("(?<![a-z])$it(?![a-z])").containsMatchIn(text)
        }

    private fun collectText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        fun walk(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > 10) return
            n.text?.toString()?.let { if (it.isNotBlank()) parts.add(it) }
            n.contentDescription?.toString()?.let { if (it.isNotBlank()) parts.add(it) }
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(node, 0)
        return parts.joinToString(" ")
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo, maxDepth: Int): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(maxDepth + 1) {
            if (current?.isClickable == true) return current
            current = current?.parent
        }
        return null
    }

    private fun timestamp() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun setStatus(status: String, log: String) {
        prefs.edit().putString("last_status", status).putString("last_log", log).apply()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("kayzen_monitor", "Peringatan Voucher Kayzen", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi cepat dan pop-up saat target voucher DANA terdeteksi"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notifyUser(title: String, message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "kayzen_monitor")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(message).setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH).setCategory(Notification.CATEGORY_STATUS)
                .setVibrate(longArrayOf(0, 250, 120, 250)).build())
    }
}
