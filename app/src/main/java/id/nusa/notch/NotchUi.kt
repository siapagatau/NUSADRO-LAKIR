package id.nusa.notch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.Locale

// ======================= Palet (dari situs Nusa) =======================
val NusaVoid = Color(0xFF03040A)
val NusaPanel = Color(0xFF0A0D18)
val NusaInk = Color(0xFFECEFF7)
val NusaMist = Color(0xFF8C94A8)
val NusaLine = Color(0x1AECEFF7)
val NusaCyan = Color(0xFF19B8FF)
val NusaIndigo = Color(0xFF2D5BFF)
val NusaViolet = Color(0xFF7B4BFF)

// cubic-bezier yang sama dengan situs: --spring dan "ease"
private val SpringEase = CubicBezierEasing(0.34f, 1.32f, 0.5f, 1f)
private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

private val White = Color.White
private val Dim = Color(0x80FFFFFF)
private val Green = Color(0xFF33E07A)

@Composable
fun NusaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color(0xFF05060C),
            secondary = NusaCyan,
            background = NusaVoid,
            onBackground = NusaInk,
            surface = NusaPanel,
            onSurface = NusaInk,
            surfaceVariant = Color(0xFF141829),
            onSurfaceVariant = NusaMist,
            secondaryContainer = Color(0xFF1B2140),
            onSecondaryContainer = Color.White,
            outline = NusaLine,
        ),
        content = content,
    )
}

// ======================= Tata letak absolut (koordinat dp seperti di situs) =======================
private class PosData(
    val x: Float, val y: Float, val cx: Boolean, val cy: Boolean, val right: Boolean,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@PosData
}

private fun Modifier.at(
    x: Float, y: Float, cx: Boolean = false, cy: Boolean = false, right: Boolean = false,
): Modifier = this.then(PosData(x, y, cx, cy, right))

@Composable
private fun Abs(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEachIndexed { i, m ->
                val p = placeables[i]
                val d = m.parentData as? PosData
                if (d == null) {
                    p.place(0, 0)
                } else {
                    val px = d.x.dp.roundToPx()
                    val py = d.y.dp.roundToPx()
                    val bx = if (d.right) constraints.maxWidth - px - p.width
                    else px - (if (d.cx) p.width / 2 else 0)
                    val by = py - (if (d.cy) p.height / 2 else 0)
                    p.place(bx, by)
                }
            }
        }
    }
}

@Composable
private fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.focusProperties { canFocus = false }.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

private fun Modifier.fadeEdges(): Modifier =
    this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.22f to Color.Black,
                    0.78f to Color.Black,
                    1f to Color.Transparent,
                ),
                blendMode = BlendMode.DstIn,
            )
        }

@Composable
private fun Txt(
    text: String, x: Float, y: Float, size: Float = 13f, color: Color = White,
    weight: FontWeight = FontWeight.Normal, cx: Boolean = false, cy: Boolean = true,
    right: Boolean = false, maxW: Float = 300f, spacing: Float = 0f,
) {
    Text(
        text, color = color, fontSize = size.sp, fontWeight = weight, maxLines = 1,
        overflow = TextOverflow.Ellipsis, letterSpacing = spacing.sp,
        modifier = Modifier.at(x, y, cx, cy, right).widthIn(max = maxW.dp),
    )
}

// ======================= Ikon (path SVG dari situs) =======================
@Composable
internal fun Ico(
    d: String, vb: Float, w: Float, h: Float = w, color: Color = White,
    stroke: Float = 0f, modifier: Modifier = Modifier,
) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(modifier.size(w.dp, h.dp)) {
        val k = size.width / vb
        withTransform({ scale(k, k, Offset.Zero) }) {
            if (stroke > 0f) {
                drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawPath(path, color)
            }
        }
    }
}

internal const val P_PREV = "M11 6 L3 12 L11 18 Z M21 6 L13 12 L21 18 Z"
internal const val P_NEXT = "M13 6 L21 12 L13 18 Z M3 6 L11 12 L3 18 Z"
internal const val P_PAUSE = "M7 5 h4 v14 H7 Z M13 5 h4 v14 h-4 Z"
internal const val P_PLAY = "M7 4.5 v15 L19 12 Z"
internal const val P_BUBBLE =
    "M4 0.8 h10 a3.2 3.2 0 0 1 3.2 3.2 v4.6 a3.2 3.2 0 0 1 -3.2 3.2 H8.4 L4.6 15 l0.5 -3.2 H4 A3.2 3.2 0 0 1 0.8 8.6 V4 A3.2 3.2 0 0 1 4 0.8 Z"
internal const val P_DROP = "M12 3 C12 3 18 9.5 18 14 a6 6 0 0 1 -12 0 C6 9.5 12 3 12 3 Z"
internal const val P_BELL =
    "M12 3 a6 6 0 0 0 -6 6 v4 l-2 3 h16 l-2 -3 V9 a6 6 0 0 0 -6 -6 Z M10 19 a2 2 0 0 0 4 0 Z"
internal const val P_CUP =
    "M5 9 h11 v5 a5 5 0 0 1 -5 5 H10 a5 5 0 0 1 -5 -5 Z M16 10 h1.5 a2.5 2.5 0 0 1 0 5 H16 M8 3 v3 M12 3 v3"
internal const val P_TARGET =
    "M4 12 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 M8.5 12 a3.5 3.5 0 1 0 7 0 a3.5 3.5 0 1 0 -7 0 M12 12 L19 5"
internal const val P_RESET = "M20 12 a8 8 0 1 1 -2.3 -5.6 M20 4 v4 h-4"
internal const val P_TIMER =
    "M4 13.5 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 M12 13.5 V9 M10 2.5 h4 M12 2.5 v3"
internal const val P_PLUS =
    "M12 2 a10 10 0 1 0 0 20 a10 10 0 1 0 0 -20 Z M13 7 v4 h4 v2 h-4 v4 h-2 v-4 H7 v-2 h4 V7 Z"
internal const val P_EDIT = "M4 20 h7 M14 5 l5 5 l-9 9 H5 v-5 Z"
internal const val P_CHECK = "M2 5.2 L4 7.2 L8 2.8"
internal const val P_BOLT = "M6 0 L0 8 h4 l-1 6 l6 -8 H5 Z"
internal const val P_BT = "M7 7 L17 17 L12 21 V3 L17 7 L7 17"
internal const val P_PHONES =
    "M4 15 v-3 a8 8 0 0 1 16 0 v3 M4 15 h3 v5 H5 a1 1 0 0 1 -1 -1 Z M20 15 h-3 v5 h2 a1 1 0 0 0 1 -1 Z"
internal const val P_CHEV_L = "M6.5 1.5 L3 5 L6.5 8.5"
internal const val P_CHEV_R = "M3.5 1.5 L7 5 L3.5 8.5"

// ======================= Bentuk notch dengan "telinga" cekung =======================
private enum class NotchMode { PET, IDLE, PEEK, MUSIC, LYRICS, LYRICLINE, POMO, WATER, TODO }

private class Metrics(val w: Float, val h: Float, val r: Float, val e: Float)

private fun metricsOf(m: NotchMode): Metrics = when (m) {
    NotchMode.PET, NotchMode.IDLE, NotchMode.LYRICLINE -> Metrics(150f, 30f, 14f, 10f)
    NotchMode.PEEK -> Metrics(310f, 60f, 19f, 13f)
    NotchMode.LYRICS -> Metrics(520f, 140f, 32f, 22f)
    else -> Metrics(420f, 140f, 32f, 22f)
}

private fun DrawScope.drawNotch(w: Dp, r: Dp, e: Dp, petGlow: Float, lyricGlow: Float) {
    val wp = w.toPx()
    val ep = e.toPx()
    val hp = size.height
    val rp = r.toPx().coerceAtMost(minOf(wp, hp) / 2f)
    val tw = wp + 2f * ep
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(tw, 0f)
        arcTo(Rect(tw - ep, 0f, tw + ep, 2f * ep), 270f, -90f, false)
        lineTo(tw - ep, hp - rp)
        arcTo(Rect(tw - ep - 2f * rp, hp - 2f * rp, tw - ep, hp), 0f, 90f, false)
        lineTo(ep + rp, hp)
        arcTo(Rect(ep, hp - 2f * rp, ep + 2f * rp, hp), 90f, 90f, false)
        lineTo(ep, ep)
        arcTo(Rect(-ep, 0f, ep, 2f * ep), 0f, -90f, false)
        close()
    }
    drawPath(path, Color.Black)
    if (petGlow > 0.01f || lyricGlow > 0.01f) {
        clipPath(path) {
            if (petGlow > 0.01f) {
                drawGlow(ep, wp, hp, 0.9f, petGlow, NusaCyan, NusaIndigo, NusaViolet)
            }
            if (lyricGlow > 0.01f) {
                drawGlow(ep, wp, hp, 0.82f, lyricGlow, Color(0xFF2C64B8), Color(0xFF3A7AD0), Color(0xFF2A5AA8))
            }
        }
    }
}

