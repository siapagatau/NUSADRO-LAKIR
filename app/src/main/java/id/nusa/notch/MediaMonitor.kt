package id.nusa.notch

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock

/** Membaca sesi media aktif (Spotify, YouTube, dll.) lewat MediaSessionManager. */
object MediaMonitor {
    private var manager: MediaSessionManager? = null
    private var started = false
    private var current: MediaController? = null
    private val regs = ArrayList<Pair<MediaController, MediaController.Callback>>()

    val active: Boolean get() = started

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    /** Butuh izin "Akses notifikasi". Mengembalikan false bila belum diberikan. */
    fun start(ctx: Context): Boolean {
        if (started) return true
        return try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val comp = ComponentName(ctx, NusaNotificationListener::class.java)
            val list = msm.getActiveSessions(comp) // SecurityException bila izin belum ada
            msm.addOnActiveSessionsChangedListener(sessionsListener, comp)
            manager = msm
            started = true
            pick(list)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun stop() {
        manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        regs.forEach { it.first.unregisterCallback(it.second) }
        regs.clear()
        current = null
        started = false
        Store.media.value = null
    }

    private fun pick(list: List<MediaController>?) {
        regs.forEach { it.first.unregisterCallback(it.second) }
        regs.clear()
        list?.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            }
            c.registerCallback(cb)
            regs.add(c to cb)
        }
        publish()
    }

    private fun publish() {
        val all = regs.map { it.first }
        val keep = current
        val c = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: all.firstOrNull { keep != null && it.sessionToken == keep.sessionToken }
            ?: all.firstOrNull()
        current = c
        if (c == null) {
            Store.media.value = null
            return
        }
        val m = c.metadata
        val s = c.playbackState
        val title = m?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        if (title.isBlank()) {
            Store.media.value = null
            return
        }
        val artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val playing = s?.state == PlaybackState.STATE_PLAYING
        val at = s?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime()
        val speed = s?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        val art = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        Store.media.value = MediaInfo(
            title = title,
            artist = artist,
            durationMs = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            playing = playing,
            positionMs = s?.position ?: 0L,
            positionAt = at,
            speed = speed,
            art = art,
        )
    }

    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() {
        current?.transportControls?.skipToNext()
    }

    fun prev() {
        current?.transportControls?.skipToPrevious()
    }

    fun seekTo(ms: Long) {
        current?.transportControls?.seekTo(ms)
    }
}
