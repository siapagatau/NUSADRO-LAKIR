package id.nusa.notch

import android.media.RingtoneManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Pomodoro {
    private var job: Job? = null

    private fun ms(min: Int) = min * 60_000L

    fun toggle() {
        if (Store.pomodoro.value.running) pause() else start()
    }

    fun start() {
        val s = Store.pomodoro.value
        if (s.running) return
        val end = System.currentTimeMillis() + s.remainingMs
        Store.pomodoro.value = s.copy(running = true, endAt = end)
        job?.cancel()
        job = Store.scope.launch {
            var lastSec = -1L
            while (true) {
                val rem = end - System.currentTimeMillis()
                if (rem <= 0) {
                    finish()
                    break
                }
                val sec = (rem + 999) / 1000
                if (sec != lastSec) {
                    lastSec = sec
                    Store.pomodoro.value = Store.pomodoro.value.copy(remainingMs = rem)
                }
                delay(200)
            }
        }
    }

    fun pause() {
        job?.cancel()
        val s = Store.pomodoro.value
        Store.pomodoro.value = s.copy(
            running = false,
            remainingMs = (s.endAt - System.currentTimeMillis()).coerceAtLeast(0L),
        )
    }

    fun reset() {
        job?.cancel()
        val s = Store.pomodoro.value
        Store.pomodoro.value = s.copy(
            running = false,
            remainingMs = ms(if (s.focus) s.focusMin else s.breakMin),
        )
    }

    fun switchMode() {
        job?.cancel()
        val s = Store.pomodoro.value
        val f = !s.focus
        Store.pomodoro.value = s.copy(
            focus = f,
            running = false,
            remainingMs = ms(if (f) s.focusMin else s.breakMin),
        )
    }

    fun setDurations(focusMin: Int, breakMin: Int) {
        val f = focusMin.coerceIn(5, 90)
        val b = breakMin.coerceIn(1, 30)
        val s = Store.pomodoro.value
        Store.savePomo(f, b)
        Store.pomodoro.value = if (s.running) {
            s.copy(focusMin = f, breakMin = b)
        } else {
            s.copy(focusMin = f, breakMin = b, remainingMs = ms(if (s.focus) f else b))
        }
    }

    private fun finish() {
        val s = Store.pomodoro.value
        val nextFocus = !s.focus
        Store.pomodoro.value = s.copy(
            focus = nextFocus,
            running = false,
            remainingMs = ms(if (nextFocus) s.focusMin else s.breakMin),
            cycles = if (s.focus) s.cycles + 1 else s.cycles,
        )
        val msg = if (nextFocus) "Istirahat selesai. Saatnya fokus!" else "Sesi fokus selesai. Waktunya istirahat!"
        Reminders.notify(Store.app, 7001, "Pomodoro", msg)
        chime()
        Store.showAlert(
            Alert(
                AlertKind.POMO,
                if (nextFocus) "Istirahat selesai" else "Sesi fokus selesai",
                if (nextFocus) "Saatnya fokus" else "Waktunya istirahat",
            ),
            5000,
        )
    }

    private fun chime() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(Store.app, uri)?.play()
        } catch (e: Exception) {
            // abaikan
        }
    }
}
