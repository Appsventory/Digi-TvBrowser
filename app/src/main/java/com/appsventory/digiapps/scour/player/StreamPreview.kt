package com.appsventory.digiapps.scour.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** What the stream list shows besides the URL. Missing pieces: thumbnail null, numbers <= 0. */
data class StreamPreview(
    val thumbnail: Bitmap?,
    val durationMs: Long,
    val sizeBytes: Long
)

object StreamPreviewLoader {

    private const val THUMB_WIDTH = 224
    private const val THUMB_TIME_US = 1_000_000L
    private const val HLS_SEGMENT_MAX_BYTES = 2 * 1024 * 1024
    private const val PARALLEL = 2

    private val gate = Semaphore(PARALLEL)

    suspend fun load(cacheDir: File, stream: DetectedStream): StreamPreview =
        gate.withPermit {
            withContext(Dispatchers.IO) {
                val size = if (stream.type == StreamType.PROGRESSIVE) {
                    runCatching { sizeOf(stream) }.getOrDefault(-1L)
                } else -1L
                val frame = runCatching { frameOf(cacheDir, stream) }.getOrNull()
                StreamPreview(frame?.first, frame?.second ?: 0L, size)
            }
        }

    /** Content-Length via a 1-byte ranged GET (many CDNs reject HEAD). */
    private fun sizeOf(stream: DetectedStream): Long {
        val conn = URL(stream.url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = PlayerConfig.NETWORK_TIMEOUT_MS
            conn.readTimeout = PlayerConfig.NETWORK_TIMEOUT_MS
            conn.setRequestProperty("Range", "bytes=0-0")
            applyIdentity(conn, stream, stream.url)
            val total = conn.getHeaderField("Content-Range")?.substringAfter('/')?.toLongOrNull()
            return total ?: conn.contentLengthLong
        } finally {
            conn.disconnect()
        }
    }

    private fun applyIdentity(conn: HttpURLConnection, stream: DetectedStream, url: String) {
        stream.userAgent?.let { conn.setRequestProperty("User-Agent", it) }
        stream.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        cookieOf(url)?.let { conn.setRequestProperty("Cookie", it) }
    }

    private fun cookieOf(url: String): String? =
        runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Thumbnail + duration. HLS: decode the first downloaded segment locally (no network hangs). */
    private suspend fun frameOf(cacheDir: File, stream: DetectedStream): Pair<Bitmap?, Long>? {
        val retriever = MediaMetadataRetriever()
        var temp: File? = null
        try {
            when (stream.type) {
                StreamType.PROGRESSIVE -> {
                    val headers = HashMap<String, String>(stream.headers)
                    stream.userAgent?.let { headers["User-Agent"] = it }
                    cookieOf(stream.url)?.let { headers["Cookie"] = it }
                    retriever.setDataSource(stream.url, headers)
                }
                StreamType.HLS -> {
                    val urls = ManifestProbe.firstSegmentUrls(stream) ?: return null
                    temp = File.createTempFile("thumb", ".bin", cacheDir)
                    temp.outputStream().use { out ->
                        urls.forEachIndexed { i, u ->
                            val max = if (i == urls.lastIndex) HLS_SEGMENT_MAX_BYTES else PlayerConfig.PROBE_MAX_BYTES
                            out.write(StreamHttp.readBytes(u, stream, max))
                        }
                    }
                    retriever.setDataSource(temp.absolutePath)
                }
                StreamType.DASH -> return null
            }
            val durationMs = if (stream.type == StreamType.PROGRESSIVE) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } else 0L
            val raw = retriever.getFrameAtTime(THUMB_TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            val thumb = raw?.let {
                val h = (THUMB_WIDTH.toFloat() * it.height / it.width).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(it, THUMB_WIDTH, h, true)
            }
            return thumb to durationMs
        } finally {
            runCatching { retriever.release() }
            temp?.delete()
        }
    }
}
