package id.nusa.notch

import android.animation.ValueAnimator
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Foreground service yang menampilkan notch (Compose) sebagai overlay di atas layar.
 * Service juga menjadi LifecycleOwner supaya ComposeView bisa hidup di luar Activity.
 *
 * Cara kerja jendela (dua jendela):
 *  - Jendela VISUAL: layar penuh, tidak bisa disentuh (FLAG_NOT_TOUCHABLE), dan TIDAK PERNAH berubah ukuran
 *    atau posisi. Semua gambar dan gerak (membesar, mengecil, bergeser searah tepi, digeser/slime) dilakukan
 *    Compose di jendela ini, jadi tidak ada frame "basi" yang menampilkan notch di tempat lain sebelum
 *    melompat ke posisi akhir.
 *  - Jendela SENTUH: transparan dan kecil (sebesar notch + tepi) agar sentuhan di luar notch tetap sampai ke
 *    aplikasi di bawahnya. Ukuran/posisinya boleh berubah kapan saja karena tidak menggambar apa pun; semua
 *    sentuhan diteruskan ke jendela visual. Saat digeser/meluncur jendela ini layar penuh.
 *  - Digeser: titik tempel notch mengikuti keliling layar (sudut membulat) dengan pegas, jadi bentuknya
 *    memutar dan meregang seperti slime.
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val vmStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = vmStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: TouchFrame? = null
    private var overlayLp: WindowManager.LayoutParams? = null
    private var inputView: InputFrame? = null
    private var inputLp: WindowManager.LayoutParams? = null
    private var attached = false
    private var curW = 0
    private var curH = 0
    private var shrinkJob: Job? = null
    private var audioCallback: AudioDeviceCallback? = null
    private var receiversRegistered = false
    @Volatile private var devicesReady = false
    private var notifiedFull = false
    private var notifiedCritical = false

    // ---- posisi & gerak ----
    private var scrW = 1080
    private var scrH = 2340
    private var perimeter: Perimeter? = null
    private var restS = 0f          // posisi diam di keliling layar
    private var pAnim: ValueAnimator? = null
    private var lastBoxArea = 0f
    private var animTo = Float.NaN
    private var winP = 0f           // pusat jendela searah tepi (px) saat diam
    private var moving = false      // jendela persegi, notch bebas berputar
    private var dragging = false
    private var mS = 0f             // posisi saat ini di keliling (pegas)
    private var mV = 0f
    private var snapS = 0f          // tujuan saat dilepas
    private var fx = 0f
    private var fy = 0f
    private var lastNs = 0L
    private var grabOff = 0f
    private var d0 = 0f
    private var aS = 0f             // sudut bentuk (pegas)
    private var aV = 0f
    private var pS = 0f             // tarikan karet (pegas)
    private var pV = 0f
    private var lagPx = 0f
    private var releasedAt = 0L
    private val tmp = FloatArray(8)
    private val density: Float get() = resources.displayMetrics.density

    // ---- guncangan ----
    private var shakeRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)

        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, buildNotification(), type)

        if (!Settings.canDrawOverlays(this) && NusaAccessibilityService.instance == null) {
            stopSelf()
            return
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        addOverlay()
        Store.serviceRunning.value = true

        // Ukuran notch atau ukuran layar berubah: sesuaikan jendela (tidak menyentuh WindowManager bila tidak berubah).
        scope.launch {
            combine(Store.notchBox, Store.screenWdp, Store.screenHdp) { _, _, _ -> 0 }
                .collect { applyWindow(false) }
        }
        // Layanan Aksesibilitas menyala/mati: pindahkan jendela ke tipe yang sesuai.
        scope.launch {
            var first = true
            Store.a11yActive.collect {
                if (first) first = false else addOverlay()
            }
        }
        // Tombol "kembalikan posisi" dari aplikasi: notch meluncur ke tengah atas.
        scope.launch { Store.resetRequests.collect { moveTo(Store.restEdge, Store.restFrac) } }
        // Kolom ketik di notch (tugas): jendela dibuat bisa fokus hanya selama mengetik.
        scope.launch { Store.editing.collect { setFocusable(it) } }
        // Guncangan HP.
        scope.launch {
            Store.shakeEnabled.collect { if (it) registerShake() else unregisterShake() }
        }

        // Sambungkan ke sesi media begitu izin akses notifikasi diberikan (tidak memanggil sistem sebelum itu).
        scope.launch {
            while (true) {
                if (!MediaMonitor.active &&
                    NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(packageName)
                ) {
                    MediaMonitor.start(applicationContext)
                }
                delay(5000)
            }
        }
        Lyrics.watch(scope)
        registerPowerReceiver()
        registerAudioCallback()
        startWaterReminderLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Store.serviceRunning.value = false
        scope.cancel()
        Choreographer.getInstance().removeFrameCallback(frameCb)
        pAnim?.cancel()
        unregisterShake()
        MediaMonitor.stop()
        if (receiversRegistered) {
            try { unregisterReceiver(powerReceiver) } catch (e: Exception) { }
        }
        audioCallback?.let {
            getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(it)
        }
        removeOverlay()
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        vmStore.clear()
        super.onDestroy()
    }

    // ---------- Overlay ----------
    /**
     * FrameLayout yang meneruskan sentuhan mentah ke [onRawTouch] dan mengambil alih sentuhan menjadi geseran
     * (pindah posisi) bila jari bergerak melewati ambang dan notch sedang tertutup. Memakai koordinat layar
     * (raw), karena jendelanya sendiri ikut bergerak di bawah jari.
     */
    private class TouchFrame(ctx: Context) : FrameLayout(ctx) {
        var onRawTouch: ((MotionEvent) -> Unit)? = null
        var canDrag: () -> Boolean = { false }
        var isBusy: () -> Boolean = { false }
        var hitsNotch: (Float, Float) -> Boolean = { _, _ -> true }
        var onDragStart: (Float, Float) -> Unit = { _, _ -> }
        var onDragMove: (Float, Float) -> Unit = { _, _ -> }
        var onDragEnd: () -> Unit = { }

        private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var armed = false
        private var drag = false

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            onRawTouch?.invoke(ev)
            return super.dispatchTouchEvent(ev)
        }

        private fun far(ev: MotionEvent) = hypot(ev.rawX - downX, ev.rawY - downY) > slop

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    drag = false
                    // Notch sedang meluncur: hanya sentuhan tepat di notch yang meraihnya.
                    val busy = isBusy() && hitsNotch(ev.rawX, ev.rawY)
                    armed = busy || (!isBusy() && canDrag())
                    // saat notch masih meluncur ke tepi, ketukan tidak diteruskan ke isi (bisa diraih lagi)
                    if (busy) return true
                }
                MotionEvent.ACTION_MOVE -> if (armed && !drag && far(ev)) {
                    drag = true
                    onDragStart(ev.rawX, ev.rawY)
                    return true
                }
            }
            return drag
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> return true
                MotionEvent.ACTION_MOVE -> {
                    if (!drag && armed && far(ev)) {
                        drag = true
                        onDragStart(ev.rawX, ev.rawY)
                    }
                    if (drag) onDragMove(ev.rawX, ev.rawY)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (drag) {
                        drag = false
                        onDragEnd()
                    }
                    armed = false
                }
            }
            return true
        }
    }

    /**
     * Jendela sentuh transparan. Meneruskan semua sentuhan ke jendela visual (layar penuh, koordinat lokal =
     * koordinat layar). Offset dihitung dari selisih rawX/x pada tiap event, jadi tetap benar walau jendela ini
     * diubah ukuran/posisinya di tengah geseran.
     */
    private class InputFrame(ctx: Context) : FrameLayout(ctx) {
        var target: View? = null
        var onOutside: (MotionEvent) -> Unit = { }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutside(ev)
                return true
            }
            val t = target ?: return true
            val c = MotionEvent.obtain(ev)
            c.offsetLocation(ev.rawX - ev.x, ev.rawY - ev.y)
            t.dispatchTouchEvent(c)
            c.recycle()
            return true
        }
    }

    private fun removeOverlay() {
        shrinkJob?.cancel()
        pAnim?.cancel()
        inputView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
        inputView = null
        inputLp = null
        overlayView = null
        overlayLp = null
        attached = false
        curW = 0
        curH = 0
    }

    private fun addOverlay() {
        removeOverlay()
        // Dengan Aksesibilitas aktif, jendela dipasang lewat layanan itu (tipe ACCESSIBILITY_OVERLAY):
        // digambar di atas status bar dan tetap menerima ketukan. Tanpa itu, pakai overlay biasa.
        val a11y = NusaAccessibilityService.instance
        val host: Context = a11y ?: this
        val wm = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val type = if (a11y != null) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        val lp = WindowManager.LayoutParams(
            1, 1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            layoutInDisplayCutoutMode = cutoutMode()
        }
        overlayLp = lp
        // Jendela sentuh: kecil, transparan, menerima sentuhan lalu meneruskannya ke jendela visual.
        val ilp = WindowManager.LayoutParams(
            1, 1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            layoutInDisplayCutoutMode = cutoutMode()
        }
        inputLp = ilp

        val owner = this
        val frame = TouchFrame(this)
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { NusaTheme { Notch() } }
        }
        frame.setViewTreeLifecycleOwner(owner)
        frame.setViewTreeViewModelStoreOwner(owner)
        frame.setViewTreeSavedStateRegistryOwner(owner)
        frame.addView(
            compose,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        frame.onRawTouch = { ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> {
                    Store.collapse.tryEmit(Unit)
                    Store.activity.tryEmit(Unit)
                    // Android 12+ mengosongkan koordinat sentuhan di luar jendela; abaikan bila nol.
                    if (ev.rawX != 0f || ev.rawY != 0f) emitGaze(frame, ev)
                }
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!dragging) emitGaze(frame, ev)
                }
                else -> { }
            }
        }
        frame.canDrag = { !Store.panelOpen.value }
        frame.isBusy = { moving && !dragging }
        frame.hitsNotch = { x, y -> hitNotch(x, y) }
        frame.onDragStart = { x, y -> beginDrag(x, y) }
        frame.onDragMove = { x, y -> fx = x; fy = y }
        frame.onDragEnd = { endDrag() }
        overlayView = frame
        val input = InputFrame(this)
        input.target = frame
        input.onOutside = { ev -> frame.onRawTouch?.invoke(ev) }
        inputView = input

        moving = false
        dragging = false
        Store.dragPhase.value = 0
        Choreographer.getInstance().removeFrameCallback(frameCb)
        refreshScreen()
        lp.width = scrW
        lp.height = scrH
        lp.x = 0
        lp.y = 0
        Store.pose.value = Pose.rest(Store.restEdge, scrW, scrH, alongTarget(Store.restEdge))
        applyWindow(true)
        try {
            wm.addView(frame, lp)
            wm.addView(input, ilp)
            attached = true
            frame.viewTreeObserver.addOnGlobalLayoutListener { syncWinOffset() }
            frame.post { syncWinOffset() }
        } catch (e: Exception) {
            try { wm.removeView(frame) } catch (e2: Exception) { }
            overlayView = null
            overlayLp = null
            inputView = null
            inputLp = null
        }
    }

    /**
     * Baca posisi asli jendela visual di layar. Kalau sistem menggesernya dari (0,0), Compose memakai selisih ini
     * untuk menempatkan notch, supaya sisi kiri/kanan/atas/bawah tetap rapat ke tepi layar sungguhan.
     */
    private fun syncWinOffset() {
        val frame = overlayView ?: return
        if (!frame.isAttachedToWindow) return
        val v: View = if (frame.childCount > 0) frame.getChildAt(0) else frame
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        if (Store.winOffX.value != loc[0]) Store.winOffX.value = loc[0]
        if (Store.winOffY.value != loc[1]) Store.winOffY.value = loc[1]
    }

    private fun cutoutMode(): Int = if (Build.VERSION.SDK_INT >= 30) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    /** Jendela visual selalu seukuran layar (hanya diubah saat layar diputar). */
    private fun layoutVisual() {
        val v = overlayView ?: return
        val lp = overlayLp ?: return
        if (lp.width == scrW && lp.height == scrH && lp.x == 0 && lp.y == 0) return
        lp.width = scrW
        lp.height = scrH
        lp.x = 0
        lp.y = 0
        if (attached) {
            try { windowManager?.updateViewLayout(v, lp) } catch (e: Exception) { }
        }
    }

    private fun emitGaze(frame: View, ev: MotionEvent) {
        val loc = IntArray(2)
        frame.getLocationOnScreen(loc)
        Store.gaze.value = Gaze(ev.rawX - loc[0], ev.rawY - loc[1], System.nanoTime())
    }

    private fun setFocusable(on: Boolean) {
        val lp = overlayLp ?: return
        val view = overlayView ?: return
        val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val has = lp.flags and f != 0
        if (on == !has) return
        lp.flags = if (on) lp.flags and f.inv() else lp.flags or f
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        if (attached) {
            try { windowManager?.updateViewLayout(view, lp) } catch (e: Exception) { }
        }
    }

    // ---------- Layar & keliling ----------
    private fun realScreen(): Pair<Int, Int> {
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }

    private fun refreshScreen() {
        val (w, h) = realScreen()
        scrW = w.coerceAtLeast(1)
        scrH = h.coerceAtLeast(1)
        Store.setScreenPx(scrW, scrH, density)
        val per = Perimeter(scrW.toFloat(), scrH.toFloat(), CORNER_DP * density)
        perimeter = per
        restS = per.restToS(Store.restEdge, Store.restFrac)
        mS = restS
        mV = 0f
    }

    /** Titik tempel notch saat diam, searah tepi (x untuk atas/bawah, y untuk kiri/kanan). */
    private fun restAlong(edge: Int): Float {
        val per = perimeter ?: return 0f
        per.at(restS, tmp)
        return if (edge == 0 || edge == 2) tmp[0] else tmp[1]
    }

    // ---------- Jendela sentuh ----------
    /**
     * Pusat notch searah tepi saat diam (px): titik tempel, dijepit supaya jendela sentuh (sebesar notch akhir
     * + tepi) tidak keluar layar. Bila panel lebih lebar dari ruang yang ada, di tengah layar.
     */
    private fun alongTarget(edge: Int): Float {
        val horiz = edge == 0 || edge == 2
        val la = Math.ceil((Store.notchBox.value.w * density).toDouble()).toInt().coerceAtLeast(1)
        val alongWant = la + 2 * Math.round(PAD_ALONG_DP * density)
        val len = if (horiz) scrW else scrH
        val attachP = restAlong(edge)
        return if (len > alongWant) attachP.coerceIn(alongWant / 2f, len - alongWant / 2f) else len / 2f
    }

    private fun publishAlong(p: Float) {
        val cur = Store.pose.value
        if (cur.along != p) Store.pose.value = cur.copy(along = p)
    }

    /**
     * Geser notch searah tepi (mis. panel masuk ke dalam layar). Hanya menulis state pose tiap frame (tanpa
     * menyentuh WindowManager) dan berjalan di callback animasi yang sama dengan frame Compose, jadi posisi
     * dan ukuran selalu sinkron.
     */
    private fun animateAlong(to: Float, delayMs: Long) {
        if (pAnim?.isRunning == true && animTo == to) return   // sudah menuju tujuan yang sama
        pAnim?.cancel()
        animTo = to
        val from = Store.pose.value.along
        if (abs(to - from) < 0.5f) {
            publishAlong(to)
            return
        }
        val a = ValueAnimator.ofFloat(from, to)
        a.duration = 620
        a.startDelay = delayMs
        a.interpolator = PathInterpolator(0.34f, 1.32f, 0.5f, 1f)
        a.addUpdateListener { if (!moving) publishAlong(it.animatedValue as Float) }
        pAnim = a
        a.start()
    }

    /**
     * Ukuran jendela sentuh mengikuti ukuran akhir notch (supaya panel tidak terpotong). Membesar langsung;
     * mengecil setelah animasi selesai. Jendela ini tidak menggambar apa pun, jadi perubahannya tidak terlihat;
     * gerak notch searah tepi dianimasikan Compose lewat [Pose.along].
     */
    private fun applyWindow(force: Boolean) {
        val view = inputView ?: return
        val lp = inputLp ?: return
        if (moving) {
            applyMoving(view, lp)
            return
        }
        val d = density
        val box = Store.notchBox.value
        val la = Math.ceil((box.w * d).toDouble()).toInt().coerceAtLeast(1)
        val ln = Math.ceil((box.h * d).toDouble()).toInt().coerceAtLeast(1)
        val edge = Store.pose.value.edge
        val horiz = edge == 0 || edge == 2
        val padA = Math.round(PAD_ALONG_DP * d)
        val padN = Math.round(PAD_NORMAL_DP * d)
        val wantW = if (horiz) la + 2 * padA else ln + padN
        val wantH = if (horiz) ln + padN else la + 2 * padA

        shrinkJob?.cancel()
        val immediate = force || curW == 0
        val tw = if (immediate) wantW else maxOf(wantW, curW)
        val th = if (immediate) wantH else maxOf(wantH, curH)

        winP = alongTarget(edge)
        val areaNow = box.w * box.h
        if (immediate) {
            pAnim?.cancel()
            publishAlong(winP)
        } else {
            val area = box.w * box.h
            // sama dengan Compose: animasi ukuran menunggu ~3 frame hanya saat membesar
            animateAlong(winP, if (area > lastBoxArea + 1f) 45L else 0L)
        }
        lastBoxArea = areaNow
        commit(tw, th, edge)
        if (!immediate && (wantW < tw || wantH < th)) {
            shrinkJob = scope.launch {
                delay(760)
                commit(wantW, wantH, Store.pose.value.edge)
            }
        }
    }

    private fun commit(w: Int, h: Int, edge: Int) {
        val view = inputView ?: return
        val lp = inputLp ?: return
        val horiz = edge == 0 || edge == 2
        val x: Int
        val y: Int
        if (horiz) {
            x = Math.round(winP - w / 2f)
            y = if (edge == 0) 0 else scrH - h
        } else {
            y = Math.round(winP - h / 2f)
            x = if (edge == 3) 0 else scrW - w
        }
        curW = w
        curH = h
        // Tidak ada perubahan nyata -> jangan sentuh WindowManager.
        if (attached && lp.width == w && lp.height == h && lp.x == x && lp.y == y) return
        lp.width = w
        lp.height = h
        lp.x = x
        lp.y = y
        lp.gravity = Gravity.TOP or Gravity.LEFT
        if (attached) {
            try { windowManager?.updateViewLayout(view, lp) } catch (e: Exception) { }
        }
    }

    private fun applyMoving(view: View, lp: WindowManager.LayoutParams) {
        if (dragging) {
            // Saat jari menyeret, jendela sentuh layar penuh supaya geseran tidak terputus di mana pun jari berada.
            curW = scrW
            curH = scrH
            setInputBounds(view, lp, 0, 0, scrW, scrH)
        } else {
            // Saat notch meluncur/memantul setelah dilepas, jendela sentuh hanya sebesar notch dan mengikutinya.
            // Sentuhan di luar notch langsung sampai ke aplikasi di bawah dan tidak lagi menggeser notch.
            val pose = Store.pose.value
            val box = Store.notchBox.value
            val d = density
            val sc = maxOf(pose.sx, pose.sy, 1f)
            val r = Math.ceil((hypot(box.w * d, box.h * d) / 2f * sc + 12f * d).toDouble()).toInt().coerceAtLeast(1)
            val x = Math.round(pose.cx) - r
            val y = Math.round(pose.cy) - r
            curW = 2 * r
            curH = 2 * r
            setInputBounds(view, lp, x, y, 2 * r, 2 * r)
        }
    }

    private fun setInputBounds(view: View, lp: WindowManager.LayoutParams, x: Int, y: Int, w: Int, h: Int) {
        if (attached && lp.width == w && lp.height == h && lp.x == x && lp.y == y) return
        lp.width = w
        lp.height = h
        lp.x = x
        lp.y = y
        lp.gravity = Gravity.TOP or Gravity.LEFT
        if (attached) {
            try { windowManager?.updateViewLayout(view, lp) } catch (e: Exception) { }
        }
    }

    /** Apakah titik layar (px) berada di badan notch yang sedang meluncur (persegi panjang berputar + sedikit toleransi). */
    private fun hitNotch(x: Float, y: Float): Boolean {
        val pose = Store.pose.value
        val box = Store.notchBox.value
        val d = density
        val slack = 10f * d
        val halfW = box.w * d / 2f * pose.sx + slack
        val halfH = maxOf(box.h * d / 2f * pose.sy + slack, 24f * d)
        val dx = x - pose.cx
        val dy = y - pose.cy
        // Dicoba dua arah putar agar tidak bergantung pada konvensi tanda sudut.
        for (sign in floatArrayOf(1f, -1f)) {
            val rad = Math.toRadians((pose.shapeDeg * sign).toDouble())
            val c = Math.cos(rad).toFloat()
            val sn = Math.sin(rad).toFloat()
            val lx = dx * c + dy * sn
            val ly = -dx * sn + dy * c
            if (abs(lx) <= halfW && abs(ly) <= halfH) return true
        }
        return false
    }

    // ---------- Geser, karet & slime ----------
    private fun wrapDeg(x: Float): Float {
        var v = x % 360f
        if (v > 180f) v -= 360f
        if (v <= -180f) v += 360f
        return v
    }

    private fun beginDrag(x: Float, y: Float) {
        val view = overlayView ?: return
        val per = perimeter ?: return
        dragging = true
        fx = x
        fy = y
        if (!moving) enterMoving()
        applyWindow(false)        // jendela sentuh -> layar penuh selama diseret
        // Notch menahan titik yang dipegang: tidak melompat ke jari.
        grabOff = per.delta(per.nearest(fx, fy), mS)
        per.at(mS, tmp)
        d0 = (fx - tmp[0]) * -tmp[2] + (fy - tmp[1]) * -tmp[3]
        Store.dragPhase.value = 1
        Store.activity.tryEmit(Unit)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        startFrames()
    }

    private fun enterMoving() {
        val per = perimeter ?: return
        moving = true
        mS = restS
        mV = 0f
        pS = 0f
        pV = 0f
        aV = 0f
        lagPx = 0f
        lastNs = 0L
        per.at(mS, tmp)
        aS = tmp[4]
        shrinkJob?.cancel()
        pAnim?.cancel()
        publishPose(0f)           // pose penuh (pusat badan) dipasang lebih dulu; jendela visual tidak berubah
        applyWindow(false)
    }

    private fun endDrag() {
        dragging = false
        val per = perimeter ?: return
        releasedAt = SystemClock.elapsedRealtime()
        Store.dragPhase.value = 2
        publishPose(lagPx)
        applyWindow(false)        // jendela sentuh kembali sebesar notch
        // meluncur sedikit mengikuti kecepatan, lalu menempel ke sisi lurus terdekat
        snapS = per.snap(mS + mV * 0.14f)
        startFrames()
    }

    /** Meluncur otomatis ke posisi diam tertentu (tombol "kembalikan posisi"). */
    private fun moveTo(edge: Int, frac: Float) {
        val per = perimeter ?: return
        if (overlayView == null) return
        dragging = false
        if (!moving) enterMoving()
        releasedAt = SystemClock.elapsedRealtime()
        Store.dragPhase.value = 2
        snapS = per.restToS(edge, frac)
        startFrames()
    }

    private fun startFrames() {
        val c = Choreographer.getInstance()
        c.removeFrameCallback(frameCb)
        c.postFrameCallback(frameCb)
    }

    /** Hitung pose dari keadaan fisika dan tulis ke Compose. */
    private fun publishPose(lag: Float) {
        val per = perimeter ?: return
        per.at(mS, tmp)
        val dens = density
        val ln = Store.notchBox.value.h * dens
        val bell = tmp[6]
        val lagK = min(0.3f, abs(lag) / dens / 260f)
        val sx = (1f + lagK - 0.28f * pS) * (1f - 0.14f * bell)
        val sy = (1f + 0.9f * pS - 0.4f * lagK) * (1f + 0.06f * bell)
        val tilt = (mV / dens / 90f).coerceIn(-10f, 10f)
        val extra = wrapDeg(aS - tmp[4])
        Store.pose.value = Pose(
            edge = Store.pose.value.edge,
            shapeDeg = aS + tilt,
            contentDeg = tmp[5] + extra + tilt,
            sx = sx,
            sy = sy,
            cx = tmp[0] - tmp[2] * ln / 2f,
            cy = tmp[1] - tmp[3] * ln / 2f,
            pull = pS.coerceIn(0f, 1f),
            fw = scrW,
            fh = scrH,
            along = Store.pose.value.along,
            moving = true,
        )
    }

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!moving) return
            val per = perimeter ?: return
            val dens = density
            val dt = if (lastNs == 0L) 1f / 60f else ((frameTimeNanos - lastNs) / 1e9f).coerceIn(0.001f, 0.033f)
            lastNs = frameTimeNanos

            var pullT = 0f
            val goal: Float
            if (dragging) {
                goal = per.nearestSticky(fx, fy, mS, 48f * dens) + grabOff
                per.at(mS, tmp)
                val dIn = (fx - tmp[0]) * -tmp[2] + (fy - tmp[1]) * -tmp[3]
                pullT = ((dIn - d0) / (150f * dens)).coerceIn(0f, 1f)
            } else {
                goal = snapS
            }
            val target = mS + per.delta(mS, goal)
            lagPx = target - mS

            // Posisi: saat dipegang redam kritis (halus, tanpa gentar); saat dilepas memantul seperti slime.
            val w1 = if (dragging) 15f else 19f
            val z1 = if (dragging) 1.0f else 0.42f
            // Sudut bentuk: ketinggalan sedikit lalu menyusul, jadi sudut terasa lentur, bukan kaku.
            per.at(mS, tmp)
            val angT = tmp[4]
            val w2 = 20f
            val z2 = 0.5f
            // Tarikan karet: memantul bila dilepas.
            val w3 = if (dragging) 26f else 22f
            val z3 = if (dragging) 0.8f else 0.3f
            val steps = 2
            val h = dt / steps
            repeat(steps) {
                mV += (w1 * w1 * (target - mS) - 2f * z1 * w1 * mV) * h
                mS += mV * h
                val e = wrapDeg(angT - aS)
                aV += (w2 * w2 * e - 2f * z2 * w2 * aV) * h
                aS += aV * h
                pV += (w3 * w3 * (pullT - pS) - 2f * z3 * w3 * pV) * h
                pS += pV * h
            }
            publishPose(lagPx)
            if (!dragging) applyWindow(false)   // jendela sentuh mengikuti notch

            val now = SystemClock.elapsedRealtime()
            val calm = abs(lagPx) < 0.6f && abs(mV) < 8f && abs(wrapDeg(angT - aS)) < 0.6f &&
                abs(aV) < 6f && abs(pS) < 0.01f && abs(pV) < 0.05f
            if (!dragging && (calm || now - releasedAt > 4000)) finishMove() else Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun finishMove() {
        val per = perimeter ?: return
        val (edge, frac) = per.toRest(snapS)
        Store.saveRest(edge, frac)
        restS = per.restToS(edge, frac)
        mS = restS
        mV = 0f
        // Jendela visual tidak pernah berubah ukuran, jadi pose diam dan posisi akhir dipasang sekaligus:
        // tidak ada frame perantara. Hanya jendela sentuh (tak terlihat) yang diperkecil.
        if (dragging) return
        pAnim?.cancel()
        moving = false
        val along = alongTarget(edge)
        Store.pose.value = Pose(
            edge, Pose.shapeOf(edge), Pose.contentOf(edge), 1f, 1f,
            0f, 0f, 0f, scrW, scrH, along, false,
        )
        applyWindow(true)
        Store.dragPhase.value = 0
        Store.settleCount.value = Store.settleCount.value + 1
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Layar diputar: posisi (sisi + pecahan) dipertahankan, keliling dihitung ulang.
        Choreographer.getInstance().removeFrameCallback(frameCb)
        pAnim?.cancel()
        moving = false
        dragging = false
        Store.dragPhase.value = 0
        refreshScreen()
        layoutVisual()
        Store.pose.value = Pose.rest(Store.restEdge, scrW, scrH, alongTarget(Store.restEdge))
        applyWindow(true)
        overlayView?.post { syncWinOffset() }
    }

    // ---------- Guncangan ----------
    private val shakeListener = object : SensorEventListener {
        private var peaks = 0
        private var firstPeak = 0L
        private var lastPeak = 0L
        private var lastFire = 0L

        override fun onSensorChanged(e: SensorEvent) {
            val x = e.values[0]
            val y = e.values[1]
            val z = e.values[2]
            // Selisih dari gravitasi (9,8 m/s2): HP diam ~0, diayun/diguncang jelas di atas 6.
            val dev = abs(Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH)
            if (dev < 6f) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastPeak < 80) return
            lastPeak = now
            if (now - firstPeak > 1100) {
                firstPeak = now
                peaks = 0
            }
            peaks++
            if (peaks >= 3 && now - lastFire > 2500) {
                peaks = 0
                lastFire = now
                if (Store.shakeEnabled.value) {
                    scope.launch {
                        Store.activity.tryEmit(Unit)
                        Store.shake.tryEmit(Unit)
                        overlayView?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
    }

    private fun registerShake() {
        if (shakeRegistered) return
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        shakeRegistered = sm.registerListener(shakeListener, acc, SensorManager.SENSOR_DELAY_GAME, Handler(Looper.getMainLooper()))
    }

    private fun unregisterShake() {
        if (!shakeRegistered) return
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(shakeListener)
        shakeRegistered = false
    }

    // ---------- Baterai / charger ----------
    private fun batteryLevel(): Int {
        val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level < 0 || scale <= 0) 0 else level * 100 / scale
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val l = batteryLevel()
            Store.battery.value = l
            when (i.action) {
                Intent.ACTION_POWER_CONNECTED ->
                    Store.showAlert(Alert(AlertKind.CHARGE, "Mengisi daya", "Charger terhubung", "$l%"))
                Intent.ACTION_POWER_DISCONNECTED ->
                    Store.showAlert(Alert(AlertKind.UNPLUG, "Charger dilepas", "Menggunakan baterai", "$l%"))
                Intent.ACTION_BATTERY_LOW ->
                    Store.showAlert(Alert(AlertKind.LOW, "Baterai lemah", "Segera colok charger", "$l%"), 5000)
                Intent.ACTION_BATTERY_CHANGED -> {
                    val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                    if (plugged && l >= 100) {
                        if (!notifiedFull) {
                            notifiedFull = true
                            Store.showAlert(Alert(AlertKind.CHARGE, "Baterai penuh", "Boleh dicabut", "100%"), 4500)
                        }
                    } else if (l < 98) {
                        notifiedFull = false
                    }
                    if (!plugged && l <= 5) {
                        if (!notifiedCritical) {
                            notifiedCritical = true
                            Store.showAlert(Alert(AlertKind.LOW, "Baterai kritis", "Colok charger sekarang", "$l%"), 6000)
                        }
                    } else if (l > 8 || plugged) {
                        notifiedCritical = false
                    }
                }
                Intent.ACTION_SCREEN_OFF -> unregisterShake()
                Intent.ACTION_SCREEN_ON -> {
                    if (Store.shakeEnabled.value) registerShake()
                    Store.activity.tryEmit(Unit)
                }
                Intent.ACTION_USER_PRESENT -> Store.activity.tryEmit(Unit)
            }
        }
    }

    private fun registerPowerReceiver() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val l0 = batteryLevel()
        Store.battery.value = l0
        // Jangan memunculkan "penuh/kritis" untuk kondisi yang sudah ada sebelum layanan dinyalakan.
        notifiedFull = l0 >= 100
        notifiedCritical = l0 <= 8
        ContextCompat.registerReceiver(this, powerReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiversRegistered = true
    }

    // ---------- Headphone ----------
    private fun isHeadphone(d: AudioDeviceInfo): Boolean {
        if (!d.isSink) return false
        return when (d.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET -> true
            else -> Build.VERSION.SDK_INT >= 31 && d.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }

    private fun registerAudioCallback() {
        val am = getSystemService(AudioManager::class.java)
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (!devicesReady) return
                val d = addedDevices.firstOrNull { isHeadphone(it) } ?: return
                val name = d.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Headphone"
                Store.showAlert(Alert(AlertKind.HEADPHONE, name, "Terhubung"))
            }
        }
        audioCallback = cb
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
        // Panggilan awal berisi perangkat yang sudah terpasang; abaikan.
        scope.launch {
            delay(1500)
            devicesReady = true
        }
    }

    // ---------- Pengingat minum ----------
    private fun startWaterReminderLoop() {
        Store.lastWaterPing = System.currentTimeMillis()
        scope.launch {
            while (true) {
                delay(60_000)
                Store.refreshDay()
                val w = Store.water.value
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (w.remindEveryMin > 0 && hour in 8..21 && w.totalMl < w.goalMl) {
                    val now = System.currentTimeMillis()
                    if (now - Store.lastWaterPing >= w.remindEveryMin * 60_000L) {
                        Store.lastWaterPing = now
                        Reminders.notify(
                            applicationContext, 7002, "Waktunya minum",
                            "${w.totalMl} / ${w.goalMl} ml hari ini"
                        )
                        Store.showAlert(
                            Alert(
                                AlertKind.WATER, "Waktunya minum",
                                "${w.totalMl} / ${w.goalMl} ml",
                                "${(w.totalMl * 100 / w.goalMl).coerceAtMost(100)}%",
                            ),
                            4500,
                        )
                    }
                }
            }
        }
    }

    // ---------- Notifikasi foreground ----------
    private fun buildNotification(): Notification {
        Reminders.ensureChannels(this)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, Reminders.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Nusa aktif")
            .setContentText("Notch tampil di atas layar")
            .setContentIntent(open)
            .addAction(0, "Matikan", stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val ACTION_STOP = "id.nusa.notch.STOP"
        private const val PAD_ALONG_DP = 14f
        private const val PAD_NORMAL_DP = 12f
        private const val CORNER_DP = 110f
    }
}