private fun DrawScope.drawGlow(
    ep: Float, wp: Float, hp: Float, frac: Float, alpha: Float, c1: Color, c2: Color, c3: Color,
) {
    val top = hp * (1f - frac)
    val bounds = Rect(ep, top, ep + wp, hp)
    drawContext.canvas.saveLayer(bounds, Paint())
    drawRect(
        brush = Brush.horizontalGradient(0f to c1, 0.55f to c2, 1f to c3, startX = ep, endX = ep + wp),
        topLeft = Offset(ep, top),
        size = Size(wp, hp - top),
        alpha = alpha,
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.3f to Color(0x1F000000),
            0.7f to Color(0x8C000000),
            1f to Color.Black,
            startY = top,
            endY = hp,
        ),
        topLeft = Offset(ep, top),
        size = Size(wp, hp - top),
        blendMode = BlendMode.DstIn,
    )
    drawContext.canvas.restore()
}

// ======================= Maskot (mata) =======================
/**
 * Sepasang mata. Kedip dan napas digerakkan coroutine (hanya beranimasi saat benar-benar bergerak),
 * bukan transisi tak berujung, jadi overlay tidak merender 60 fps terus-menerus.
 */
@Composable
private fun Eyes(
    w: Float, h: Float, gap: Float, sleeping: Boolean = false, greetKey: Int = 0,
    gazeFlow: StateFlow<Gaze?> = Store.gaze, reachX: Float = 22f, reachY: Float = 6f,
    expr: Expr = Expr.NORMAL,
) {
    val blink = remember { Animatable(1f) }
    val breath = remember { Animatable(0f) }
    var glance by remember { mutableFloatStateOf(0f) }

    // kedip acak tiap 3-6 detik, kadang dua kali berturut-turut
    LaunchedEffect(sleeping) {
        if (sleeping) {
            blink.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            delay(2800L + (Math.random() * 2800).toLong())
            blink.animateTo(0.1f, tween(110))
            blink.animateTo(1f, tween(140))
            if (Math.random() < 0.2) {
                delay(130)
                blink.animateTo(0.1f, tween(100))
                blink.animateTo(1f, tween(130))
            }
        }
    }
    // napas pelan saat tidur (berhenti setelah beberapa siklus supaya hemat baterai)
    LaunchedEffect(sleeping) {
        if (!sleeping) {
            breath.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(8) {
            breath.animateTo(1f, tween(1800, easing = EaseInOut))
            breath.animateTo(0f, tween(1800, easing = EaseInOut))
        }
    }
    // melirik ke samping sesekali saat terjaga
    LaunchedEffect(sleeping) {
        if (sleeping) {
            glance = 0f
            return@LaunchedEffect
        }
        while (true) {
            delay(2500L + (Math.random() * 4500).toLong())
            glance = ((Math.random() * 2.0 - 1.0) * 0.7).toFloat()
            delay(900L + (Math.random() * 900).toLong())
            glance = 0f
        }
    }
    // kelopak turun pelan saat mulai tidur, terbuka cepat saat bangun
    val closed by animateFloatAsState(
        if (sleeping) 1f else 0f,
        tween(if (sleeping) 1400 else 220, easing = CssEase),
        label = "lid",
    )
    val pop = remember { Animatable(1f) }
    val rot = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    // bangun lalu "melambai": mata membuka dengan pantulan, bergoyang kiri-kanan
    LaunchedEffect(greetKey) {
        if (greetKey == 0) return@LaunchedEffect
        pop.snapTo(0.25f)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 450f)) }
        launch {
            hop.animateTo(3f, tween(140))
            hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 400f))
        }
        rot.animateTo(-16f, tween(140))
        rot.animateTo(16f, tween(220))
        rot.animateTo(-12f, tween(200))
        rot.animateTo(12f, tween(200))
        rot.animateTo(0f, tween(160))
    }
    // ---- mata mengikuti jari / kursor ----
    val follow by Store.eyeFollow.collectAsState()
    val gz by gazeFlow.collectAsState()
    val dens = LocalDensity.current.density
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var shift by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(gz, follow, sleeping) {
        val g = gz
        val c = coords
        if (g == null || !follow || sleeping || c == null || !c.isAttached) {
            shift = Offset.Zero
            return@LaunchedEffect
        }
        val mid = c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)
        val rx = g.x - mid.x
        val ry = g.y - mid.y
        // jendela bisa diputar (notch di sisi layar): putar balik arah tatapan ke bingkai isi
        val rad = Math.toRadians(Store.pose.value.contentDeg.toDouble())
        val dx = (rx * cos(rad) + ry * sin(rad)).toFloat()
        val dy = (-rx * sin(rad) + ry * cos(rad)).toFloat()
        // geser jenuh pelan-pelan: dekat = bergerak jelas, jauh = tidak melewati batas
        shift = Offset(
            dx / (abs(dx) + 90f * dens) * reachX * dens,
            dy / (abs(dy) + 40f * dens) * reachY * dens,
        )
        delay(1800)
        shift = Offset.Zero
    }
    // pegas teredam kritis & lembut: berpindah pelan dan halus, tanpa memantul
    val lookX by animateFloatAsState(
        shift.x + (if (shift == Offset.Zero) glance * reachX * 0.5f * dens else 0f),
        spring(dampingRatio = 1f, stiffness = 70f), label = "lookx",
    )
    val lookY by animateFloatAsState(shift.y, spring(dampingRatio = 1f, stiffness = 70f), label = "looky")

    Box(Modifier.onGloballyPositioned { coords = it }) {
        if (expr != Expr.NORMAL) {
            ExprEyes(expr, w, h, gap)
        } else {
            Row(
                modifier = Modifier.graphicsLayer {
                    rotationZ = rot.value
                    translationX = lookX
                    translationY = (-hop.value + closed * 1.2f * breath.value) * density + lookY
                },
                horizontalArrangement = Arrangement.spacedBy(gap.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(2) {
                    Box(
                        Modifier
                            .size(w.dp, h.dp)
                            .graphicsLayer {
                                val open = 1f - closed
                                val sleepY = 0.09f + 0.07f * breath.value
                                scaleY = (blink.value * open + sleepY * closed) * pop.value
                            }
                            .drawBehind {
                                val g = 5.dp.toPx()
                                drawOval(Color(0x3350A0FF), Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g))
                                drawOval(Color.White)
                            }
                    )
                }
            }
        }
    }
}

/**
 * Ekspresi saat HP diguncang. Hanya hidup selama ekspresi tampil, jadi animasi tak berujungnya
 * tidak jalan terus di latar.
 */
