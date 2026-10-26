package id.nusa.notch

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Pose notch yang dibaca Compose tiap frame.
 *
 * [edge]: 0 atas, 1 kanan, 2 bawah, 3 kiri. Dipakai hanya saat notch diam menempel di tepi.
 * [shapeDeg]: rotasi bentuk notch supaya sisi tempelnya menghadap tepi layar.
 * [contentDeg]: rotasi isi (teks, ikon). Di tepi bawah isi tetap tegak (tidak terbalik), hanya bentuknya yang dibalik.
 * [sx], [sy]: regangan "slime" pada sumbu bentuk (sx searah tepi).
 */
data class Pose(
    val edge: Int = 0,
    val shapeDeg: Float = 0f,
    val contentDeg: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    /** Pusat badan notch di layar (px); dipakai saat jendela layar penuh (digeser). */
    val cx: Float = 0f,
    val cy: Float = 0f,
    /** Tarikan karet 0..1 (jari menarik notch menjauhi tepi). */
    val pull: Float = 0f,
    /** Ukuran layar penuh (px). Jendela seukuran ini = mode digeser. */
    val fw: Int = 0,
    val fh: Int = 0,
    /** Pusat badan notch searah tepi (px) saat diam; sudah dijepit supaya panel tidak keluar layar. */
    val along: Float = 0f,
    /** true saat digeser/meluncur (posisi dari [cx],[cy]); false saat diam menempel di tepi. */
    val moving: Boolean = false,
) {
    companion object {
        fun rest(edge: Int, fw: Int = 0, fh: Int = 0, along: Float = 0f) =
            Pose(edge, shapeOf(edge), contentOf(edge), 1f, 1f, 0f, 0f, 0f, fw, fh, along, false)
        fun shapeOf(edge: Int) = when (edge) { 1 -> 90f; 2 -> 180f; 3 -> 270f; else -> 0f }
        fun contentOf(edge: Int) = when (edge) { 1 -> 90f; 3 -> -90f; else -> 0f }
    }
}

/** Ekspresi maskot saat HP diguncang. */
enum class Expr { NORMAL, SHOCK, DIZZY, HAPPY, SQUINT }

/**
 * Keliling layar sebagai persegi bersudut membulat. Koordinat s dihitung searah jarum jam dari ujung
 * kiri sisi atas. Sudut yang membulat adalah tempat notch "lewat" (tidak pernah berhenti di sana).
 */
class Perimeter(val w: Float, val h: Float, rcIn: Float) {
    val rc = rcIn.coerceIn(1f, minOf(w, h) / 4f)
    private val sw = w - 2f * rc
    private val sh = h - 2f * rc
    private val arc = (PI / 2.0 * rc).toFloat()

    /** Awal tiap segmen: 0 atas, 1 sudut kanan-atas, 2 kanan, 3 kanan-bawah, 4 bawah, 5 kiri-bawah, 6 kiri, 7 kiri-atas. */
    private val b = FloatArray(9)
    val total: Float

    init {
        val len = floatArrayOf(sw, arc, sh, arc, sw, arc, sh, arc)
        for (i in 0 until 8) b[i + 1] = b[i] + len[i]
        total = b[8]
    }

    fun wrap(s: Float): Float {
        var v = s % total
        if (v < 0f) v += total
        return v
    }

    /** Selisih terpendek di keliling (hasil dalam -total/2..total/2). */
    fun delta(from: Float, to: Float): Float {
        var d = (to - from) % total
        if (d > total / 2f) d -= total
        if (d < -total / 2f) d += total
        return d
    }

