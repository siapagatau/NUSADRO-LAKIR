package id.nusa.notch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================= Komponen UI bergaya situs Nusa =======================
// Token, radius, bobot huruf, dan "app icon" squircle mengikuti CSS situs (index.html).

internal val InkDark = Color(0xFF05060C)
internal val BorderSoft = Color(0x2EECEFF7)

// ---- Ikon (path 24x24 dari situs; dipakai bersama Ico di NotchUi.kt) ----
internal const val I_HOME = "M3 11 L12 3 L21 11 M5 9.5 V20 H10 V14 H14 V20 H19 V9.5"
internal const val I_CHECK_CIRCLE = "M3.5 12 a8.5 8.5 0 1 0 17 0 a8.5 8.5 0 1 0 -17 0 M8 12.3 L10.7 15 L16.2 9.4"
internal const val I_TICK = "M5 12.5 L9.5 17 L19 7.5"
internal const val I_SLIDERS =
    "M4 8 H20 M4 16 H20 M7 8 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0 M13 16 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0"
internal const val I_MUSIC =
    "M10.1 17 V6 L19.6 4 V15 M4.9 17 a2.6 2.6 0 1 0 5.2 0 a2.6 2.6 0 1 0 -5.2 0 M14.4 15 a2.6 2.6 0 1 0 5.2 0 a2.6 2.6 0 1 0 -5.2 0"
internal const val I_LYRICS =
    "M6 4.5 h12 a3 3 0 0 1 3 3 v6 a3 3 0 0 1 -3 3 h-7 l-5 3.5 l0.8 -3.5 H6 a3 3 0 0 1 -3 -3 v-6 a3 3 0 0 1 3 -3 Z"
internal const val I_STOPWATCH =
    "M4.5 13.5 a7.5 7.5 0 1 0 15 0 a7.5 7.5 0 1 0 -15 0 M12 13.5 V9.5 M10 3 h4 M12 3 v3"
internal const val I_BOLT = "M13 2.5 L5.5 13.2 h5.3 l-1.3 8.3 l7.5 -10.7 h-5.3 Z"
internal const val I_EYES =
    "M5.8 12 a2.2 3.4 0 1 0 4.4 0 a2.2 3.4 0 1 0 -4.4 0 M13.8 12 a2.2 3.4 0 1 0 4.4 0 a2.2 3.4 0 1 0 -4.4 0"
internal const val I_TRASH = "M5 7 h14 M10 7 V4.5 h4 V7 M7 7 l1 12.5 h8 l1 -12.5 M10.5 11 v5 M13.5 11 v5"
internal const val I_ALARM =
    "M4 13 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 M12 9 v4.5 l3 1.8 M4 5 l3 -2.5 M20 5 l-3 -2.5"
internal const val I_UNDO = "M9 6 L4 11 L9 16 M4 11 H15 a5 5 0 0 1 0 10 H11"
internal const val I_MINUS = "M5 12 H19"
internal const val I_PLUS_LINE = "M5 12 H19 M12 5 V19"
internal const val I_SHIELD = "M12 3 L19 6 V12 C19 16.5 16 19.5 12 21 C8 19.5 5 16.5 5 12 V6 Z"
internal const val I_POWER = "M12 3 V12 M7 6.5 a7.5 7.5 0 1 0 10 0"
internal const val I_CHEV_RIGHT = "M9 5 L16 12 L9 19"
internal const val I_LAYERS = "M12 3 L21 8 L12 13 L3 8 Z M3 12.5 L12 17.5 L21 12.5 M3 16.5 L12 21.5 L21 16.5"

/**
 * Klik dengan umpan balik instan: tombol mengecil sedikit begitu disentuh. Tanpa ripple (lebih ringan)
 * dan tidak ada jeda yang membuat tombol terasa "mati".
 */
@Composable
internal fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(80), label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Cahaya gradasi di bawah tombol putih (::before .btn di situs). */
internal fun Modifier.glowUnder(strength: Float = 0.75f): Modifier = this.drawBehind {
    val top = size.height * 0.45f
    val l = size.width * 0.08f
    val w = size.width * 0.84f
    val h = size.height - top + 14.dp.toPx()
    for (i in 0..6) {
        val g = i * 2.8f.dp.toPx()
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(NusaCyan, NusaIndigo, NusaViolet), startX = l - g, endX = l + w + g,
            ),
            topLeft = Offset(l - g, top - g),
            size = Size(w + 2 * g, h + 2 * g),
            cornerRadius = CornerRadius((h + 2 * g) / 2f),
            alpha = strength * 0.17f * (1f - i / 7f),
        )
    }
}

@Composable
fun H1(text: String, size: TextUnit = 42.sp, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text, modifier = modifier, color = NusaInk, fontSize = size, fontWeight = FontWeight.ExtraBold,
        lineHeight = (size.value * 1.02f).sp, letterSpacing = (-0.035f * size.value).sp, textAlign = align,
    )
}

@Composable
fun SecTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, color = NusaInk, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
        lineHeight = 30.sp, letterSpacing = (-0.8f).sp,
    )
}