@Composable
private fun ExprEyes(expr: Expr, w: Float, h: Float, gap: Float) {
    val tr = rememberInfiniteTransition(label = "expr")
    val t = tr.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "et",
    )
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(expr) {
        pop.snapTo(0.3f)
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }
    Canvas(
        Modifier
            .size((2f * w + gap).dp, 24.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
    ) {
        val u = 1.dp.toPx()
        val cy = size.height / 2f
        val xs = floatArrayOf(w * u / 2f, (w + gap + w / 2f) * u)
        val phase = t.value * 2f * Math.PI.toFloat()
        when (expr) {
            Expr.SHOCK -> {
                // mata melebar dengan pupil kecil, sedikit gemetar
                val shake = sin(phase * 6f) * 0.9f * u
                for (cx in xs) {
                    val ew = w * 1.3f * u
                    val eh = h * 1.45f * u
                    drawOval(Color(0x3350A0FF), Offset(cx - ew / 2 - 3 * u + shake, cy - eh / 2 - 3 * u), Size(ew + 6 * u, eh + 6 * u))
                    drawOval(Color.White, Offset(cx - ew / 2 + shake, cy - eh / 2), Size(ew, eh))
                    drawCircle(Color(0xFF0B0D14), 1.9f * u, Offset(cx + shake, cy))
                }
            }
            Expr.DIZZY -> {
                // mata berputar (spiral), badan oleng
                val sway = sin(phase * 2f) * 1.6f * u
                for ((i, cx) in xs.withIndex()) {
                    val path = Path()
                    val rMax = 5.4f * u
                    val n = 42
                    val dir = if (i == 0) 1f else -1f
                    for (k in 0..n) {
                        val f = k / n.toFloat()
                        val a = f * 3f * Math.PI.toFloat() * 1.0f + dir * phase * 2f
                        val x = cx + sway + rMax * f * cos(a)
                        val y = cy + rMax * f * sin(a)
                        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, Color.White, style = Stroke(1.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Expr.HAPPY -> {
                // mata tersenyum ^ ^ sambil memantul
                val bounce = -abs(sin(phase * 2f)) * 1.8f * u
                for (cx in xs) {
                    drawArc(
                        Color.White, 180f, 180f, false,
                        Offset(cx - 4.6f * u, cy - 1.5f * u + bounce), Size(9.2f * u, 9.2f * u),
                        style = Stroke(2.3f * u, cap = StrokeCap.Round),
                    )
                }
            }
            else -> {
                // mata memicing > <
                val jig = sin(phase * 5f) * 0.6f * u
                val l = Path().apply {
                    moveTo(xs[0] - 3.2f * u + jig, cy - 4.2f * u); lineTo(xs[0] + 3f * u + jig, cy); lineTo(xs[0] - 3.2f * u + jig, cy + 4.2f * u)
                }
                val r = Path().apply {
                    moveTo(xs[1] + 3.2f * u - jig, cy - 4.2f * u); lineTo(xs[1] - 3f * u - jig, cy); lineTo(xs[1] + 3.2f * u - jig, cy + 4.2f * u)
                }
                val st = Stroke(2.3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(l, Color.White, style = st)
                drawPath(r, Color.White, style = st)
            }
        }
    }
}

/** Lapisan maskot di notch tertutup: mata, tidur (zZz) saat diam, sapa saat kembali dipakai. */
@Composable
private fun PetLayer(asleep: Boolean, greet: Int, expr: Expr = Expr.NORMAL) {
    // zZz hanya tampil 30 detik pertama setelah tidur, supaya animasinya tidak jalan selamanya
    var showZ by remember { mutableStateOf(false) }
    LaunchedEffect(asleep) {
        if (asleep) {
            showZ = true
            delay(30_000)
            showZ = false
        } else {
            showZ = false
        }
    }
    val zOn = asleep && showZ
    val za by animateFloatAsState(if (zOn) 1f else 0f, tween(if (zOn) 900 else 150), label = "zzz")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.Center)) { Eyes(8.4f, 12.4f, 15f, asleep, greet, Store.gaze, 24f, 6f, expr) }
        if (za > 0.01f) Zzz(za, Modifier.align(Alignment.TopStart).offset(x = 100.dp, y = 0.dp))
    }
}

@Composable
private fun Zzz(master: Float, modifier: Modifier) {
    val tr = rememberInfiniteTransition(label = "zzz")
    val t by tr.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "zt",
    )
    Box(modifier.size(46.dp, 30.dp)) {
        listOf(0f to 7f, 0.33f to 9f, 0.66f to 11f).forEach { (phase, sz) ->
            val p = (t + phase) % 1f
            val fade = if (p < 0.2f) p / 0.2f else if (p > 0.75f) (1f - p) / 0.25f else 1f
            val a = fade.coerceIn(0f, 1f) * master * 0.85f
            Text(
                if (sz >= 11f) "Z" else "z",
                color = Color(0xFF9CC8FF),
                fontSize = sz.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .offset(x = (p * 22f).dp, y = (22f - p * 20f).dp)
                    .graphicsLayer { alpha = a },
            )
        }
    }
}

// ======================= Komponen kecil =======================
@Composable
private fun MiniRing(level: Int, modifier: Modifier) {
    Canvas(modifier.size(15.dp)) {
        val sw = 2.6f.dp.toPx()
        val d = size.width - sw
        drawArc(Green.copy(alpha = 0.28f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw))
        drawArc(
            Green, -90f, 360f * (level / 100f).coerceIn(0f, 1f), false,
            Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun Bars(active: Boolean, modifier: Modifier) {
    if (active) BarsLive(modifier) else BarsStill(modifier)
}

@Composable
private fun BarsStill(modifier: Modifier) {
    Row(
        modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) {
            Box(
                Modifier
                    .width(2.2f.dp)
                    .height(4.2f.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x99FFFFFF))
            )
        }
    }
}

@Composable
private fun BarsLive(modifier: Modifier) {
    val tr = rememberInfiniteTransition(label = "eq")
    val offs = listOf(0, 300, 600, 150, 450)
    Row(
        modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        offs.forEach { o ->
            val f by tr.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(900, easing = EaseInOut),
                    RepeatMode.Reverse,
                    initialStartOffset = StartOffset(o, StartOffsetType.FastForward),
                ),
                label = "bar",
            )
            Box(
                Modifier
                    .width(2.2f.dp)
                    .height((14f * f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(White)
            )
        }
    }
}

@Composable
private fun Art(bm: Bitmap?, modifier: Modifier) {
    Box(
        modifier
            .size(62.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFCFE2FB))))
    ) {
        if (bm != null) {
            Image(
                bitmap = bm.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF2E7FE0), size.minDimension * 0.22f, center)
                drawCircle(
                    Color(0xFF5AA6F2), size.minDimension * 0.33f, center,
                    style = Stroke(1.5f.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun CircleBtn(x: Float, y: Float, d: Float, bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.at(x, y, cx = true, cy = true).size(d.dp).clip(CircleShape).background(bg).tap(onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Ctl(x: Float, y: Float, path: String, onClick: () -> Unit) {
    Box(Modifier.at(x, y, cx = true, cy = true).size(40.dp).tap(onClick), contentAlignment = Alignment.Center) {
        Ico(path, 24f, 17f)
    }
}

@Composable
private fun Pill(x: Float, y: Float, w: Float, h: Float, bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier.at(x, y).width(w.dp).height(h.dp).clip(RoundedCornerShape((h / 2).dp)).background(bg).tap(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private fun openApp(ctx: Context, tab: Int) {
    val i = Intent(ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra("tab", tab)
    try { ctx.startActivity(i) } catch (e: Exception) { }
}

@Composable
private fun rememberClock(fmtStr: String): String {
    fun now() = java.text.SimpleDateFormat(fmtStr, Locale.getDefault()).format(java.util.Date())
    var t by remember(fmtStr) { mutableStateOf(now()) }
    LaunchedEffect(fmtStr) {
        while (true) {
            t = now()
            delay(10_000)
        }
    }
    return t
}

@Composable
private fun rememberPosition(m: MediaInfo?): Long {
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(m) {
        while (true) {
            pos = m?.currentPosition() ?: 0L
            delay(300)
        }
    }
    return pos
}

// ======================= Notch =======================
/** Overlay selalu digambar kiri-ke-kanan (koordinatnya absolut), apa pun bahasa perangkat. */
@Composable
fun Notch() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { NotchBody() }
}

/** Isi notch dipotong mengikuti bentuk badan (sudut bawah membulat dalam bingkai notch, yang ikut berputar). */
private fun bodyShapeOf(rDp: Dp): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { rDp.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        return Outline.Rounded(
            RoundRect(
                0f, 0f, size.width, size.height,
                topLeftCornerRadius = CornerRadius.Zero,
                topRightCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius(r),
                bottomLeftCornerRadius = CornerRadius(r),
            ),
        )
    }
}

@Composable
private fun NotchBody() {
    val ctx = LocalContext.current
    val screenW by Store.screenWdp.collectAsState()
    val screenH by Store.screenHdp.collectAsState()
    val petEnabled by Store.petEnabled.collectAsState()
    val media by Store.media.collectAsState()
    val alert by Store.alert.collectAsState()
    val pomo by Store.pomodoro.collectAsState()
    // Hanya sisi tempel yang dibaca di komposisi; sudut/regangan per frame dibaca di layout & gambar saja.
    val edge by remember { derivedStateOf { Store.pose.value.edge } }

    var open by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
    var lyricsOn by remember { mutableStateOf(false) }
    // Lirik disematkan: tetap tampil satu baris saat notch ditutup (sampai bubble lirik dimatikan)
    var lyricPin by remember { mutableStateOf(false) }
    var touch by remember { mutableLongStateOf(0L) }
    var held by remember { mutableStateOf<Alert?>(null) }
    var greet by remember { mutableIntStateOf(0) }

    LaunchedEffect(alert) { if (alert != null) held = alert }
    LaunchedEffect(Unit) {
        Store.collapse.collect {
            if (!Store.editing.value) {
                open = false
                lyricsOn = false
            }
        }
    }
    LaunchedEffect(open) { Store.panelOpen.value = open }
    LaunchedEffect(open, touch) {
        if (open) {
            delay(12_000)
            if (!Store.editing.value) {
                open = false
                lyricsOn = false
            }
        }
    }
    LaunchedEffect(open) { if (!open) Store.editing.value = false }

    // Guncangan: maskot muncul tiba-tiba dengan ekspresi, lalu kembali ke keadaan semula.
    var shaking by remember { mutableStateOf(false) }
    var expr by remember { mutableStateOf(Expr.NORMAL) }
    var shakeKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { Store.shake.collect { shakeKey += 1 } }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0 || !Store.petEnabled.value || open) return@LaunchedEffect
        expr = listOf(Expr.SHOCK, Expr.DIZZY, Expr.HAPPY, Expr.SQUINT).random()
        shaking = true
        greet += 1
        delay(3800)
        shaking = false
        expr = Expr.NORMAL
    }

    // Ekspresi dari gerakan notch: dipegang/digeser -> kaget, ditarik kuat -> memicing, dilepas -> pusing.
    val dragPhase by Store.dragPhase.collectAsState()
    val settled by Store.settleCount.collectAsState()
    var dizzy by remember { mutableStateOf(false) }
    LaunchedEffect(settled) {
        if (settled == 0 || !Store.petEnabled.value) return@LaunchedEffect
        dizzy = true
        delay(1900)
        dizzy = false
    }
    val pulling by remember { derivedStateOf { Store.pose.value.pull > 0.35f } }
    var dancing by remember { mutableStateOf(false) }
    val exprNow = when {
        shaking -> expr
        dragPhase == 1 -> if (pulling) Expr.SQUINT else Expr.SHOCK
        dragPhase == 2 || dizzy -> Expr.DIZZY
        dancing -> Expr.HAPPY
        else -> Expr.NORMAL
    }
    val showExpr = petEnabled && exprNow != Expr.NORMAL
    val playing = media?.playing == true
    val quiet = !open && alert == null && !playing && !pomo.running
    // Maskot: selalu muncul begitu notch sepi (tanpa menunggu), tidur setelah 20 detik tanpa aktivitas,
    // dan bangun lalu melambai saat ada sentuhan, layar menyala, atau kunci dibuka.
    val resting = quiet && petEnabled
    var lastAct by remember { mutableLongStateOf(System.nanoTime()) }
    var asleep by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { Store.activity.collect { lastAct = System.nanoTime() } }
    LaunchedEffect(resting, lastAct) {
        if (!resting) {
            asleep = false
            return@LaunchedEffect
        }
        if (asleep) {
            asleep = false
            greet += 1
        }
        delay(20_000)
        asleep = true
    }

    // ---- ukuran notch lirik mengikuti panjang lirik aktif ----
    val edgeNow = edge
    val alongDp0 = if (edgeNow == 0 || edgeNow == 2) screenW else screenH
    val normalDp0 = if (edgeNow == 0 || edgeNow == 2) screenH else screenW
    val lyricsState by Store.lyrics.collectAsState()
    val found = lyricsState as? LyricsState.Found
    var lyricIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(lyricPin, open, lyricsOn, playing, found) {
        if (found == null || !(lyricPin || (open && lyricsOn))) return@LaunchedEffect
        while (true) {
            val p = media?.currentPosition() ?: 0L
            val i = found.lines.indexOfLast { it.timeMs <= p + 250 }.coerceAtLeast(0)
            if (i != lyricIdx) lyricIdx = i
            delay(250)
        }
    }
    val lyricMini = !open && lyricPin && playing && found != null
    val tmeasure = rememberTextMeasurer()
    val dens = LocalDensity.current
    val activeText = found?.lines?.getOrNull(lyricIdx)?.text ?: ""
    val wideOk = alongDp0 >= 570f
    val lyW = if (wideOk) 520f else (alongDp0 - 8f - 44f).coerceIn(280f, 520f)
    val lyTextW = if (wideOk) 234f else lyW - 36f
    val lyLines = remember(activeText, lyTextW) {
        tmeasure.measure(
            AnnotatedString(activeText.ifEmpty { " " }), LyricStyle,
            constraints = Constraints(maxWidth = (lyTextW * dens.density).roundToInt()),
        ).lineCount.coerceIn(1, 6)
    }
    val lyH = (
        if (wideOk) lyLines * 20f + 100f else lyLines * 20f + 9f + 2f * 34f + 96f
        ).coerceIn(if (wideOk) 140f else 190f, 340f).coerceAtMost(normalDp0 - 16f).coerceAtLeast(120f)
    // Lirik satu baris saat notch tertutup: selebar teksnya (sampai lebar layar dikurangi telinga notch);
    // bila masih lebih panjang, teks dibungkus ke beberapa baris dan notch bertambah tinggi (tidak dipotong "...").
    val lineMaxW = (alongDp0 - 8f - 20f).coerceAtLeast(150f)
    val lineOneW = remember(activeText) {
        val px = tmeasure.measure(
            AnnotatedString(activeText.ifEmpty { " " }),
            TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp), maxLines = 1, softWrap = false,
        ).size.width
        px / dens.density
    }
    val lineW = (lineOneW + 52f).coerceIn(150f, lineMaxW)
    val lineRows = remember(activeText, lineW) {
        tmeasure.measure(
            AnnotatedString(activeText.ifEmpty { " " }),
            TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp, letterSpacing = 0.sp),
            constraints = Constraints(maxWidth = ((lineW - 28f) * dens.density).roundToInt().coerceAtLeast(1)),
        ).lineCount.coerceIn(1, 4)
    }
    val lineH = (lineRows * 17f + 13f).coerceAtMost((normalDp0 - 16f).coerceAtLeast(30f))

    val pages = listOf(NotchMode.MUSIC, NotchMode.POMO, NotchMode.WATER, NotchMode.TODO)
    val mode = when {
        open && page == 0 && lyricsOn -> NotchMode.LYRICS
        open -> pages[page]
        showExpr -> NotchMode.PET
        alert != null -> NotchMode.PEEK
        lyricMini -> NotchMode.LYRICLINE
        resting -> NotchMode.PET
        else -> NotchMode.IDLE
    }
    val mt = when (mode) {
        NotchMode.LYRICS -> Metrics(lyW, lyH, 32f, 22f)
        NotchMode.LYRICLINE -> Metrics(lineW, lineH, if (lineRows > 1) 18f else 14f, 10f)
        else -> metricsOf(mode)
    }

    // Timing persis seperti CSS situs: lebar/tinggi .62s spring, radius dan telinga .45s ease.
    // Saat membesar, animasi menunggu ~3 frame supaya jendela overlay sempat melebar lebih dulu.
    val lastArea = remember { FloatArray(1) }
    val area = (mt.w + 2f * mt.e) * mt.h
    val growDelay = if (area > lastArea[0] + 1f) 45 else 0
    SideEffect { lastArea[0] = area }
    val sizeSpec = tween<Dp>(620, growDelay, SpringEase)
    val wS = animateDpAsState(mt.w.dp, sizeSpec, label = "w")
    val hS = animateDpAsState(mt.h.dp, sizeSpec, label = "h")
    val rS = animateDpAsState(mt.r.dp, tween(450, easing = CssEase), label = "r")
    val eS = animateDpAsState(mt.e.dp, tween(450, easing = CssEase), label = "e")

    // Skala muat-layar lewat graphicsLayer (bukan mengubah kepadatan), jadi teks tidak diukur ulang tiap frame.
    val alongDp = if (edge == 0 || edge == 2) screenW else screenH
    val normalDp = if (edge == 0 || edge == 2) screenH else screenW
    val scTarget = minOf(1f, (alongDp - 8f) / (mt.w + 2f * mt.e), (normalDp - 8f) / mt.h).coerceAtLeast(0.3f)
    val scS = animateFloatAsState(scTarget, tween(620, growDelay, SpringEase), label = "sc")
    LaunchedEffect(mode, scTarget, edge, mt.w, mt.h) {
        Store.notchBox.value = BoxDp((mt.w + 2f * mt.e) * scTarget, mt.h * scTarget)
    }

    // Lapisan: keluar .2s ease, masuk .32s ease setelah jeda .2s
    fun fadeSpec(on: Boolean) =
        if (on) tween<Float>(320, 200, CssEase) else tween<Float>(200, easing = CssEase)
    val petOn = mode == NotchMode.PET
    val lyricOn = mode == NotchMode.LYRICS
    val isPage = mode == NotchMode.MUSIC || mode == NotchMode.POMO ||
        mode == NotchMode.WATER || mode == NotchMode.TODO
    val petGlow = animateFloatAsState(
        if (!petOn) 0f else if (asleep) 0.5f else 1f,
        if (petOn && asleep) tween(1400, easing = CssEase) else fadeSpec(petOn),
        label = "pg",
    )
    val lyricGlow = animateFloatAsState(if (lyricOn) 0.95f else 0f, fadeSpec(lyricOn), label = "lg")
    val chromeA = animateFloatAsState(if (isPage) 1f else 0f, fadeSpec(isPage), label = "chrome")
    val chromeVisible by remember { derivedStateOf { chromeA.value > 0.01f } }
    // Bermain sendiri saat tidak diapa-apakan: memantul (squash & stretch) atau berjoged kanan-kiri.
    val playSq = remember { Animatable(0f) }
    val playRot = remember { Animatable(0f) }
    val playX = remember { Animatable(0f) }
    val canPlay = mode == NotchMode.PET && !asleep && !open && dragPhase == 0 && (!showExpr || dancing)
    LaunchedEffect(canPlay) {
        if (!canPlay) {
            playSq.snapTo(0f); playRot.snapTo(0f); playX.snapTo(0f)
            dancing = false
            return@LaunchedEffect
        }
        while (true) {
            delay(4500L + (Math.random() * 6500).toLong())
            if (Math.random() < 0.5) {
                repeat(3) {
                    playSq.animateTo(-0.3f, tween(110, easing = CssEase))
                    playSq.animateTo(0.32f, tween(150, easing = CssEase))
                    playSq.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 520f))
                    delay(60)
                }
            } else {
                dancing = true
                kotlinx.coroutines.coroutineScope {
                    launch {
                        repeat(4) {
                            playRot.animateTo(5f, tween(210, easing = EaseInOut))
                            playRot.animateTo(-5f, tween(210, easing = EaseInOut))
                        }
                        playRot.animateTo(0f, tween(180))
                    }
                    launch {
                        repeat(4) {
                            playX.animateTo(6f, tween(210, easing = EaseInOut))
                            playX.animateTo(-6f, tween(210, easing = EaseInOut))
                        }
                        playX.animateTo(0f, tween(180))
                    }
                }
                dancing = false
            }
        }
    }
    val dropY = remember { Animatable(-1.1f) }
    LaunchedEffect(Unit) {
        delay(200)
        dropY.animateTo(0f, tween(900, easing = SpringEase))
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(open) {
                // ketukan di tepi transparan: tertutup -> buka, terbuka -> tutup
                detectTapGestures {
                    if (open) {
                        // sedang mengetik (keyboard tampil): ketukan di area kosong tidak menutup notch
                        if (!Store.editing.value) {
                            open = false
                            lyricsOn = false
                        }
                    } else {
                        open = true
                        page = 0
                        lyricsOn = lyricPin && found != null
                    }
                }
            },
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    Modifier
                        .layout { m, _ ->
                            val uw = (wS.value + eS.value * 2f).roundToPx()
                            val uh = hS.value.roundToPx()
                            val pl = m.measure(Constraints.fixed(uw, uh))
                            layout(uw, uh) { pl.place(0, 0) }
                        }
                        .graphicsLayer {
                            val ps = Store.pose.value
                            val k = scS.value
                            val sq = playSq.value
                            val sxT = ps.sx * (1f - 0.5f * sq)
                            val syT = ps.sy * (1f + sq)
                            rotationZ = ps.shapeDeg + playRot.value
                            scaleX = k * sxT
                            scaleY = k * syT
                            // vektor geser dalam sumbu notch: ke sisi tempel (dropY), joget ke samping, dan
                            // pergeseran ke dalam supaya sisi tempel tetap menempel saat badan meregang
                            val lx = playX.value * density
                            val ly = dropY.value * size.height + (syT - 1f) * size.height * k / 2f
                            val rad = Math.toRadians(ps.shapeDeg.toDouble())
                            val c = cos(rad).toFloat()
                            val sn = sin(rad).toFloat()
                            translationX = lx * c - ly * sn
                            translationY = lx * sn + ly * c
                        }
                        .drawBehind { drawNotch(wS.value, rS.value, eS.value, petGlow.value, lyricGlow.value) }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    touch = System.nanoTime()
                                    Store.activity.tryEmit(Unit)
                                }
                            }
                        },
                ) {
                    Box(
                        Modifier
                            .offset { IntOffset(eS.value.roundToPx(), 0) }
                            .layout { m, _ ->
                                val bw = wS.value.roundToPx()
                                val bh = hS.value.roundToPx()
                                val pl = m.measure(Constraints.fixed(bw, bh))
                                layout(bw, bh) { pl.place(0, 0) }
                            }
                            .graphicsLayer {
                                clip = true
                                shape = bodyShapeOf(rS.value)
                            }
                            .then(
                                if (!open) Modifier.tap {
                                    open = true
                                    page = 0
                                    lyricsOn = lyricPin && found != null
                                } else Modifier,
                            ),
                    ) {
                        // Lapisan isi: diputar balik terhadap bingkai notch supaya teks tetap terbaca
                        // (di tepi bawah bentuk notch terbalik, teksnya tetap tegak).
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val ps = Store.pose.value
                                    var rel = ps.contentDeg - ps.shapeDeg
                                    while (rel > 180f) rel -= 360f
                                    while (rel <= -180f) rel += 360f
                                    rotationZ = rel
                                    // di sudut (isi dan bentuk berputar berlawanan) isi memudar sebentar
                                    val off = minOf(abs(rel), 180f - abs(rel))
                                    alpha = (1f - off / 40f).coerceIn(0f, 1f)
                                }
                                .pointerInput(open) {
                                    if (open) {
                                        var drag = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { drag = 0f },
                                            onDragEnd = {
                                                if (drag < -60f && page < 3) { page += 1; lyricsOn = false }
                                                else if (drag > 60f && page > 0) { page -= 1; lyricsOn = false }
                                            },
                                            onDragCancel = { },
                                            onHorizontalDrag = { _, dx -> drag += dx },
                                        )
                                    }
                                },
                        ) {
                            val pa = alert ?: held
                            if (chromeVisible) Chrome(page, chromeA)
                            AnimatedContent(
                                targetState = mode,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    ContentTransform(
                                        targetContentEnter = fadeIn(tween(320, 200, CssEase)),
                                        initialContentExit = fadeOut(tween(200, easing = CssEase)),
                                        sizeTransform = null,
                                    )
                                },
                                label = "layer",
                            ) { m ->
                                val goPrev: () -> Unit = {
                                    if (page > 0) {
                                        page -= 1
                                        lyricsOn = false
                                    }
                                }
                                val goNext: () -> Unit = {
                                    if (page < 3) {
                                        page += 1
                                        lyricsOn = false
                                    }
                                }
                                when (m) {
                                    NotchMode.PET -> PetLayer(asleep && exprNow == Expr.NORMAL, greet, if (petEnabled) exprNow else Expr.NORMAL)
                                    NotchMode.IDLE -> IdleLayer()
                                    NotchMode.PEEK -> PeekOrNothing(pa)
                                    NotchMode.MUSIC -> MusicLayer(false, { lyricsOn = true; lyricPin = true }, goNext, ctx)
                                    NotchMode.LYRICS -> {
                                        // tinggi isi mengikuti tinggi panel yang beranimasi (bukan langsung ke tinggi akhir),
                                        // supaya seek bar, kontrol, dan kolom lirik tidak loncat saat jumlah baris berubah
                                        val phA = hS.value.value.coerceIn(minOf(120f, lyH), maxOf(lyH, 120f))
                                        MusicLayer(true, { lyricsOn = false; lyricPin = false }, goNext, ctx, lyW, phA)
                                    }
                                    NotchMode.LYRICLINE -> LyricLine(activeText)
                                    NotchMode.POMO -> PomoLayer(page, goPrev, goNext)
                                    NotchMode.WATER -> WaterLayer(page, goPrev, goNext, ctx)
                                    NotchMode.TODO -> TodoLayer(page, goPrev, ctx)
                                }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            // Jendela visual selalu layar penuh. Digeser -> notch di titik pose; diam -> menempel di tepi dan
            // terpusat searah tepi di posisi [Pose.along] (dianimasikan service, searah dengan pose).
            val wPx = constraints.maxWidth
            val hPx = constraints.maxHeight
            val pl = measurables[0].measure(Constraints())
            val ps = Store.pose.value
            val k = scS.value
            val vw = pl.width * k
            val vh = pl.height * k
            val moving = ps.moving
            val sideways = !moving && (ps.edge == 1 || ps.edge == 3)
            val bw = if (sideways) vh else vw
            val bh = if (sideways) vw else vh
            // Semua koordinat pose = koordinat layar. Jendela bisa saja digeser sistem dari (0,0): kurangi
            // posisi jendela supaya notch tetap rapat ke tepi layar sungguhan (bukan tepi jendela).
            val ox = Store.winOffX.value.toFloat()
            val oy = Store.winOffY.value.toFloat()
            val scrWf = if (ps.fw > 0) ps.fw.toFloat() else wPx + ox
            val scrHf = if (ps.fh > 0) ps.fh.toFloat() else hPx + oy
            val cx: Float
            val cy: Float
            if (moving) {
                cx = ps.cx - ox
                cy = ps.cy - oy
            } else {
                val al = ps.along
                when (ps.edge) {
                    0 -> { cx = al - ox; cy = bh / 2f - oy }
                    2 -> { cx = al - ox; cy = scrHf - bh / 2f - oy }
                    1 -> { cx = scrWf - bw / 2f - ox; cy = al - oy }
                    else -> { cx = bw / 2f - ox; cy = al - oy }
                }
            }
            layout(wPx, hPx) {
                pl.place((cx - pl.width / 2f).roundToInt(), (cy - pl.height / 2f).roundToInt())
            }
        }
    }
}

