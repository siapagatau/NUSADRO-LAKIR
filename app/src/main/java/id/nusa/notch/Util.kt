package id.nusa.notch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** mm:ss, dibulatkan ke bawah (untuk posisi lagu). */
fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** mm:ss, dibulatkan ke atas (untuk hitung mundur). */
fun fmtCeil(ms: Long): String {
    val s = ((ms + 999) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

fun fmtClockOf(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

fun fmtDateTime(ms: Long): String = SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(ms))