@Composable
fun Lede(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start, size: TextUnit = 15.sp) {
    Text(text, modifier = modifier, color = NusaMist, fontSize = size, lineHeight = (size.value * 1.5f).sp, textAlign = align)
}

@Composable
fun Chip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, NusaLine, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) { Text(text, color = NusaMist, fontSize = 13.sp) }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NusaPanel)
            .border(1.dp, NusaLine, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        content = content,
    )
}

/** Ikon squircle bergradasi dengan kilap di atas, seperti .app di situs. */
@Composable
fun AppIcon(c1: Color, c2: Color, size: Dp = 52.dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        Modifier
            .size(size)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(c2.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(this.size.width / 2f, this.size.height * 1.0f),
                        radius = this.size.width * 0.75f,
                    ),
                    topLeft = Offset(-this.size.width * 0.1f, this.size.height * 0.3f),
                    size = Size(this.size.width * 1.2f, this.size.height * 0.9f),
                    cornerRadius = CornerRadius(this.size.width * 0.4f),
                )
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c1, c2)))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0x38FFFFFF), Color.Transparent), endY = this.size.height * 0.48f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun GlowButton(
    text: String, modifier: Modifier = Modifier, icon: String? = null, enabled: Boolean = true, onClick: () -> Unit,
) {
    val a = if (enabled) 1f else 0.45f
    Box(
        modifier
            .pressable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = a }
            .then(if (enabled) Modifier.glowUnder() else Modifier)
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Ico(icon, 24f, 18f, color = InkDark, stroke = 2.4f)
            Text(text, color = InkDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PillBtn(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false,
    enabled: Boolean = true, icon: String? = null, iconFilled: Boolean = false, tint: Color? = null,
) {
    val fg = if (filled) InkDark else NusaInk
    val bg = tint ?: Color.White
    Box(
        modifier
            .pressable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .then(if (filled) Modifier.background(bg) else Modifier.border(1.dp, BorderSoft, RoundedCornerShape(50)))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Ico(icon, 24f, 16f, color = if (tint != null && filled) Color.White else fg, stroke = if (iconFilled) 0f else 2.2f)
            Text(
                text, color = if (tint != null && filled) Color.White else fg,
                fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            )
        }
    }
}

@Composable
fun RoundIconBtn(path: String, onClick: () -> Unit, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .border(1.dp, BorderSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Ico(path, 24f, 16f, color = NusaInk, stroke = 2.2f) }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = NusaInk, fontSize = 15.sp, modifier = Modifier.weight(1f))
        RoundIconBtn(I_MINUS, onMinus)
        Text(
            value, color = NusaInk, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 4.dp),
        )
        RoundIconBtn(I_PLUS_LINE, onPlus)
    }
}

// ======================= Komponen layar =======================
@Composable
fun ScreenTitle(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title, color = NusaInk, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp, letterSpacing = (-0.8f).sp,
        )
        if (sub != null) Lede(sub, size = 15.sp)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier = modifier, color = NusaMist, fontSize = 12.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.2f.sp,
    )
}

@Composable
fun StatusPill(on: Boolean, onText: String, offText: String) {
    val tint by animateColorAsState(if (on) Color(0xFF33E07A) else Color(0xFF8C94A8), tween(250), label = "st")
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.08f))))
            .border(1.dp, tint.copy(alpha = 0.5f), shape)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(tint.copy(alpha = 0.28f)))
            Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        }
        Text(
            if (on) onText else offText,
            color = if (on) Color(0xFFC4F7D8) else NusaMist,
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MiniTag(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6f.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Pilihan satu dari beberapa (mis. Kiri | Kanan). */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x0FECEFF7))
            .border(1.dp, NusaLine, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color.White else Color.Transparent, tween(180), label = "segbg")
            val fg by animateColorAsState(if (on) InkDark else NusaInk, tween(180), label = "segfg")
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .pressable { onSelect(i) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ======================= Navigasi bawah =======================
class NavItem(val label: String, val icon: String, val filled: Boolean = false)

@Composable
fun NusaBottomBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Box(Modifier.fillMaxWidth().background(NusaVoid).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF121626), NusaPanel)))
                .border(1.dp, NusaLine, shape)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { i, item ->
                val wgt by animateFloatAsState(if (i == selected) 2.3f else 1f, tween(260), label = "navw")
                NavButton(item, i == selected, Modifier.weight(wgt)) { onSelect(i) }
            }
        }
    }
}

@Composable
private fun NavButton(item: NavItem, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg by animateColorAsState(if (on) Color.White else NusaMist, tween(200), label = "navfg")
    val pillA by animateFloatAsState(if (on) 1f else 0f, tween(220), label = "navpill")
    Box(
        modifier.fillMaxHeight().pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pillA }
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.horizontalGradient(listOf(NusaIndigo, NusaCyan))),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Ico(item.icon, 24f, 22f, color = fg, stroke = if (item.filled) 0f else 2f)
            if (on) {
                Text(item.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}