// ----------------------- Tertutup -----------------------
@Composable
private fun IdleLayer() {
    val battery by Store.battery.collectAsState()
    val media by Store.media.collectAsState()
    val pomo by Store.pomodoro.collectAsState()
    val clock = rememberClock("HH:mm")
    Abs(Modifier.fillMaxSize()) {
        MiniRing(battery, Modifier.at(20f, 15f, true, true))
        Txt(
            if (pomo.running) fmtCeil(pomo.remainingMs) else clock,
            75f, 15f, size = 16f, weight = FontWeight.Bold, cx = true,
            color = if (pomo.running) Color(0xFFFF6B7A) else White,
        )
        Bars(media?.playing == true, Modifier.at(131f, 15f, true, true))
    }
}

// ----------------------- Peek (charger / headphone / air) -----------------------
@Composable
private fun PeekOrNothing(a: Alert?) {
    if (a != null) PeekLayer(a)
}

@Composable
private fun PeekLayer(a: Alert) {
    val p = a.value.removeSuffix("%").toIntOrNull()?.let { it / 100f } ?: 1f
    val col = when (a.kind) {
        AlertKind.CHARGE, AlertKind.UNPLUG, AlertKind.GOAL -> Green
        AlertKind.LOW -> Color(0xFFFF5A5A)
        AlertKind.HEADPHONE -> Color(0xFF5EC2FF)
        AlertKind.WATER -> Color(0xFF4FC0FF)
        AlertKind.POMO -> Color(0xFFFF5A6E)
    }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(a) { target = p }
    val ap by animateFloatAsState(
        target, tween(1000, 350, CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)), label = "peek",
    )

    Abs(Modifier.fillMaxSize()) {
        Canvas(Modifier.at(35f, 30f, true, true).size(30.dp)) {
            val sw = size.width * 5f / 30f
            val d = size.width - sw
            drawArc(col.copy(alpha = 0.28f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw))
            drawArc(
                col, -90f, 360f * ap, false, Offset(sw / 2, sw / 2), Size(d, d),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Box(Modifier.at(35f, 30f, true, true).size(14.dp), contentAlignment = Alignment.Center) {
            when (a.kind) {
                AlertKind.CHARGE, AlertKind.UNPLUG, AlertKind.LOW -> Ico(P_BOLT, 10f, 11f, 13f)
                AlertKind.HEADPHONE -> Ico(P_BT, 24f, 12f, stroke = 2.4f)
                AlertKind.WATER, AlertKind.GOAL -> Ico(P_DROP, 24f, 12f)
                AlertKind.POMO -> Ico(P_TIMER, 24f, 14f, stroke = 2f)
            }
        }
        Txt(a.title, 64f, 22f, size = 15f, weight = FontWeight.SemiBold, maxW = 170f)
        Txt(a.sub, 64f, 40f, size = 11.5f, color = Dim, maxW = 170f)
        if (a.kind == AlertKind.HEADPHONE) {
            Canvas(Modifier.at(275f, 30f, true, true).size(40.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(Color(0x2E5EC2FF), 20.dp.toPx(), c)
                drawCircle(Color(0xFF0D1A26), 15.5f.dp.toPx(), c)
                drawCircle(Color(0x8C5EC2FF), 15f.dp.toPx(), c, style = Stroke(1.5f.dp.toPx()))
            }
            Box(Modifier.at(275f, 30f, true, true).size(16.dp)) { Ico(P_PHONES, 24f, 16f, stroke = 2f) }
        } else if (a.value.isNotEmpty()) {
            Txt(a.value, 22f, 30f, size = 24f, right = true, maxW = 90f)
        }
    }
}

// ----------------------- Kerangka halaman (header + titik, tetap saat ganti halaman) -----------------------
@Composable
private fun Chrome(page: Int, a: State<Float>) {
    val battery by Store.battery.collectAsState()
    val media by Store.media.collectAsState()
    val date = rememberClock("EEE d MMM yyyy")
    val time = rememberClock("HH:mm")
    Abs(Modifier.fillMaxSize().graphicsLayer { alpha = a.value }) {
        MiniRing(battery, Modifier.at(146f, 18f, true, true))
        Text(
            buildAnnotatedString {
                append(date)
                append("  ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(time) }
            },
            color = White, fontSize = 13.sp, maxLines = 1,
            modifier = Modifier.at(163f, 18f, cy = true).widthIn(max = 200.dp),
        )
        Bars(media?.playing == true, Modifier.at(396f, 18f, true, true))
        Row(
            Modifier.at(210f, 133f, true, true),
            horizontalArrangement = Arrangement.spacedBy(4.5f.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { i ->
                val on = i == page
                val dw by animateDpAsState(if (on) 15.dp else 3.6f.dp, tween(400, easing = SpringEase), label = "dot")
                val dc by animateColorAsState(if (on) White else Color(0x73FFFFFF), tween(300, easing = CssEase), label = "dotc")
                Box(Modifier.width(dw).height(3.6f.dp).clip(RoundedCornerShape(2.dp)).background(dc))
            }
        }
    }
}

@Composable
private fun Arrow(x: Float, y: Float, left: Boolean, onClick: () -> Unit) {
    Box(Modifier.at(x, y, true, true).size(40.dp).tap(onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(Color(0x47FFFFFF)),
            contentAlignment = Alignment.Center,
        ) { Ico(if (left) P_CHEV_L else P_CHEV_R, 10f, 9f, stroke = 2.4f) }
    }
}

// ----------------------- Musik + Lirik -----------------------
@Composable
private fun MusicLayer(
    lyrics: Boolean, onToggleLyrics: () -> Unit, onNext: () -> Unit, ctx: Context,
    pw: Float = 420f, ph: Float = 140f,
) {
    val media by Store.media.collectAsState()
    val info = media
    val pos = rememberPosition(info)
    val dur = info?.durationMs ?: 0L
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val remain = if (dur > 0) "-" + fmt(dur - pos) else "--:--"
    val compact = lyrics && pw < 500f

    if (compact) {
        // Layar sempit: lirik memenuhi lebar panel, kontrol di bawah. Teks tetap berukuran penuh (tidak diskalakan kecil).
        Abs(Modifier.fillMaxSize()) {
            val head = if (info != null) "${info.title} · ${info.artist}" else "Tidak ada musik"
            Txt(head, 18f, 17f, size = 12f, color = Dim, maxW = pw - 36f)
            LyricsColumn(pos, 18f, 30f, pw - 36f, ph - 30f - 66f, true)
            Txt(fmt(pos), 18f, ph - 48f, size = 12f)
            SeekBar(58f, ph - 48f, pw - 116f, frac, dur)
            Txt(remain, 18f, ph - 48f, size = 12f, right = true)
            Box(Modifier.at(34f, ph - 19f, true, true).size(40.dp).tap(onToggleLyrics), contentAlignment = Alignment.Center) {
                LyricsBubble(true)
            }
            Ctl(pw / 2f - 47f, ph - 19f, P_PREV) { MediaMonitor.prev() }
            Ctl(pw / 2f, ph - 19f, if (info?.playing == true) P_PAUSE else P_PLAY) { MediaMonitor.playPause() }
            Ctl(pw / 2f + 47f, ph - 19f, P_NEXT) { MediaMonitor.next() }
        }
        return
    }

    val barW = if (lyrics) 108f else 260f
    // panel lirik yang lebih tinggi: seek bar dan kontrol ikut turun ke bawah
    val dy = if (lyrics) (ph - 140f).coerceAtLeast(0f) else 0f

    Abs(Modifier.fillMaxSize()) {
        if (!lyrics) Arrow(395f, 70f, false, onNext)
        Art(info?.art, Modifier.at(22f, 12f))
        Txt(info?.title ?: "Tidak ada musik", 100f, 47f, size = 15f, weight = FontWeight.SemiBold, maxW = if (lyrics) 140f else 270f)
        Txt(
            info?.artist ?: "Putar lagu dan beri akses notifikasi",
            100f, 63f, size = 11.5f, color = Dim, maxW = if (lyrics) 140f else 270f,
        )
        Txt(fmt(pos), 25f, 91f + dy)
        SeekBar(if (lyrics) 76f else 77f, 91f + dy, barW, frac, dur)
        if (lyrics) Txt(remain, 284f, 91f + dy, right = true) else Txt(remain, 25f, 91f, right = true)

        val bx = 53f
        Box(Modifier.at(bx, 118f + dy, true, true).size(40.dp).tap(onToggleLyrics), contentAlignment = Alignment.Center) {
            LyricsBubble(lyrics)
        }
        val cx0 = if (lyrics) 91f else 162.5f
        val gap = if (lyrics) 40f else 47.5f
        Ctl(cx0, 118f + dy, P_PREV) { MediaMonitor.prev() }
        Ctl(cx0 + gap, 118f + dy, if (info?.playing == true) P_PAUSE else P_PLAY) { MediaMonitor.playPause() }
        Ctl(cx0 + 2 * gap, 118f + dy, P_NEXT) { MediaMonitor.next() }

        if (lyrics) {
            Box(Modifier.at(252f, 22f).width(1.dp).height((ph - 38f).dp).background(Color(0x1FFFFFFF)))
            LyricsColumn(pos, 264f, 16f, 234f, ph - 32f, false)
        }
    }
}

@Composable
private fun LyricsBubble(active: Boolean) {
    Canvas(Modifier.size(17.dp, 15.dp)) {
        val k = size.width / 18f
        val path = Path().apply {
            addPath(PathParser().parsePathString(P_BUBBLE).toPath())
        }
        withTransform({ scale(k, k, Offset.Zero) }) {
            if (active) {
                drawPath(path, Color.White)
                drawCircle(Color.Black, 1.3f, Offset(6.3f, 5.4f))
                drawCircle(Color.Black, 1.3f, Offset(11.3f, 5.4f))
            } else {
                val c = Color(0xBFFFFFFF)
                drawPath(path, c, style = Stroke(1.3f))
                drawCircle(c, 1.3f, Offset(6.3f, 5.4f))
                drawCircle(c, 1.3f, Offset(11.3f, 5.4f))
            }
        }
    }
}

@Composable
private fun SeekBar(x: Float, y: Float, width: Float, frac: Float, durationMs: Long) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val smooth by animateFloatAsState(frac, tween(300, easing = LinearEasing), label = "seek")
    val f = drag ?: smooth
    Box(
        Modifier
            .at(x, y, cy = true)
            .width(width.dp)
            .height(24.dp)
            .pointerInput(durationMs) {
                detectTapGestures { o ->
                    if (durationMs > 0) MediaMonitor.seekTo((o.x / size.width * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        val d = drag
                        if (d != null && durationMs > 0) MediaMonitor.seekTo((d * durationMs).toLong())
                        drag = null
                    },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x38FFFFFF)))
        Box(Modifier.fillMaxWidth(f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(White))
    }
}

@Composable
private fun LyricsColumn(pos: Long, x: Float, y: Float, wDp: Float, hDp: Float, center: Boolean) {
    val ly by Store.lyrics.collectAsState()
    val box = Modifier.at(x, y).width(wDp.dp).height(hDp.dp).fadeEdges()
    when (val s = ly) {
        is LyricsState.Found -> LyricsFound(s.lines, pos, wDp, hDp, center, box)
        LyricsState.Loading -> Text("Mencari lirik...", color = Dim, fontSize = 13.sp, modifier = box.padding(top = hDp.dp / 2 - 8.dp))
        LyricsState.None -> Text("Putar lagu untuk melihat lirik.", color = Dim, fontSize = 13.sp, modifier = box.padding(top = hDp.dp / 2 - 8.dp))
        LyricsState.Missing -> Text("Lirik tersinkron tidak ditemukan.", color = Dim, fontSize = 13.sp, modifier = box.padding(top = hDp.dp / 2 - 8.dp))
    }
}

private val LyricStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, letterSpacing = 0.sp)

/**
 * Lirik dengan baris aktif selalu tepat di tengah kolom. Posisi dihitung langsung dari tinggi tiap baris
 * (bukan dari scroll LazyColumn), lalu seluruh susunan meluncur .7 s saat baris berganti.
 */
@Composable
private fun LyricsFound(lines: List<LrcLine>, pos: Long, wDp: Float, hDp: Float, center: Boolean, modifier: Modifier) {
    val idx = lines.indexOfLast { it.timeMs <= pos + 250 }.coerceAtLeast(0)
    val dens = LocalDensity.current
    val tm = rememberTextMeasurer()
    val style = remember(center) {
        LyricStyle.copy(textAlign = if (center) TextAlign.Center else TextAlign.Start)
    }
    val wPx = with(dens) { wDp.dp.roundToPx() }
    fun heightOf(i: Int): Float {
        val t = lines.getOrNull(i)?.text?.ifEmpty { " " } ?: " "
        val n = tm.measure(AnnotatedString(t), style, constraints = Constraints(maxWidth = wPx)).lineCount
        return n * 20f + 9f
    }
    // Offset awal dihitung saat komposisi (remember dengan kunci idx) dan progres dibuat baru bersamaan, jadi
    // frame pertama setelah baris berganti sudah memakai offset awal: tidak ada kedipan baris langsung di tengah.
    val prevIdx = remember { intArrayOf(idx) }
    val slideFrom = remember(idx, lines) {
        val a = prevIdx[0]
        prevIdx[0] = idx
        var start = 0f
        if (a != idx && a in lines.indices && abs(a - idx) <= 4) {
            // baris lama harus tetap tampak di tengah pada awal gerakan
            var d = heightOf(a) / 2f + heightOf(idx) / 2f
            val lo = minOf(a, idx)
            val hi = maxOf(a, idx)
            for (k in lo + 1 until hi) d += heightOf(k)
            start = if (a < idx) d else -d
        }
        start
    }
    val slideP = remember(idx, lines) { Animatable(0f) }
    LaunchedEffect(slideP) { slideP.animateTo(1f, tween(700, easing = SpringEase)) }
    val from = maxOf(0, idx - 7)
    val to = minOf(lines.lastIndex, idx + 7)
    Layout(
        modifier = modifier,
        content = {
            for (i in from..to) {
                key(i) {
                    val on = i == idx
                    val a = animateFloatAsState(if (on) 1f else 0.32f, tween(500, easing = CssEase), label = "la")
                    val k = animateFloatAsState(if (on) 1f else 0.93f, tween(500, easing = CssEase), label = "ls")
                    Text(
                        lines[i].text, style = style, color = White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = a.value
                                scaleX = k.value
                                scaleY = k.value
                                transformOrigin = TransformOrigin(if (center) 0.5f else 0f, 0.5f)
                            }
                            .tap { MediaMonitor.seekTo(lines[i].timeMs) }
                            .padding(vertical = 4.5f.dp),
                    )
                }
            }
        },
    ) { ms, c ->
        val w = c.maxWidth
        val h = c.maxHeight
        val ps = ms.map { it.measure(Constraints(maxWidth = w)) }
        val ai = (idx - from).coerceIn(0, (ps.size - 1).coerceAtLeast(0))
        val tops = IntArray(ps.size)
        if (ps.isNotEmpty()) {
            tops[ai] = h / 2 - ps[ai].height / 2
            for (k in ai - 1 downTo 0) tops[k] = tops[k + 1] - ps[k].height
            for (k in ai + 1 until ps.size) tops[k] = tops[k - 1] + ps[k - 1].height
        }
        layout(w, h) {
            val sh = (slideFrom * (1f - slideP.value) * density).roundToInt()
            ps.forEachIndexed { k, p -> p.place(0, tops[k] + sh) }
        }
    }
}

@Composable
private fun LyricLine(text: String) {
    // Satu baris lirik aktif saat notch tertutup.
    Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        androidx.compose.animation.Crossfade(targetState = text, animationSpec = tween(260), label = "lyline") { t ->
            Text(
                t, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp, letterSpacing = 0.sp,
                textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ----------------------- Pomodoro -----------------------
@Composable
private fun PomoLayer(page: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val p by Store.pomodoro.collectAsState()
    val col = if (p.focus) Color(0xFFFF4B55) else Color(0xFF1FBF8F)
    val totalMs = (if (p.focus) p.focusMin else p.breakMin) * 60_000L
    val prog = (p.remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

    Abs(Modifier.fillMaxSize()) {
        Arrow(22f, 70f, true, onPrev)
        Arrow(395f, 70f, false, onNext)
        Txt("Pomodoro", 15f, 19f, size = 15f, weight = FontWeight.SemiBold)

        Box(
            Modifier.at(80f, 74f, true, true).size(88.dp).drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(col.copy(alpha = 0.32f), Color.Transparent),
                        center = center, radius = 40.dp.toPx(),
                    ),
                    radius = 44.dp.toPx(),
                )
            }
        )
        Canvas(Modifier.at(80f, 74f, true, true).size(64.dp)) {
            val sw = 6.6f.dp.toPx()
            val d = size.width - sw
            drawArc(col.copy(alpha = 0.22f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw))
            drawArc(
                col, -90f, 360f * prog, false, Offset(sw / 2, sw / 2), Size(d, d),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Box(Modifier.at(80f, 75f, true, true).size(30.dp), contentAlignment = Alignment.Center) {
            Ico(P_TIMER, 24f, 30f, stroke = 2f)
        }

        Txt(fmtCeil(p.remainingMs), 265f, 50f, size = 31f, weight = FontWeight.Bold, cx = true, spacing = -0.5f)
        CircleBtn(330f, 50f, 24f, Color(0x1FFFFFFF), { Pomodoro.reset() }) { Ico(P_RESET, 24f, 13f, stroke = 2.4f) }
        Txt(if (p.focus) "Waktu fokus" else "Waktu istirahat", 265f, 76f, size = 11.5f, color = Dim, cx = true)

        Pill(144f, 92f, 86f, 28f, if (!p.focus) Color(0x38FFFFFF) else Color(0x1FFFFFFF), { if (p.focus) Pomodoro.switchMode() }) {
            Ico(P_CUP, 24f, 13f, stroke = 2f)
            Text("Istirahat", color = White, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
        }
        Pill(238f, 92f, 55f, 28f, col, { Pomodoro.toggle() }) {
            Ico(if (p.running) P_PAUSE else P_PLAY, 24f, 13f)
        }
        Pill(301f, 92f, 85f, 28f, if (p.focus) Color(0x38FFFFFF) else Color(0x1FFFFFFF), { if (!p.focus) Pomodoro.switchMode() }) {
            Ico(P_TARGET, 24f, 13f, stroke = 2f)
            Text("Fokus", color = White, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// ----------------------- Air minum -----------------------
@Composable
private fun WaterLayer(page: Int, onPrev: () -> Unit, onNext: () -> Unit, ctx: Context) {
    val w by Store.water.collectAsState()
    val frac = (w.totalMl.toFloat() / w.goalMl).coerceIn(0f, 1f)
    val pct = (frac * 100).toInt()
    val reached = w.totalMl >= w.goalMl
    val remindOn = w.remindEveryMin > 0
    val cyan = Color(0xFF56C8FF)
    // mulai dari nol lalu tumbuh dengan pantulan, 220 ms setelah halaman muncul
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(220)
        grown = true
    }
    val barW by animateDpAsState(
        if (grown) (152f * frac).dp else 0.dp,
        tween(1000, 150, CubicBezierEasing(0.3f, 1.2f, 0.5f, 1f)), label = "wbar",
    )

    Abs(Modifier.fillMaxSize()) {
        Arrow(22f, 70f, true, onPrev)
        Arrow(395f, 70f, false, onNext)
        Text(
            buildAnnotatedString {
                append("Air ")
                withStyle(SpanStyle(color = cyan)) { append("Minum") }
            },
            color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.at(15f, 19f, cy = true),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) {
                    append("%.1f".format(Locale.US, w.totalMl / 1000f))
                }
                withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Dim)) {
                    append("  / %.1f L".format(Locale.US, w.goalMl / 1000f))
                }
            },
            color = White, maxLines = 1, modifier = Modifier.at(44f, 47f, cy = true),
        )
        Box(
            Modifier
                .at(162f, 37f, cy = true)
                .size(34.dp, 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (remindOn) Color(0xFF2699FF) else Color(0x40FFFFFF))
                .tap { Store.setRemind(if (remindOn) 0 else 90) }
        ) {
            Box(
                Modifier
                    .align(if (remindOn) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center,
            ) { Ico(P_BELL, 24f, 9f, color = if (remindOn) Color(0xFF2699FF) else Color(0xFF666B78)) }
        }
        Text(
            buildAnnotatedString {
                append("Hari ini · ")
                withStyle(SpanStyle(color = Color(0xFF4FC0FF), fontWeight = FontWeight.Bold)) { append("$pct%") }
                append(if (reached) " · Target tercapai" else " · ${w.goalMl - w.totalMl} ml lagi")
            },
            color = Dim, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.at(44f, 71f, cy = true).widthIn(max = 160.dp),
        )
        Box(Modifier.at(44f, 82.5f).size(152.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x26FFFFFF)))
        Box(
            Modifier.at(44f, 82.5f).size(barW, 5.dp).clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF66DBFF), Color(0xFF2D6BFF))))
        )
        // Tombol utama: menambah satu botol. Labelnya langsung menyebut jumlahnya (mis. +250 ml).
        Pill(44f, 98f, 86f, 26f, Color(0xFF1E7FE0), { Store.addWater(w.bottleMl) }) {
            Ico(P_DROP, 24f, 13f, color = White)
            Text(
                "+${w.bottleMl} ml", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 5.dp),
            )
        }
        Pill(136f, 98f, 60f, 26f, Color(0x1FFFFFFF), { openApp(ctx, 2) }) {
            Canvas(Modifier.size(13.dp)) {
                val k = size.width / 24f
                drawLine(White, Offset(4 * k, 8 * k), Offset(20 * k, 8 * k), 2 * k, StrokeCap.Round)
                drawLine(White, Offset(4 * k, 16 * k), Offset(20 * k, 16 * k), 2 * k, StrokeCap.Round)
                drawCircle(Color.Black, 2 * k, Offset(9 * k, 8 * k))
                drawCircle(White, 2 * k, Offset(9 * k, 8 * k), style = Stroke(2 * k))
                drawCircle(Color.Black, 2 * k, Offset(15 * k, 16 * k))
                drawCircle(White, 2 * k, Offset(15 * k, 16 * k), style = Stroke(2 * k))
            }
            Text("Atur", color = White, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 5.dp))
        }

        Box(Modifier.at(208f, 22f).width(1.dp).height(102.dp).background(Color(0x1FFFFFFF)))

        // grafik per jam (200 ml per garis, maks 400 ml)
        val hours = IntArray(24)
        val cal = Calendar.getInstance()
        w.today.forEach {
            cal.timeInMillis = it.time
            hours[cal.get(Calendar.HOUR_OF_DAY)] += it.ml
        }
        val nowH = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val s0 = ((nowH - 3).coerceIn(0, 15) / 3) * 3
        Box(Modifier.at(220f, 56f).width(112.dp).height(1.dp).background(Color(0x1FFFFFFF)))
        Box(Modifier.at(220f, 81f).width(112.dp).height(1.dp).background(Color(0x1FFFFFFF)))
        for (i in 0..7) {
            val hh = s0 + i
            if (hh > 23) break
            WaterBar(i, 227f + i * 14f - 3.5f, minOf(hours[hh], 400) / 400f * 48.5f, grown)
        }
        Box(Modifier.at(216f, 106f).width(122.dp).height(1.dp).background(Color(0x7350B4FF)))
        Box(Modifier.at(338f, 50f).width(1.dp).height(56.dp).background(Color(0x2EFFFFFF)))
        Txt("400", 344f, 56f, size = 9.5f, color = Dim)
        Txt("200", 344f, 81f, size = 9.5f, color = Dim)
        Txt("0 ml", 344f, 106f, size = 9.5f, color = Dim)
        Txt("%02d".format(s0), 227f, 117f, size = 9.5f, color = Dim, cx = true)
        Txt("%02d".format(s0 + 3), 269f, 117f, size = 9.5f, color = Dim, cx = true)
        Txt("%02d".format(s0 + 6), 311f, 117f, size = 9.5f, color = Dim, cx = true)
    }
}

