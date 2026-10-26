package id.nusa.notch

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Penyimpanan + state global yang dipakai notch dan aplikasi. */
object Store {
    lateinit var app: Context
        private set
    private lateinit var prefs: SharedPreferences
    private var inited = false

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val water = MutableStateFlow(WaterState())
    val media = MutableStateFlow<MediaInfo?>(null)
    val lyrics = MutableStateFlow<LyricsState>(LyricsState.None)
    val alert = MutableStateFlow<Alert?>(null)
    val pomodoro = MutableStateFlow(PomoState())
    val serviceRunning = MutableStateFlow(false)
    val battery = MutableStateFlow(100)
    val openTab = MutableStateFlow(-1)
    val collapse = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val activity = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // ---------- Posisi & tampilan notch ----------
    /** Pose notch yang dibaca Compose tiap frame (snapshot state: hanya layout/gambar yang ikut berubah). */
    val pose = mutableStateOf(Pose())
    /**
     * Posisi pojok kiri-atas jendela visual di layar (px). Normalnya 0,0. Bila sistem/OEM menggeser jendela
     * (inset cutout, bar samping, dsb.), notch dikoreksi sebesar ini supaya tetap rapat ke tepi layar.
     */
    val winOffX = mutableStateOf(0)
    val winOffY = mutableStateOf(0)
    /** True saat panel notch sedang terbuka (geser-pindah dimatikan saat itu). */
    val panelOpen = MutableStateFlow(false)
    /** 0 diam, 1 sedang digeser, 2 dilepas (masih memantul). */
    val dragPhase = MutableStateFlow(0)
    /** Bertambah tiap notch selesai memantul setelah digeser (memicu ekspresi pusing). */
    val settleCount = MutableStateFlow(0)
    /** True saat kolom ketik di notch aktif: jendela dibuat bisa fokus agar keyboard muncul. */
    val editing = MutableStateFlow(false)
    /** HP diguncang: maskot muncul dengan ekspresi. */
    val shake = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** True bila layar sedang potret (tinggi >= lebar). */
    val portrait = MutableStateFlow(true)
    val petEnabled = MutableStateFlow(true)
    val eyeFollow = MutableStateFlow(true)
    val shakeEnabled = MutableStateFlow(true)
    val screenWdp = MutableStateFlow(360f)
    val screenHdp = MutableStateFlow(780f)
    /** Ukuran notch di layar (dp, sudah termasuk skala) pada keadaan AKHIR animasi. Jendela overlay mengikutinya. */
    val notchBox = MutableStateFlow(BoxDp(172f, 32f))
    /** True bila layanan Aksesibilitas Nusa aktif: notch bisa di atas status bar dan tetap bisa diketuk. */
    val a11yActive = MutableStateFlow(false)
    /** Tatapan maskot di overlay (koordinat jendela overlay). */
    val gaze = MutableStateFlow<Gaze?>(null)

    /** Posisi diam notch: sisi (0 atas, 1 kanan, 2 bawah, 3 kiri) dan pecahan 0..1 sepanjang sisi itu. */
    var restEdge: Int = 0
        private set
    var restFrac: Float = 0.5f
        private set

    @Volatile var lastWaterPing: Long = System.currentTimeMillis()
    private var alertJob: Job? = null

    fun init(ctx: Context) {
        if (inited) return
        inited = true
        app = ctx.applicationContext
        prefs = app.getSharedPreferences("nusa", Context.MODE_PRIVATE)
        Reminders.ensureChannels(app)
        tasks.value = loadTasks()
        water.value = loadWater()
        val f = prefs.getInt("pomo_focus", 25)
        val b = prefs.getInt("pomo_break", 5)
        pomodoro.value = PomoState(focusMin = f, breakMin = b, remainingMs = f * 60_000L)
        restEdge = prefs.getInt("pos_edge", 0).coerceIn(0, 3)
        restFrac = prefs.getFloat("pos_frac", 0.5f).coerceIn(0f, 1f)
        pose.value = Pose.rest(restEdge)
        petEnabled.value = prefs.getBoolean("pet_on", true)
        eyeFollow.value = prefs.getBoolean("eye_follow", true)
        shakeEnabled.value = prefs.getBoolean("shake_on", true)
        updateScreen(app)
    }

    /** Simpan ukuran layar (dp) dan orientasi. */
    fun updateScreen(ctx: Context) {
        val dm = ctx.resources.displayMetrics
        if (dm.density > 0f) setScreenPx(dm.widthPixels, dm.heightPixels, dm.density)
    }

    fun setScreenPx(wPx: Int, hPx: Int, density: Float) {
        if (density <= 0f) return
        screenWdp.value = wPx / density
        screenHdp.value = hPx / density
        portrait.value = hPx >= wPx
    }

    fun saveRest(edge: Int, frac: Float) {
        restEdge = edge.coerceIn(0, 3)
        restFrac = frac.coerceIn(0f, 1f)
        prefs.edit().putInt("pos_edge", restEdge).putFloat("pos_frac", restFrac).apply()
    }

    fun resetPosition() {
        saveRest(0, 0.5f)
        resetRequests.tryEmit(Unit)
    }

    /** Diminta aplikasi: kembalikan notch ke tengah atas. */
    val resetRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun setShake(on: Boolean) {
        shakeEnabled.value = on
        prefs.edit().putBoolean("shake_on", on).apply()
    }

