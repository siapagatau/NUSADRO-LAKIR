package id.nusa.notch

import android.graphics.Bitmap
import android.os.SystemClock

data class Task(val id: Long, val title: String, val dueAt: Long?, val done: Boolean)

data class WaterEntry(val time: Long, val ml: Int)

data class WaterState(
    val goalMl: Int = 2000,
    val bottleMl: Int = 250,
    val remindEveryMin: Int = 90,
    val today: List<WaterEntry> = emptyList(),
) {
    val totalMl: Int get() = today.sumOf { it.ml }
}

enum class AlertKind { CHARGE, UNPLUG, LOW, HEADPHONE, WATER, GOAL, POMO }

data class Alert(
    val kind: AlertKind,
    val title: String,
    val sub: String = "",
    val value: String = "",
)

data class MediaInfo(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val playing: Boolean,
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float,
    val art: Bitmap?,
) {
    fun currentPosition(): Long {
        var p = positionMs
        if (playing) p += ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0L, durationMs) else p.coerceAtLeast(0L)
    }
}

data class LrcLine(val timeMs: Long, val text: String)

sealed interface LyricsState {
    object None : LyricsState
    object Loading : LyricsState
    object Missing : LyricsState
    data class Found(val lines: List<LrcLine>) : LyricsState
}

data class PomoState(
    val focus: Boolean = true,
    val running: Boolean = false,
    val remainingMs: Long = 25 * 60_000L,
    val endAt: Long = 0L,
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    val cycles: Int = 0,
)

/** Titik yang sedang dilihat maskot, dalam piksel pada koordinat root pemilik matanya. [stamp] membuat titik sama tetap dihitung baru. */
data class Gaze(val x: Float, val y: Float, val stamp: Long)

/** Ukuran jendela notch yang dibutuhkan (dp layar, sudah termasuk skala). */
data class BoxDp(val w: Float, val h: Float)
