package id.nusa.notch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Lirik tersinkron dari LRCLIB (https://lrclib.net). */
object Lyrics {

    fun watch(scope: CoroutineScope) {
        scope.launch {
            Store.media
                .map { m -> m?.let { Triple(it.title, it.artist, it.durationMs / 1000) } }
                .distinctUntilChanged()
                .collectLatest { key ->
                    if (key == null) {
                        Store.lyrics.value = LyricsState.None
                        return@collectLatest
                    }
                    Store.lyrics.value = LyricsState.Loading
                    val lines = withContext(Dispatchers.IO) { fetch(key.first, key.second, key.third) }
                    Store.lyrics.value =
                        if (lines.isNullOrEmpty()) LyricsState.Missing else LyricsState.Found(lines)
                }
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun fetch(title: String, artist: String, durSec: Long): List<LrcLine>? {
        try {
            if (artist.isNotBlank()) {
                val dur = if (durSec > 0) "&duration=$durSec" else ""
                val r = http("https://lrclib.net/api/get?track_name=${enc(title)}&artist_name=${enc(artist)}$dur")
                if (r != null) parseSynced(JSONObject(r))?.let { return it }

                val s1 = http("https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}")
                if (s1 != null) fromSearch(s1)?.let { return it }
            }
            val q = if (artist.isNotBlank()) "$title $artist" else title
            val s2 = http("https://lrclib.net/api/search?q=${enc(q)}")
            if (s2 != null) fromSearch(s2)?.let { return it }
        } catch (e: Exception) {
            // jaringan/parsing gagal -> dianggap tidak ada
        }
        return null
    }

    private fun fromSearch(body: String): List<LrcLine>? {
        val arr = JSONArray(body)
        for (i in 0 until arr.length()) {
            parseSynced(arr.getJSONObject(i))?.let { return it }
        }
        return null
    }

    private fun parseSynced(o: JSONObject): List<LrcLine>? {
        if (o.isNull("syncedLyrics")) return null
        val lines = parseLrc(o.getString("syncedLyrics"))
        return lines.ifEmpty { null }
    }

    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parseLrc(text: String): List<LrcLine> {
        val out = ArrayList<LrcLine>()
        for (raw in text.lineSequence()) {
            val ms = tag.findAll(raw).toList()
            if (ms.isEmpty()) continue
            val body = raw.substring(ms.last().range.last + 1).trim()
            if (body.isEmpty()) continue
            for (m in ms) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val fr = m.groupValues[3]
                val f = if (fr.isEmpty()) 0L else fr.padEnd(3, '0').take(3).toLong()
                out.add(LrcLine((min * 60 + sec) * 1000 + f, body))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun http(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "Nusa-Android/1.0")
            if (c.responseCode != 200) null else c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
