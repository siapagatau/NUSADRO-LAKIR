package id.nusa.notch

import android.service.notification.NotificationListenerService

/**
 * Layanan kosong. Keberadaannya (dan izin "Akses notifikasi") dibutuhkan
 * agar MediaSessionManager mau memberi daftar sesi media yang aktif.
 */
class NusaNotificationListener : NotificationListenerService()
