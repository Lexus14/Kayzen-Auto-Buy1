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
import java.util.Locale

class KayzenAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastScanAt = 0L
    private var candidateAmount: Int? = null
    private var consecutive = 0
    private var clicked = false

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (!clicked) scanScreen()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createChannel()
        notifyUser("Aktif", "Buka DANA Deals dan tampilkan kartu voucher Free Fire.")
        handler.post(scanRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (clicked) return
        if (event?.packageName?.toString() != "id.dana") return
        val now = System.currentTimeMillis()
        if (now - lastScanAt < 1000) return
        lastScanAt = now
        scanScreen()
    }

    override fun onInterrupt() { handler.removeCallbacks(scanRunnable) }

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }

    private fun scanScreen() {
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != "id.dana") return
        var selected: Pair<Int, AccessibilityNodeInfo>? = null
        for (amount in listOf(7290, 355, 100)) {
            val card = findTargetContainer(root, amount)
            if (card == null) {
                candidateAmount = null
                consecutive = 0
                return
            }
            val text = collectText(card).lowercase(Locale.ROOT)
            if (text.contains("stok habis") || text.contains("habis")) continue
            val actionable = listOf("beli", "klaim", "ambil", "redeem").any { text.contains(it) }
            if (actionable) selected = amount to card
            break
        }
        val match = selected ?: run {
            candidateAmount = null
            consecutive = 0
            return
        }
        val (amount, card) = match
        if (candidateAmount == amount) consecutive++ else {
            candidateAmount = amount
            consecutive = 1
        }
        if (consecutive < 3) return
        val clickable = findClickableAncestor(card, 5)
        if (clickable != null && clickable.isEnabled) {
            if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                clicked = true
                notifyUser("Kartu voucher dibuka", "$amount Diamond terdeteksi. Pembayaran tetap manual.")
            } else notifyUser("Klik tidak berhasil", "Silakan ketuk kartu voucher secara manual.")
        } else notifyUser("Target terdeteksi", "$amount Diamond mungkin tersedia. Ketuk manual.")
        candidateAmount = null
        consecutive = 0
    }

    private fun findTargetContainer(root: AccessibilityNodeInfo, amount: Int): AccessibilityNodeInfo? {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun find(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 18) return
            val own = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                .joinToString(" ").lowercase(Locale.ROOT)
            val digits = own.replace(",", "").replace(".", "").replace(" ", "")
            if (digits.contains(amount.toString()) && own.contains("diamond")) matches.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { find(it, depth + 1) }
        }
        find(root, 0)
        for (node in matches) {
            var current: AccessibilityNodeInfo? = node
            repeat(9) {
                val candidate = current ?: return@repeat
                val text = collectText(candidate).lowercase(Locale.ROOT)
                val digits = text.replace(",", "").replace(".", "").replace(" ", "")
                val correctAmount = digits.contains(amount.toString()) && text.contains("diamond")
                val hasStatusOrAction = listOf("stok habis", "habis", "beli", "klaim", "ambil", "redeem").any { text.contains(it) }
                if (correctAmount && hasStatusOrAction) return candidate
                current = candidate.parent
            }
        }
        return null
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