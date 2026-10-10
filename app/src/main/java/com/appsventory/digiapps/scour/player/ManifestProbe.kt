package com.appsventory.digiapps.scour.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Reads an HLS / DASH manifest just enough to tell a feature video from a 15-second ad:
 * total duration, tallest rendition, live or not. Never throws; null = could not tell.
 */
object ManifestProbe {

    private val RESOLUTION = Regex("RESOLUTION=\\d+x(\\d+)")
    private val ISO_DURATION =
        Regex("PT(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?")
    private val MAP_URI = Regex("#EXT-X-MAP:[^\\n]*URI=\"([^\"]+)\"")
    private val DASH_DURATION = Regex("mediaPresentationDuration=\"([^\"]+)\"")
    private val DASH_HEIGHT = Regex("height=\"(\\d+)\"")

    suspend fun probe(stream: DetectedStream): StreamInfo? = withContext(Dispatchers.IO) {
        runCatching {
            when (stream.type) {
                StreamType.HLS -> probeHls(stream)
                StreamType.DASH -> probeDash(stream)
                StreamType.PROGRESSIVE -> null
            }
        }.getOrNull()
    }

    private fun fetch(url: String, stream: DetectedStream) =
        StreamHttp.readText(url, stream, PlayerConfig.PROBE_MAX_BYTES)

    /** Init segment (fMP4, if any) + first media segment of the first variant; null for non-HLS / failure. */
    suspend fun firstSegmentUrls(stream: DetectedStream): List<String>? = withContext(Dispatchers.IO) {
        if (stream.type != StreamType.HLS) return@withContext null
        runCatching {
            var base = stream.url
            var text = fetch(base, stream)
            if (text.contains("#EXT-X-STREAM-INF")) {
                val lines = text.lines()
                val i = lines.indexOfFirst { it.startsWith("#EXT-X-STREAM-INF") }
                val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim()
                    ?: return@runCatching null
                base = URI(base).resolve(uri).toString()
                text = fetch(base, stream)
            }
            if (text.contains("#EXT-X-KEY") && !text.contains("METHOD=NONE")) return@runCatching null
            val seg = text.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim()
                ?: return@runCatching null
            val init = MAP_URI.find(text)?.groupValues?.get(1)
            listOfNotNull(init, seg).map { URI(base).resolve(it).toString() }
        }.getOrNull()
    }

    private fun probeHls(stream: DetectedStream): StreamInfo {
        val master = fetch(stream.url, stream)
        if (!master.contains("#EXT-X-STREAM-INF")) {
            return hlsMediaInfo(master, height = 0)
        }
        // Master playlist: tallest variant for the label, first variant for the duration.
        val lines = master.lines()
        var tallest = 0
        var firstUri: String? = null
        for (i in lines.indices) {
            if (!lines[i].startsWith("#EXT-X-STREAM-INF")) continue
            tallest = maxOf(tallest, RESOLUTION.find(lines[i])?.groupValues?.get(1)?.toIntOrNull() ?: 0)
            if (firstUri == null) {
                firstUri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim()
            }
        }
        val media = firstUri?.let { fetch(URI(stream.url).resolve(it).toString(), stream) }
            ?: return StreamInfo(0L, tallest, live = false)
        return hlsMediaInfo(media, tallest)
    }

    private fun hlsMediaInfo(playlist: String, height: Int): StreamInfo {
        val ended = playlist.contains("#EXT-X-ENDLIST")
        var seconds = 0.0
        for (line in playlist.lineSequence()) {
            if (!line.startsWith("#EXTINF:")) continue
            seconds += line.removePrefix("#EXTINF:").substringBefore(',').trim().toDoubleOrNull() ?: 0.0
        }
        return StreamInfo(
            durationMs = if (ended) (seconds * 1000).toLong() else 0L,
            height = height,
            live = !ended
        )
    }

    private fun probeDash(stream: DetectedStream): StreamInfo {
        val mpd = fetch(stream.url, stream)
        val duration = DASH_DURATION.find(mpd)?.groupValues?.get(1)?.let(::parseIsoDurationMs) ?: 0L
        val height = DASH_HEIGHT.findAll(mpd).maxOfOrNull { it.groupValues[1].toInt() } ?: 0
        return StreamInfo(duration, height, live = mpd.contains("type=\"dynamic\""))
    }

    private fun parseIsoDurationMs(iso: String): Long {
        val m = ISO_DURATION.matchEntire(iso) ?: return 0L
        fun part(i: Int) = m.groupValues[i].toDoubleOrNull() ?: 0.0
        return ((part(1) * 3600 + part(2) * 60 + part(3)) * 1000).toLong()
    }
}