    /**
     * Isi [o]: 0 x, 1 y (titik tempel di tepi), 2 nx, 3 ny (normal keluar), 4 bentuk (derajat),
     * 5 isi (derajat), 6 "perut sudut" 0..1 (1 di tengah sudut).
     */
    fun at(sIn: Float, o: FloatArray) {
        val s = wrap(sIn)
        var i = 0
        while (i < 7 && s >= b[i + 1]) i++
        val t = s - b[i]
        val u = if (arc > 0f) (t / arc).coerceIn(0f, 1f) else 0f
        val bell = sin(PI * u).toFloat()
        when (i) {
            0 -> set(o, rc + t, 0f, 0f, -1f, 0f, 0f, 0f)
            1 -> arcPt(o, w - rc, rc, -PI / 2 + t / rc, 90f * u, 90f * u, bell)
            2 -> set(o, w, rc + t, 1f, 0f, 90f, 90f, 0f)
            3 -> arcPt(o, w - rc, h - rc, 0.0 + t / rc, 90f + 90f * u, 90f - 90f * u, bell)
            4 -> set(o, w - rc - t, h, 0f, 1f, 180f, 0f, 0f)
            5 -> arcPt(o, rc, h - rc, PI / 2 + t / rc, 180f + 90f * u, -90f * u, bell)
            6 -> set(o, 0f, h - rc - t, -1f, 0f, 270f, -90f, 0f)
            else -> arcPt(o, rc, rc, PI + t / rc, 270f + 90f * u, -90f + 90f * u, bell)
        }
    }

    private fun set(o: FloatArray, x: Float, y: Float, nx: Float, ny: Float, a: Float, p: Float, m: Float) {
        o[0] = x; o[1] = y; o[2] = nx; o[3] = ny; o[4] = a; o[5] = p; o[6] = m
    }

    private fun arcPt(o: FloatArray, cx: Float, cy: Float, th: Double, a: Float, p: Float, m: Float) {
        val c = cos(th).toFloat()
        val s = sin(th).toFloat()
        set(o, cx + rc * c, cy + rc * s, c, s, a, p, m)
    }

    /** Titik keliling terdekat dengan (fx, fy). */
    fun nearest(fx: Float, fy: Float): Float {
        var best = Float.MAX_VALUE
        var bs = 0f
        fun cand(s: Float, d2: Float) {
            if (d2 < best) { best = d2; bs = s }
        }
        fun sq(v: Float) = v * v
        val px = fx.coerceIn(rc, w - rc)
        val py = fy.coerceIn(rc, h - rc)
        cand(px - rc, sq(fx - px) + sq(fy))
        cand(b[2] + (py - rc), sq(fx - w) + sq(fy - py))
        cand(b[4] + (w - rc - px), sq(fx - px) + sq(fy - h))
        cand(b[6] + (h - rc - py), sq(fx) + sq(fy - py))
        // bagian dalam busur sudut
        fun arcCand(seg: Int, cx: Float, cy: Float, th0: Double) {
            val ang = atan2((fy - cy).toDouble(), (fx - cx).toDouble())
            var rel = ang - th0
            while (rel > PI) rel -= 2 * PI
            while (rel < -PI) rel += 2 * PI
            if (rel in 0.0..(PI / 2)) {
                val d = hypot(fx - cx, fy - cy) - rc
                cand(b[seg] + (rel * rc).toFloat(), d * d)
            }
        }
        arcCand(1, w - rc, rc, -PI / 2)
        arcCand(3, w - rc, h - rc, 0.0)
        arcCand(5, rc, h - rc, PI / 2)
        arcCand(7, rc, rc, PI)
        return bs
    }

