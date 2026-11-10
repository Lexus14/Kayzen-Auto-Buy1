package id.kayzen.autobuy

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KayzenAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("kayzen_settings", MODE_PRIVATE) }
    private var lastScanAt = 0L
    private var candidateAmount: Int? = null
    private var consecutive = 0
    private var clickedThisSession = false

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring() && !clickedThisSession) scanScreen()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createChannel()
        setStatus("Accessibility aktif. Tekan Mulai Pemantauan di aplikasi.", "Layanan aksesibilitas terhubung.")
        handler.post(scanRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isMonitoring() || clickedThisSession) return
        if (event?.packageName?.toString() != "id.dana") return
        val now = System.currentTimeMillis()
        if (now - lastScanAt < 1000) return
        lastScanAt = now
        scanScreen()
    }

    override fun onInterrupt() {
        handler.removeCallbacks(scanRunnable)
        setStatus("Layanan terinterupsi.", "Android menghentikan sementara layanan aksesibilitas.")
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }

    private fun isMonitoring() = prefs.getBoolean("monitor_enabled", false)

    private fun scanScreen() {
        if (!isMonitoring()) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != "id.dana") return

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
            setStatus("Memindai DANA Deals… belum ada target yang memenuhi syarat.", "Pemindaian " + timestamp() + ": belum ada kartu voucher yang cocok.")
            return
        }

        val (amount, card) = match
        if (candidateAmount == amount) consecutive++ else {
            candidateAmount = amount
            consecutive = 1
        }
        setStatus("Target $amount Diamond terdeteksi ($consecutive/3).", "Target $amount Diamond terbaca pada " + timestamp() + "; menunggu tiga pemindaian.")
        if (consecutive < 3) return

        val clickable = findClickableAncestor(card, 3)
        val clickableText = clickable?.let { normalize(collectText(it)) } ?: ""
        if (clickable != null && clickable.isEnabled &&
            containsAmount(clickableText, amount) && hasExplicitAction(clickableText)) {
            val success = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                clickedThisSession = true
                prefs.edit().putBoolean("monitor_enabled", false).apply()
                setStatus("Kartu $amount Diamond berhasil dibuka. Periksa detail secara manual.", "Klik kartu $amount Diamond berhasil pada " + timestamp() + ". Pemantauan dihentikan.")
                notifyUser("Kartu voucher dibuka", "$amount Diamond terdeteksi. Periksa detail; pembayaran tetap manual.")
            } else {
                setStatus("Klik kartu tidak berhasil. Silakan buka kartu secara manual.", "Aksi klik ditolak oleh tampilan DANA.")
                notifyUser("Klik tidak berhasil", "Target $amount Diamond ditemukan. Silakan ketuk manual.")
            }
        } else {
            setStatus("Target $amount Diamond ditemukan, tetapi area klik tidak aman. Ketuk manual.", "Target ditemukan tetapi node klik tidak cocok; tidak ada klik dilakukan.")
            notifyUser("Target terdeteksi", "$amount Diamond ditemukan. Ketuk kartu secara manual.")
        }
        candidateAmount = null
        consecutive = 0
    }

    private fun findTargetContainer(root: AccessibilityNodeInfo, amount: Int): AccessibilityNodeInfo? {
        val textNodes = mutableListOf<AccessibilityNodeInfo>()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 18) return
            val own = normalize(listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" "))
            if (containsAmount(own, amount) && own.contains("diamond")) textNodes.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        for (textNode in textNodes) {
            var current: AccessibilityNodeInfo? = textNode
            repeat(7) {
                val candidate = current ?: return@repeat
                val text = normalize(collectText(candidate))
                if (text.length <= 350 && containsAmount(text, amount) && text.contains("diamond") &&
                    (hasExplicitAction(text) || text.contains("habis"))) return candidate
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
            if (depth > 8) return
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
            val channel = NotificationChannel("kayzen_monitor", "Kayzen Monitor", NotificationManager.IMPORTANCE_HIGH)
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
        manager.notify(7290, builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(message).setAutoCancel(true).build())
    }
}