    fun setPet(on: Boolean) {
        petEnabled.value = on
        prefs.edit().putBoolean("pet_on", on).apply()
    }

    fun setEyeFollow(on: Boolean) {
        eyeFollow.value = on
        prefs.edit().putBoolean("eye_follow", on).apply()
    }

    // ---------- Alert singkat di notch ----------
    fun showAlert(a: Alert, ms: Long = 3500) {
        alertJob?.cancel()
        alert.value = a
        alertJob = scope.launch {
            delay(ms)
            alert.value = null
        }
    }

    // ---------- To-do ----------
    fun addTask(title: String, dueAt: Long?) {
        val t = Task(System.currentTimeMillis(), title.trim(), dueAt, false)
        tasks.value = tasks.value + t
        saveTasks()
        Reminders.schedule(app, t)
    }

    fun editTask(id: Long, title: String, dueAt: Long?) {
        val t = title.trim()
        if (t.isEmpty()) return
        tasks.value = tasks.value.map { if (it.id == id) it.copy(title = t, dueAt = dueAt) else it }
        saveTasks()
        val nt = tasks.value.firstOrNull { it.id == id } ?: return
        if (!nt.done) Reminders.schedule(app, nt)
    }

    fun clearDone() {
        tasks.value.filter { it.done }.forEach { Reminders.cancel(app, it.id) }
        tasks.value = tasks.value.filter { !it.done }
        saveTasks()
    }

    fun toggleTask(id: Long) {
        tasks.value = tasks.value.map { if (it.id == id) it.copy(done = !it.done) else it }
        saveTasks()
        val t = tasks.value.firstOrNull { it.id == id } ?: return
        if (t.done) Reminders.cancel(app, id) else Reminders.schedule(app, t)
    }

    fun deleteTask(id: Long) {
        Reminders.cancel(app, id)
        tasks.value = tasks.value.filter { it.id != id }
        saveTasks()
    }

    private fun loadTasks(): List<Task> = try {
        val arr = JSONArray(prefs.getString("tasks", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Task(
                id = o.getLong("id"),
                title = o.getString("title"),
                dueAt = if (o.has("due")) o.getLong("due") else null,
                done = o.optBoolean("done", false),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun saveTasks() {
        val arr = JSONArray()
        for (t in tasks.value) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("done", t.done)
            if (t.dueAt != null) o.put("due", t.dueAt)
            arr.put(o)
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    // ---------- Air minum ----------
    private fun dayStart(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Buang catatan hari kemarin saat hari berganti. */
    fun refreshDay() {
        val s = water.value
        val f = s.today.filter { it.time >= dayStart() }
        if (f.size != s.today.size) {
            water.value = s.copy(today = f)
            saveWater()
        }
    }

    fun addWater(ml: Int) {
        val s = water.value
        val now = System.currentTimeMillis()
        val ns = s.copy(today = s.today.filter { it.time >= dayStart() } + WaterEntry(now, ml))
        water.value = ns
        lastWaterPing = now
        saveWater()
        val pct = (ns.totalMl * 100 / ns.goalMl).coerceAtMost(100)
        if (s.totalMl < s.goalMl && ns.totalMl >= ns.goalMl) {
            showAlert(Alert(AlertKind.GOAL, "Target tercapai", "Kerja bagus!", "100%"), 4500)
        } else {
            showAlert(Alert(AlertKind.WATER, "+$ml ml", "${ns.totalMl} / ${ns.goalMl} ml", "$pct%"))
        }
    }

    fun undoWater() {
        val s = water.value
        if (s.today.isEmpty()) return
        water.value = s.copy(today = s.today.dropLast(1))
        saveWater()
    }

    fun setGoal(v: Int) {
        water.value = water.value.copy(goalMl = v.coerceIn(500, 6000))
        saveWater()
    }

    fun setBottle(v: Int) {
        water.value = water.value.copy(bottleMl = v.coerceIn(100, 1500))
        saveWater()
    }

    fun setRemind(v: Int) {
        water.value = water.value.copy(remindEveryMin = v.coerceIn(0, 240))
        saveWater()
    }

    private fun loadWater(): WaterState {
        val start = dayStart()
        val entries = try {
            val arr = JSONArray(prefs.getString("water_log", "[]"))
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                WaterEntry(o.getLong("t"), o.getInt("ml"))
            }.filter { it.time >= start }
        } catch (e: Exception) {
            emptyList()
        }
        return WaterState(
            goalMl = prefs.getInt("water_goal", 2000),
            bottleMl = prefs.getInt("water_bottle", 250),
            remindEveryMin = prefs.getInt("water_remind", 90),
            today = entries,
        )
    }

    private fun saveWater() {
        val s = water.value
        val arr = JSONArray()
        for (e in s.today) arr.put(JSONObject().put("t", e.time).put("ml", e.ml))
        prefs.edit()
            .putString("water_log", arr.toString())
            .putInt("water_goal", s.goalMl)
            .putInt("water_bottle", s.bottleMl)
            .putInt("water_remind", s.remindEveryMin)
            .apply()
    }

    // ---------- Pomodoro ----------
    fun savePomo(focusMin: Int, breakMin: Int) {
        prefs.edit().putInt("pomo_focus", focusMin).putInt("pomo_break", breakMin).apply()
    }
}