    private fun segCand(i: Int, fx: Float, fy: Float, o: FloatArray) {
        fun sq(v: Float) = v * v
        when (i) {
            0 -> { val px = fx.coerceIn(rc, w - rc); o[0] = px - rc; o[1] = sq(fx - px) + sq(fy) }
            2 -> { val py = fy.coerceIn(rc, h - rc); o[0] = b[2] + (py - rc); o[1] = sq(fx - w) + sq(fy - py) }
            4 -> { val px = fx.coerceIn(rc, w - rc); o[0] = b[4] + (w - rc - px); o[1] = sq(fx - px) + sq(fy - h) }
            6 -> { val py = fy.coerceIn(rc, h - rc); o[0] = b[6] + (h - rc - py); o[1] = sq(fx) + sq(fy - py) }
            else -> {
                val cx = if (i == 1 || i == 3) w - rc else rc
                val cy = if (i == 1 || i == 7) rc else h - rc
                val th0 = when (i) { 1 -> -PI / 2; 3 -> 0.0; 5 -> PI / 2; else -> PI }
                var rel = atan2((fy - cy).toDouble(), (fx - cx).toDouble()) - th0
                while (rel > PI) rel -= 2 * PI
                while (rel < -PI) rel += 2 * PI
                if (rel in 0.0..(PI / 2)) {
                    val d = hypot(fx - cx, fy - cy) - rc
                    o[0] = b[i] + (rel * rc).toFloat(); o[1] = d * d
                } else {
                    val x0 = cx + rc * cos(th0).toFloat(); val y0 = cy + rc * sin(th0).toFloat()
                    val x1 = cx + rc * cos(th0 + PI / 2).toFloat(); val y1 = cy + rc * sin(th0 + PI / 2).toFloat()
                    val d0 = sq(fx - x0) + sq(fy - y0)
                    val d1 = sq(fx - x1) + sq(fy - y1)
                    if (d0 <= d1) { o[0] = b[i]; o[1] = d0 } else { o[0] = b[i + 1]; o[1] = d1 }
                }
            }
        }
    }

    private val sc1 = FloatArray(2)

    /**
     * Seperti [nearest], tetapi bertahan di sisi/sudut sekarang ([curS]) kecuali sisi lain lebih dekat
     * dengan selisih lebih dari [margin]. Mencegah notch lompat/berputar saat jari melintas di tengah layar.
     */
    fun nearestSticky(fx: Float, fy: Float, curS: Float, margin: Float): Float {
        val n = nearest(fx, fy)
        val s = wrap(curS)
        var i = 0
        while (i < 7 && s >= b[i + 1]) i++
        segCand(i, fx, fy, sc1)
        val dc = Math.sqrt(sc1[1].toDouble()).toFloat()
        // jarak titik n
        at(n, tmpA)
        val dn = hypot(fx - tmpA[0], fy - tmpA[1])
        return if (dc <= dn + margin) sc1[0] else n
    }

    private val tmpA = FloatArray(8)

    /** s untuk posisi diam: [edge] (0 atas, 1 kanan, 2 bawah, 3 kiri) dan [frac] 0..1 sepanjang sisi lurusnya. */
    fun restToS(edge: Int, frac: Float): Float {
        val seg = edge * 2
        val len = b[seg + 1] - b[seg]
        return b[seg] + frac.coerceIn(0f, 1f) * len
    }

    /** Titik diam terdekat (hanya di sisi lurus). Mengembalikan s yang sudah dipaskan. */
    fun snap(sIn: Float): Float {
        val s = wrap(sIn)
        var i = 0
        while (i < 7 && s >= b[i + 1]) i++
        if (i % 2 == 0) return s
        // di busur sudut: pilih ujung sisi lurus yang lebih dekat
        val mid = (b[i] + b[i + 1]) / 2f
        return if (s < mid) b[i] else wrap(b[i + 1])
    }

    /** Ubah s (yang sudah di sisi lurus) menjadi edge + frac. */
    fun toRest(sIn: Float): Pair<Int, Float> {
        val s = snap(sIn)
        var i = 0
        while (i < 7 && s >= b[i + 1]) i++
        val seg = if (i % 2 == 0) i else i - 1
        val len = (b[seg + 1] - b[seg]).coerceAtLeast(1f)
        return (seg / 2) to ((s - b[seg]) / len).coerceIn(0f, 1f)
    }

    fun speedAbs(v: Float) = abs(v)
}
