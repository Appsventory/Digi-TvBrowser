package com.appsventory.digiapps.scour.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SubtitleCue(val startMs: Long, val endMs: Long, val text: String)

/** Minimal SRT + WebVTT parser (both share the "start --> end" block layout). */
object SubtitleParser {

    private val TIME = Regex("(?:(\\d+):)?(\\d{1,2}):(\\d{2})[.,](\\d{1,3})")
    private val TAGS = Regex("<[^>]+>|\\{\\\\[^}]*\\}")

    fun parse(raw: String): List<SubtitleCue> {
        val text = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val cues = ArrayList<SubtitleCue>()
        for (block in text.split("\n\n")) {
            val lines = block.lines()
            val timingIdx = lines.indexOfFirst { it.contains("-->") }
            if (timingIdx < 0) continue
            val times = TIME.findAll(lines[timingIdx]).toList()
            if (times.size < 2) continue
            val body = lines.drop(timingIdx + 1)
                .joinToString("\n") { clean(it) }
                .trim()
            if (body.isEmpty()) continue
            cues += SubtitleCue(toMs(times[0]), toMs(times[1]), body)
        }
        cues.sortBy { it.startMs }
        return cues
    }

    private fun clean(line: String): String =
        TAGS.replace(line, "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()

    private fun toMs(m: MatchResult): Long {
        val h = m.groupValues[1].toLongOrNull() ?: 0L
        val min = m.groupValues[2].toLong()
        val s = m.groupValues[3].toLong()
        val ms = m.groupValues[4].padEnd(3, '0').take(3).toLong()
        return ((h * 60 + min) * 60 + s) * 1000 + ms
    }

    /** Text of whatever cue(s) cover [positionMs]; null when none. */
    fun activeText(cues: List<SubtitleCue>, positionMs: Long): String? {
        if (cues.isEmpty() || positionMs < 0) return null
        var lo = 0
        var hi = cues.lastIndex
        var idx = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].startMs <= positionMs) { idx = mid; lo = mid + 1 } else hi = mid - 1
        }
        if (idx < 0) return null
        val active = (idx downTo maxOf(0, idx - 3))
            .map { cues[it] }
            .filter { it.endMs > positionMs }
            .map { it.text }
            .distinct()
        return active.takeIf { it.isNotEmpty() }?.asReversed()?.joinToString("\n")
    }
}

object SubtitleLoader {
    /** Downloads and parses [subtitle] with the same identity (UA / Referer / cookies) as the stream. */
    suspend fun load(subtitle: DetectedSubtitle, stream: DetectedStream): List<SubtitleCue> =
        withContext(Dispatchers.IO) {
            SubtitleParser.parse(
                StreamHttp.readText(subtitle.url, stream, PlayerConfig.SUBTITLE_MAX_BYTES)
            )
        }
}
