package id.nusa.notch

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        Store.updateScreen(this)
        Store.openTab.value = intent.getIntExtra("tab", -1)
        setContent { NusaTheme { App() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Store.openTab.value = intent.getIntExtra("tab", -1)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Store.updateScreen(this)
    }
}

private const val TAB_COUNT = 5

private val NAV = listOf(
    NavItem("Beranda", I_HOME),
    NavItem("Tugas", I_CHECK_CIRCLE),
    NavItem("Air", P_DROP, filled = true),
    NavItem("Timer", I_STOPWATCH),
    NavItem("Atur", I_SLIDERS),
)

@Composable
private fun App() {
    var tab by rememberSaveable { mutableStateOf(0) }
    val openTab by Store.openTab.collectAsState()
    LaunchedEffect(openTab) {
        if (openTab in 0 until TAB_COUNT) {
            tab = openTab
            Store.openTab.value = -1
        }
    }
    // Tombol Kembali: dari tab mana pun kembali ke Beranda dulu, baru keluar.
    BackHandler(enabled = tab != 0) { tab = 0 }

    Scaffold(
        containerColor = NusaVoid,
        bottomBar = { NusaBottomBar(NAV, tab) { tab = it } },
    ) { pad ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.padding(pad).fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(90))
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeScreen { tab = it }
                1 -> TasksScreen()
                2 -> WaterScreen()
                3 -> TimerScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NusaInk,
    unfocusedTextColor = NusaInk,
    focusedContainerColor = Color(0x0DECEFF7),
    unfocusedContainerColor = Color(0x0DECEFF7),
    cursorColor = NusaCyan,
    focusedBorderColor = Color(0x52ECEFF7),
    unfocusedBorderColor = NusaLine,
    focusedPlaceholderColor = NusaMist,
    unfocusedPlaceholderColor = NusaMist,
)

private fun a11yEnabledInSettings(ctx: Context): Boolean {
    val v = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return v.split(':').any { it.startsWith(ctx.packageName + "/") && it.endsWith("NusaAccessibilityService") }
}

/** Buka layar pengaturan; bila tidak ada di perangkat ini, jatuh ke Info aplikasi. Tidak pernah crash. */
private fun safeStart(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")),
            )
        } catch (e2: Exception) { }
    }
}

/** Jalankan [onResume] tiap kali aplikasi kembali ke depan (mis. setelah memberi izin di Pengaturan). */
@Composable
private fun OnResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) onResume() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