@Composable
private fun WaterBar(i: Int, x: Float, h: Float, grown: Boolean) {
    val bh by animateDpAsState(
        if (grown) h.dp else 0.dp,
        tween(800, i * 55, CubicBezierEasing(0.34f, 1.5f, 0.5f, 1f)), label = "wb",
    )
    Box(
        Modifier.at(x, 57.5f).size(7.dp, 48.5f.dp)
            .clip(RoundedCornerShape(3.5f.dp)).background(Color(0x12FFFFFF))
    )
    if (bh.value > 0.1f) {
        val top = if (i == 0) Color(0xFF5CC6FF) else Color(0xBF5CC6FF)
        val bot = if (i == 0) Color(0xFF1D63E0) else Color(0xBF1D63E0)
        Box(
            Modifier.at(x, 104.5f - bh.value).size(7.dp, bh)
                .clip(RoundedCornerShape(3.5f.dp)).background(Brush.verticalGradient(listOf(top, bot)))
        )
    }
}

// ----------------------- To-do -----------------------
@Composable
private fun TodoLayer(page: Int, onPrev: () -> Unit, ctx: Context) {
    val tasks by Store.tasks.collectAsState()
    val done = tasks.count { it.done }
    val shown = tasks.sortedBy { it.done }.take(3)

    // null = melihat daftar, -1 = menambah, selain itu = mengubah tugas dengan id tsb
    var editId by remember { mutableStateOf<Long?>(null) }
    var text by remember { mutableStateOf("") }
    var remind by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val uiScope = rememberCoroutineScope()

    fun startAdd() { text = ""; remind = false; editId = -1L }
    fun startEdit(t: Task) { text = t.title; remind = t.dueAt != null; editId = t.id }
    fun cancel() { editId = null; text = "" }
    fun save() {
        val id = editId ?: return
        val title = text.trim()
        if (title.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (id == -1L) {
                Store.addTask(title, if (remind) now + 3_600_000L else null)
            } else {
                val old = tasks.firstOrNull { it.id == id }
                val due = if (!remind) null else (old?.dueAt ?: (now + 3_600_000L))
                Store.editTask(id, title, due)
            }
        }
        cancel()
    }
    LaunchedEffect(editId != null) {
        Store.editing.value = editId != null
        if (editId != null) {
            delay(180)   // beri waktu jendela menjadi bisa fokus
            try { focus.requestFocus() } catch (e: Exception) { }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Store.editing.value = false } }

    Abs(Modifier.fillMaxSize()) {
        Arrow(22f, 70f, true, onPrev)
        Text(
            buildAnnotatedString {
                append("Daftar ")
                withStyle(SpanStyle(color = Color(0xFF38D36F))) { append("Tugas") }
            },
            color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.at(15f, 19f, cy = true),
        )
        Txt("$done/${tasks.size}", 44f, 48f, size = 32f, weight = FontWeight.Bold)
        Txt("Tugas selesai", 44f, 76f, size = 11.5f, color = Dim)
        Pill(41f, 93f, 74f, 28f, Color(0xFF1E9F55), { startAdd() }) {
            Ico(P_PLUS, 24f, 12f)
            Text("Tambah", color = White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
        }
        Pill(121f, 93f, 64f, 28f, Color(0x1FFFFFFF), { Store.clearDone() }) {
            Ico(P_CHECK, 10f, 11f, stroke = 2.2f)
            Text("Bersih", color = White, fontSize = 11.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
        }

        if (editId != null) {
            Box(
                Modifier
                    .at(202f, 34f)
                    .size(176.dp, 36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(80) },
                    singleLine = true,
                    textStyle = TextStyle(color = White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFF38D36F)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged { st ->
                            // fokus terebut (mis. oleh ketukan di notch) saat masih mengetik: ambil lagi
                            if (!st.isFocused && Store.editing.value) {
                                uiScope.launch {
                                    delay(60)
                                    if (Store.editing.value) {
                                        try { focus.requestFocus() } catch (e: Exception) { }
                                        keyboard?.show()
                                    }
                                }
                            }
                        },
                )
                if (text.isEmpty()) {
                    Text(if (editId == -1L) "Tulis tugas baru" else "Ubah tugas", color = Dim, fontSize = 13.sp, maxLines = 1)
                }
            }
            Pill(202f, 80f, 78f, 28f, Color(0xFF1E9F55), { save() }) {
                Text("Simpan", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
            Pill(286f, 80f, 56f, 28f, Color(0x1FFFFFFF), { cancel() }) {
                Text("Batal", color = White, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
            CircleBtn(362f, 94f, 28f, if (remind) Color(0xFF2699FF) else Color(0x1FFFFFFF), { remind = !remind }) {
                Ico(P_BELL, 24f, 13f)
            }
        } else {
            if (tasks.isEmpty()) {
                Txt("Belum ada tugas", 290f, 70f, size = 12f, color = Dim, cx = true)
            }
            shown.forEachIndexed { i, t ->
                Row(
                    Modifier
                        .at(202f, 34f + i * 31f)
                        .width(180.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (t.done) Color(0x6128783C) else Color(0x1AFFFFFF))
                        .padding(start = 3.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // lingkaran = centang
                    Box(Modifier.size(26.dp).tap { Store.toggleTask(t.id) }, contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (t.done) Color(0xFF2FD06B) else Color(0x24FFFFFF)),
                            contentAlignment = Alignment.Center,
                        ) { if (t.done) Ico(P_CHECK, 10f, 9f, stroke = 2.6f) }
                    }
                    // judul = ubah
                    Box(
                        Modifier.weight(1f).fillMaxHeight().tap { startEdit(t) }.padding(start = 2.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            t.title,
                            color = Color(0xBFFFFFFF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None,
                        )
                    }
                    // silang = hapus
                    Box(Modifier.size(26.dp).tap { Store.deleteTask(t.id) }, contentAlignment = Alignment.Center) {
                        Ico("M3 3 L9 9 M9 3 L3 9", 12f, 9f, color = Color(0x99FFFFFF), stroke = 1.8f)
                    }
                }
            }
            if (tasks.size > 3) {
                Txt("+${tasks.size - 3} lagi", 292f, 128f, size = 10f, color = Dim, cx = true)
            }
        }
    }
}
