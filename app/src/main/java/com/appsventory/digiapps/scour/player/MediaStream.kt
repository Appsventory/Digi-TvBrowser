package com.appsventory.digiapps.scour.player

import android.net.Uri
import java.util.Locale

enum class StreamType { HLS, DASH, PROGRESSIVE }

enum class SubtitleFormat { VTT, SRT }

/** Facts read from the manifest. [durationMs] 0 = unknown (live / not declared). */
data class StreamInfo(
    val durationMs: Long,
    val height: Int,
    val live: Boolean
)

/**
 * A playable stream seen by the WebView.
 * [headers] holds only what the CDN may require (Referer / Origin); cookies are added per
 * request from the WebView cookie jar at playback time (see [PlayerFactory]).
 */
data class DetectedStream(
    val url: String,
    val type: StreamType,
    val title: String,
    val userAgent: String?,
    val headers: Map<String, String>,
    /** Filled in by [ManifestProbe]; null while pending or when the manifest could not be read. */
    val info: StreamInfo? = null,
    /** False while the manifest probe is still running. */
    val probed: Boolean = false
)

/** A side-loadable subtitle file (.vtt / .srt) seen on the page. */
data class DetectedSubtitle(
    val url: String,
    val format: SubtitleFormat,
    val language: String?
)

/** What the browser hands to the player screen. */
data class PlayerRequest(
    val stream: DetectedStream,
    val subtitles: List<DetectedSubtitle>,
    /** Every stream seen on the page, best guess first — lets the user switch inside the player. */
    val streams: List<DetectedStream>
)

sealed interface MediaHit {
    data class Stream(val url: String, val type: StreamType) : MediaHit
    data class Subtitle(val url: String, val format: SubtitleFormat, val language: String?) : MediaHit
}

/** URL-based detection used from WebViewClient.shouldInterceptRequest (background thread safe). */
object MediaStreamDetector {

    private val PROGRESSIVE_EXTENSIONS = setOf("mp4", "m4v", "webm", "mkv", "mov", "ogv", "3gp")

    /** HLS-embedded WebVTT chunks (seg-12.vtt, 0001.vtt …) are handled by ExoPlayer itself. */
    private val SEGMENT_NAME = Regex("(seg|segment|chunk|frag|fragment)[-_]?\\d+")

    private val languageByEnglishName: Map<String, String> by lazy {
        Locale.getISOLanguages().associateBy {
            Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH).lowercase(Locale.ROOT)
        }
    }

    fun classify(uri: Uri): MediaHit? {
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        val path = uri.path?.lowercase(Locale.ROOT) ?: return null
        val fileName = path.substringAfterLast('/')
        val ext = fileName.substringAfterLast('.', "")
        val url = uri.toString()
        return when {
            ext == "m3u8" -> MediaHit.Stream(url, StreamType.HLS)
            ext == "mpd" -> MediaHit.Stream(url, StreamType.DASH)
            ext in PROGRESSIVE_EXTENSIONS -> MediaHit.Stream(url, StreamType.PROGRESSIVE)
            ext == "vtt" || ext == "srt" -> {
                val base = fileName.substringBeforeLast('.')
                if (isSegmentName(base)) null
                else MediaHit.Subtitle(
                    url = url,
                    format = if (ext == "vtt") SubtitleFormat.VTT else SubtitleFormat.SRT,
                    language = guessLanguage(base)
                )
            }
            else -> null
        }
    }

    /** <video>/<source> URL read from the DOM: unknown extension still means "a video file". */
    fun classifyMediaElement(url: String): MediaHit.Stream? {
        val uri = Uri.parse(url)
        (classify(uri) as? MediaHit.Stream)?.let { return it }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        return if (scheme == "http" || scheme == "https") MediaHit.Stream(url, StreamType.PROGRESSIVE) else null
    }

    /** <track src srclang> — any http(s) URL is accepted; format from extension, WebVTT by default. */
    fun classifyTrack(url: String, lang: String): MediaHit.Subtitle? {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        val srt = uri.path?.lowercase(Locale.ROOT)?.endsWith(".srt") == true
        val code = lang.substringBefore('-').lowercase(Locale.ROOT).takeIf { it.length in 2..3 }
        return MediaHit.Subtitle(url, if (srt) SubtitleFormat.SRT else SubtitleFormat.VTT, code)
    }

    private fun isSegmentName(base: String): Boolean =
        base.isNotEmpty() && (base.all { it.isDigit() } || SEGMENT_NAME.containsMatchIn(base))

    /** "movie.en" / "subs_indonesian" → ISO code; null when nothing recognisable. */
    private fun guessLanguage(base: String): String? {
        val token = base.split('.', '_', '-', ' ').lastOrNull { it.isNotBlank() }
            ?.lowercase(Locale.ROOT) ?: return null
        if (token.length == 2 && token in Locale.getISOLanguages()) return token
        return languageByEnglishName[token]
    }

    private fun Map<String, String>.header(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.takeIf { it.isNotBlank() }

    /** Referer (falls back to the page URL) and Origin, as the original request sent them. */
    fun forwardedHeaders(requestHeaders: Map<String, String>, pageUrl: String?): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        (requestHeaders.header("Referer") ?: pageUrl?.takeIf { it.isNotBlank() })
            ?.let { out["Referer"] = it }
        // WebView often omits Origin; browsers send the Referer's origin on media fetches.
        (requestHeaders.header("Origin") ?: out["Referer"]?.let { originOf(it) })
            ?.let { out["Origin"] = it }
        return out
    }

    private fun originOf(url: String): String? = runCatching {
        val u = java.net.URI(url)
        if (u.scheme == null || u.host == null) null
        else u.scheme + "://" + u.host + (if (u.port != -1) ":" + u.port else "")
    }.getOrNull()

    fun userAgentOf(requestHeaders: Map<String, String>): String? = requestHeaders.header("User-Agent")
}