// ---------------- Beranda ----------------
@Composable
private fun HomeScreen(goTab: (Int) -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    OnResume { tick++ }
    var launchedAt by remember { mutableLongStateOf(0L) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        // Android tidak lagi menampilkan dialog bila sudah ditolak dua kali: jawabannya datang instan.
        // Saat itu langsung buka pengaturan notifikasi aplikasi supaya tombol tidak terasa mati.
        if (!granted && System.currentTimeMillis() - launchedAt < 500) {
            safeStart(
                ctx,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
            )
        }
    }

    val overlayOk = remember(tick) { Settings.canDrawOverlays(ctx) }
    val listenerOk = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val a11yLive by Store.a11yActive.collectAsState()
    val a11yOk = a11yLive || remember(tick) { a11yEnabledInSettings(ctx) }
    val running by Store.serviceRunning.collectAsState()
    val tasks by Store.tasks.collectAsState()
    val water by Store.water.collectAsState()
    val pomo by Store.pomodoro.collectAsState()

    val canStart = overlayOk || a11yOk
    val allDone = overlayOk && listenerOk && notifOk && a11yOk
    val doneCount = listOf(overlayOk, listenerOk, notifOk, a11yOk).count { it }
    var showSetup by rememberSaveable { mutableStateOf(false) }

    Page {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.nusa_icon), null, Modifier.size(34.dp))
            Text("Nusa", color = NusaInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            StatusPill(running, "Notch aktif", "Notch mati")
        }

        // Kartu utama: nyalakan / matikan
        Panel {
            Text(
                if (running) "Notch menyala" else "Notch belum menyala",
                color = NusaInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5f).sp,
            )
            Text(
                when {
                    running -> "Ketuk notch di atas layar untuk membuka musik, timer, air, dan tugas."
                    !canStart -> "Beri izin \"Tampil di atas aplikasi lain\" di bawah dulu."
                    else -> "Tekan tombol di bawah. Notch muncul di tengah atas; tahan dan geser untuk memindahkannya."
                },
                color = NusaMist, fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            Spacer(Modifier.height(4.dp))
            GlowButton(
                text = if (running) "Matikan notch" else "Aktifkan notch",
                icon = if (running) null else I_POWER,
                enabled = canStart || running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (running) {
                    ctx.stopService(Intent(ctx, OverlayService::class.java))
                } else {
                    try {
                        ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java))
                    } catch (e: Exception) { }
                }
            }
        }

        // Izin
        Panel {
            Row(
                Modifier.fillMaxWidth().then(if (allDone) Modifier.clickable { showSetup = !showSetup } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (allDone) "Semua izin siap" else "Siapkan Nusa",
                        color = NusaInk, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (allDone) "Ketuk untuk melihat atau mengubah izin." else "Sekali saja. Datamu tetap di HP.",
                        color = NusaMist, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    "$doneCount/4", color = if (allDone) Color(0xFF33E07A) else NusaInk,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
            }
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Color(0x1FFFFFFF)),
            ) {
                Box(
                    Modifier.fillMaxWidth(doneCount / 4f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(NusaCyan, NusaIndigo, NusaViolet))),
                )
            }
            AnimatedVisibility(visible = !allDone || showSetup) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    PermRow(
                        "Tampil di atas aplikasi lain", "Supaya notch bisa muncul di atas layar.", "WAJIB",
                        overlayOk, Color(0xFF5FB2FF), Color(0xFF1F55D6), { Ico(I_LAYERS, 24f, 22f, stroke = 2f) },
                    ) {
                        safeStart(
                            ctx,
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")),
                        )
                    }
                    PermRow(
                        "Akses notifikasi", "Untuk membaca lagu yang diputar dan mengontrolnya.", "MUSIK",
                        listenerOk, Color(0xFFFF6AA2), Color(0xFF8B3DFF), { Ico(I_MUSIC, 24f, 22f, stroke = 2f) },
                    ) { safeStart(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    PermRow(
                        "Kirim notifikasi", "Untuk pengingat tugas, air minum, dan timer.", "PENGINGAT",
                        notifOk, Color(0xFFFF8A7A), Color(0xFFE8243A), { Ico(P_BELL, 24f, 22f) },
                    ) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            launchedAt = System.currentTimeMillis()
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    PermRow(
                        "Aksesibilitas", "Supaya notch bisa diketuk di atas status bar. Nusa tidak membaca layarmu.", "DISARANKAN",
                        a11yOk, Color(0xFF6EE79A), Color(0xFF159A45), { Ico(I_SHIELD, 24f, 22f, stroke = 2f) },
                    ) { safeStart(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    if (!listenerOk || !a11yOk) {
                        Text(
                            "Izin tampak abu-abu karena APK dipasang manual? Buka Info aplikasi Nusa, ketuk titik tiga, " +
                                "pilih \"Izinkan setelan terbatas\", lalu ulangi.",
                            color = NusaMist, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        // Ringkasan cepat
        SectionLabel("Hari ini", Modifier.padding(top = 4.dp))
        val openTasks = tasks.count { !it.done }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                "Tugas", if (openTasks == 0) "Beres" else "$openTasks", if (openTasks == 0) "tidak ada yang tertunda" else "belum selesai",
                Color(0xFF6EE79A), Color(0xFF159A45), { Ico(I_CHECK_CIRCLE, 24f, 22f, stroke = 2f) },
                Modifier.weight(1f),
            ) { goTab(1) }
            Tile(
                "Air", "%.1f L".format(java.util.Locale.US, water.totalMl / 1000f), "dari ${"%.1f".format(java.util.Locale.US, water.goalMl / 1000f)} L",
                Color(0xFF7FE0FF), Color(0xFF2D6BFF), { Ico(P_DROP, 24f, 20f) },
                Modifier.weight(1f),
            ) { goTab(2) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                "Timer", fmtCeil(pomo.remainingMs), if (pomo.running) (if (pomo.focus) "sedang fokus" else "sedang istirahat") else "siap dimulai",
                Color(0xFFFF8A7A), Color(0xFFE8243A), { Ico(I_STOPWATCH, 24f, 22f, stroke = 2f) },
                Modifier.weight(1f),
            ) { goTab(3) }
            Tile(
                "Geser", "bebas", "ke sekeliling layar",
                Color(0xFFF2F4F8), Color(0xFFA7AFBF), { Ico(I_SLIDERS, 24f, 22f, color = Color(0xFF1B2030), stroke = 2f) },
                Modifier.weight(1f),
            ) { goTab(4) }
        }

        // Bantuan
        SectionLabel("Bantuan", Modifier.padding(top = 8.dp))
        Column(Modifier.fillMaxWidth()) {
            Faq(
                "Kenapa notch tidak bisa diketuk saat HP berdiri?",
                "Status bar sistem menelan ketukan di area paling atas. Aktifkan layanan Aksesibilitas Nusa di atas: " +
                    "jendela notch jadi tampil di atas status bar dan bisa diketuk.",
            )
            Faq(
                "Notch menutupi kamera?",
                "Buka tab Atur lalu pilih Kiri atau Kanan. Saat HP direbahkan (landscape), notch otomatis pindah ke tengah atas.",
            )
            Faq(
                "Kapan maskot muncul?",
                "Setiap kali notch sepi (tidak ada musik, timer, atau pemberitahuan). Matanya berkedip dan melirik, " +
                    "tidur setelah 20 detik tanpa disentuh, dan melambai saat kamu kembali. Bisa dimatikan di tab Atur.",
            )
            Faq(
                "Apa yang dilakukan layanan Aksesibilitas?",
                "Hanya meminjam jenis jendela yang boleh tampil di atas status bar. Nusa tidak membaca isi layar, tidak merekam ketukan, dan tidak mengirim apa pun.",
            )
            Faq(
                "Kenapa lagu tidak punya lirik?",
                "Lirik berasal dari LRCLIB, pustaka komunitas gratis. Lagu populer hampir selalu ada; lagu lokal atau baru kadang belum.",
            )
        }
        Text(
            "Nusa untuk Android 1.2.0. Proyek pribadi, datamu tetap di HP.",
            color = NusaMist, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun Tile(
    title: String, value: String, sub: String, c1: Color, c2: Color,
    icon: @Composable BoxScope.() -> Unit, modifier: Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .pressable(onClick = onClick)
            .clip(shape)
            .background(NusaPanel)
            .border(1.dp, NusaLine, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(c1, c2, size = 36.dp, content = icon)
            Text(title, color = NusaMist, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column {
            Text(value, color = NusaInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5f).sp, maxLines = 1)
            Text(sub, color = NusaMist, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PermRow(
    title: String, desc: String, tag: String, granted: Boolean, c1: Color, c2: Color,
    icon: @Composable BoxScope.() -> Unit, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(c1, c2, size = 44.dp, content = icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = NusaInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                if (!granted) MiniTag(tag, if (tag == "WAJIB") Color(0xFFFF6B7A) else NusaMist)
            }
            Text(desc, color = NusaMist, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Ico(I_TICK, 24f, 16f, color = Color(0xFF33E07A), stroke = 2.6f)
                Text("Aktif", color = Color(0xFF8FF0B6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            PillBtn("Izinkan", onClick, filled = true)
        }
    }
}

@Composable
private fun Faq(q: String, a: String) {
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 45f else 0f, tween(250), label = "faqrot")
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(NusaLine))
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(q, color = NusaInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(Modifier.graphicsLayer { rotationZ = rot }) { Ico(I_PLUS_LINE, 24f, 18f, color = NusaMist, stroke = 2f) }
        }
        AnimatedVisibility(visible = open) {
            Text(a, color = NusaMist, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

// ---------------- Tugas ----------------
private fun pickTime(ctx: Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    TimePickerDialog(
        ctx,
        { _, h, m ->
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, h)
            c.set(Calendar.MINUTE, m)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
            onPicked(c.timeInMillis)
        },
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        true,
    ).show()
}

@Composable
private fun TasksScreen() {
    val ctx = LocalContext.current
    val tasks by Store.tasks.collectAsState()
    var text by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Long?>(null) }

    Page {
        ScreenTitle("Tugas", "Catat tugas, beri waktu, dan notch akan mengingatkanmu.")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Tulis tugas baru") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillBtn(
                if (due == null) "Atur pengingat" else fmtDateTime(due!!),
                { pickTime(ctx) { due = it } }, icon = I_ALARM,
            )
            if (due != null) PillBtn("Hapus", { due = null })
            Spacer(Modifier.weight(1f))
            PillBtn(
                "Tambah",
                {
                    Store.addTask(text, due)
                    text = ""
                    due = null
                },
                filled = true, enabled = text.isNotBlank(),
            )
        }

        if (tasks.isEmpty()) {
            Panel { Text("Belum ada tugas.", color = NusaMist, fontSize = 14.sp) }
        }
        tasks.sortedBy { it.done }.forEach { t ->
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (t.done) Color(0x6128783C) else NusaPanel)
                    .border(1.dp, NusaLine, shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (t.done) Modifier.background(Color(0xFF2FD06B))
                            else Modifier.border(1.5f.dp, Color(0x66ECEFF7), CircleShape),
                        )
                        .clickable { Store.toggleTask(t.id) },
                    contentAlignment = Alignment.Center,
                ) { if (t.done) Ico(I_TICK, 24f, 14f, stroke = 3f) }
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        color = if (t.done) Color(0xBFFFFFFF) else NusaInk, fontSize = 15.sp,
                        textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    t.dueAt?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Ico(I_ALARM, 24f, 12f, color = NusaMist, stroke = 2.2f)
                            Text(fmtDateTime(it), fontSize = 12.sp, color = NusaMist)
                        }
                    }
                }
                Box(Modifier.size(36.dp).clip(CircleShape).clickable { Store.deleteTask(t.id) }, contentAlignment = Alignment.Center) {
                    Ico(I_TRASH, 24f, 18f, color = NusaMist, stroke = 2f)
                }
            }
        }
    }
}

// ---------------- Air ----------------
@Composable
private fun WaterScreen() {
    val w by Store.water.collectAsState()
    val frac = (w.totalMl.toFloat() / w.goalMl).coerceIn(0f, 1f)
    val bar by animateFloatAsState(frac, tween(700), label = "wbar")

    Page {
        ScreenTitle("Air minum", "Jaga tubuhmu tetap terhidrasi sepanjang hari.")
        Panel {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(java.util.Locale.US, w.totalMl / 1000f), color = NusaInk,
                    fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5f).sp,
                )
                Text(
                    "  / %.1f L".format(java.util.Locale.US, w.goalMl / 1000f), color = NusaMist,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                if (w.totalMl >= w.goalMl) "Target tercapai. Kerja bagus!" else "${w.goalMl - w.totalMl} ml lagi untuk target hari ini",
                color = NusaMist, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x26FFFFFF))) {
                Box(
                    Modifier
                        .fillMaxWidth(bar)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF66DBFF), Color(0xFF2D6BFF)))),
                )
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillBtn("+${w.bottleMl} ml", { Store.addWater(w.bottleMl) }, filled = true, icon = P_DROP, iconFilled = true)
                PillBtn("+200 ml", { Store.addWater(200) })
                PillBtn("Batal", { Store.undoWater() }, icon = I_UNDO)
            }
        }

        Panel {
            Text("Pengaturan", color = NusaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Stepper("Target harian", "${w.goalMl} ml", { Store.setGoal(w.goalMl - 250) }, { Store.setGoal(w.goalMl + 250) })
            Stepper("Ukuran botol", "${w.bottleMl} ml", { Store.setBottle(w.bottleMl - 50) }, { Store.setBottle(w.bottleMl + 50) })
            Stepper(
                "Ingatkan tiap",
                if (w.remindEveryMin == 0) "Mati" else "${w.remindEveryMin} menit",
                { Store.setRemind(w.remindEveryMin - 15) },
                { Store.setRemind(w.remindEveryMin + 15) },
            )
            Text(
                "Pengingat hanya berbunyi pukul 08.00 sampai 21.59 dan selama notch aktif.",
                fontSize = 12.sp, color = NusaMist, modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (w.today.isNotEmpty()) {
            Panel {
                Text("Catatan hari ini", color = NusaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                w.today.takeLast(8).reversed().forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(fmtClockOf(it.time), color = NusaMist, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("+${it.ml} ml", color = Color(0xFF4FC0FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------- Timer ----------------
@Composable
private fun TimerScreen() {
    val p by Store.pomodoro.collectAsState()
    val col = if (p.focus) Color(0xFFFF4B55) else Color(0xFF1FBF8F)
    val totalMs = (if (p.focus) p.focusMin else p.breakMin) * 60_000L
    val prog = (p.remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

    Page {
        ScreenTitle("Timer fokus", "Pomodoro yang ikut tampil di notch.")
        Panel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Chip(if (p.focus) "Fokus" else "Istirahat")
                Box(Modifier.padding(vertical = 22.dp).size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(230.dp)) {
                        val sw = 12.dp.toPx()
                        val d = size.width - sw
                        drawCircle(col.copy(alpha = 0.10f), size.width * 0.5f)
                        drawArc(col.copy(alpha = 0.22f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw))
                        drawArc(
                            col, -90f, 360f * prog, false, Offset(sw / 2, sw / 2), Size(d, d),
                            style = Stroke(sw, cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            fmtCeil(p.remainingMs), color = NusaInk, fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5f).sp,
                        )
                        Text("Sesi selesai: ${p.cycles}", color = NusaMist, fontSize = 13.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillBtn(
                        if (p.running) "Jeda" else "Mulai", { Pomodoro.toggle() }, filled = true, tint = col,
                        icon = if (p.running) P_PAUSE else P_PLAY, iconFilled = true,
                    )
                    PillBtn("Reset", { Pomodoro.reset() }, icon = P_RESET)
                    PillBtn(
                        if (p.focus) "Istirahat" else "Fokus", { Pomodoro.switchMode() },
                        icon = if (p.focus) P_CUP else P_TARGET,
                    )
                }
            }
        }
        Panel {
            Text("Durasi", color = NusaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Stepper(
                "Fokus", "${p.focusMin} menit",
                { Pomodoro.setDurations(p.focusMin - 5, p.breakMin) },
                { Pomodoro.setDurations(p.focusMin + 5, p.breakMin) },
            )
            Stepper(
                "Istirahat", "${p.breakMin} menit",
                { Pomodoro.setDurations(p.focusMin, p.breakMin - 1) },
                { Pomodoro.setDurations(p.focusMin, p.breakMin + 1) },
            )
        }
    }
}

// ---------------- Atur ----------------
@Composable
private fun SettingsScreen() {
    val ctx = LocalContext.current
    val pet by Store.petEnabled.collectAsState()
    val follow by Store.eyeFollow.collectAsState()
    val shakeOn by Store.shakeEnabled.collectAsState()
    val a11yLive by Store.a11yActive.collectAsState()
    val running by Store.serviceRunning.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    OnResume { tick++ }
    val a11yOk = a11yLive || remember(tick) { a11yEnabledInSettings(ctx) }

    Page {
        ScreenTitle("Atur notch", "Notch bisa dipindah langsung di layar.")
        Panel {
            Text("Pindahkan notch", color = NusaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "Saat notch tertutup, tahan lalu geser ke mana saja di sekeliling layar. Di sudut notch ikut " +
                    "membelok seperti slime, lalu menempel di tepi dan berputar mengikuti arah tepinya. " +
                    "Di tepi bawah bentuknya terbalik tetapi tulisannya tetap tegak.",
                color = NusaMist, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            )
            Row { PillBtn("Kembalikan ke tengah atas", { Store.resetPosition() }, filled = true) }
            if (!running) {
                Text(
                    "Notch belum menyala. Aktifkan di tab Beranda dulu.",
                    color = NusaMist, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        Panel {
            Text("Maskot", color = NusaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            SwitchRow(
                "Maskot bermata",
                "Saat notch sepi, sepasang mata muncul: berkedip, melirik, tidur setelah 20 detik, dan melambai saat kamu kembali.",
                pet,
            ) { Store.setPet(it) }
            SwitchRow(
                "Mata mengikuti sentuhan",
                "Mata menoleh pelan ke arah jari saat kamu menyentuh layar.",
                follow,
            ) { Store.setEyeFollow(it) }
            SwitchRow(
                "Kejutkan maskot",
                "Guncang HP: maskot tiba-tiba muncul dengan ekspresi kaget, pusing, senang, atau memicing.",
                shakeOn,
            ) { Store.setShake(it) }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIcon(Color(0xFF6EE79A), Color(0xFF159A45), size = 44.dp) { Ico(I_SHIELD, 24f, 22f, stroke = 2f) }
                Column(Modifier.weight(1f)) {
                    Text("Notch di atas status bar", color = NusaInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (a11yOk) "Aktif. Notch bisa diketuk di posisi paling atas, juga saat HP berdiri."
                        else "Belum aktif. Tanpa ini status bar menelan ketukan di atas layar saat HP berdiri.",
                        color = NusaMist, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
            }
            if (!a11yOk) {
                Text(
                    "Buka Aksesibilitas, pilih Nusa, lalu nyalakan. Nusa hanya meminjam jenis jendelanya, tidak membaca layar.",
                    color = NusaMist, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
                )
                Row(Modifier.padding(top = 12.dp)) {
                    PillBtn("Buka Aksesibilitas", { safeStart(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, filled = true, icon = I_SHIELD)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NusaInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = NusaMist, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2699FF),
                uncheckedThumbColor = Color(0xFFB8BECC), uncheckedTrackColor = Color(0x33FFFFFF),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
