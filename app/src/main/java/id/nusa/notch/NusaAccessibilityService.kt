package id.nusa.notch

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Layanan kosong. Tujuannya satu: memberi Nusa jendela bertipe TYPE_ACCESSIBILITY_OVERLAY.
 * Jendela jenis ini digambar DI ATAS status bar dan tetap menerima ketukan, jadi notch di
 * tengah layar bisa diketuk saat HP berdiri (potret). Tanpa ini, status bar sistem menelan
 * ketukan di area paling atas layar. Layanan ini tidak membaca isi layar dan tidak
 * menerima kejadian apa pun.
 */
class NusaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Store.a11yActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        Store.a11yActive.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        Store.a11yActive.value = false
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: NusaAccessibilityService? = null
    }
}
