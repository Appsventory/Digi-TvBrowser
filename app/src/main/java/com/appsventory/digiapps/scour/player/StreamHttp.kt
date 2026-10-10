package com.appsventory.digiapps.scour.player

import android.webkit.CookieManager
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Tiny HTTP GET that replays the stream's identity (UA / Referer / Origin / cookies). */
object StreamHttp {

    fun readBytes(url: String, stream: DetectedStream, maxBytes: Int): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = PlayerConfig.NETWORK_TIMEOUT_MS
            conn.readTimeout = PlayerConfig.NETWORK_TIMEOUT_MS
            stream.userAgent?.let { conn.setRequestProperty("User-Agent", it) }
            stream.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { conn.setRequestProperty("Cookie", it) }

            val out = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > maxBytes) break
                }
            }
            return out.toByteArray()
        } finally {
            conn.disconnect()
        }
    }

    fun readText(url: String, stream: DetectedStream, maxBytes: Int): String =
        String(readBytes(url, stream, maxBytes), Charsets.UTF_8)
}
