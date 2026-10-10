package com.appsventory.digiapps.scour.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One downloadable APK attached to a release. */
data class ReleaseAsset(val name: String, val url: String, val sizeBytes: Long)

/** Latest GitHub release. [assets] holds the APKs only (universal build first); may be empty. */
data class ReleaseInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val publishedAt: String,
    val pageUrl: String,
    val assets: List<ReleaseAsset>
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Failed : UpdateStatus
    data class UpToDate(val release: ReleaseInfo) : UpdateStatus
    data class Available(val release: ReleaseInfo) : UpdateStatus
}

/** The release behind this status, when one is known. */
val UpdateStatus.release: ReleaseInfo?
    get() = when (this) {
        is UpdateStatus.Available -> release
        is UpdateStatus.UpToDate -> release
        else -> null
    }

object UpdateChecker {

    private val DIGITS = Regex("\\d+")
    private const val ISO_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    /** null on any network / parse failure. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(BrowserConfig.UPDATE_API_URL).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = BrowserConfig.UPDATE_TIMEOUT_MS
                conn.readTimeout = BrowserConfig.UPDATE_TIMEOUT_MS
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "DigiBrowser")
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun parse(json: String): ReleaseInfo {
        val o = JSONObject(json)
        val tag = o.optString("tag_name")
        val assets = o.optJSONArray("assets")
        val apks = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
            .map {
                ReleaseAsset(
                    name = it.optString("name"),
                    url = it.optString("browser_download_url"),
                    sizeBytes = it.optLong("size")
                )
            }
            // Universal build (TV + phone) first, then alphabetical.
            .sortedWith(compareByDescending<ReleaseAsset> { a -> a.name.contains("universal", ignoreCase = true) }
                .thenBy { a -> a.name })
        return ReleaseInfo(
            tag = tag,
            title = o.optString("name").ifBlank { tag },
            notes = o.optString("body"),
            publishedAt = o.optString("published_at"),
            pageUrl = o.optString("html_url").ifBlank { BrowserConfig.UPDATE_RELEASES_URL },
            assets = apks
        )
    }

    /** "v1.2.0" vs "1.1" → compares numeric parts, missing parts count as 0. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = numbers(latest)
        val b = numbers(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(v: String): List<Int> =
        DIGITS.findAll(v.substringBefore('-')).map { it.value.toIntOrNull() ?: 0 }.toList()

    fun evaluate(release: ReleaseInfo?, currentVersion: String): UpdateStatus = when {
        release == null -> UpdateStatus.Failed
        isNewer(release.tag, currentVersion) -> UpdateStatus.Available(release)
        else -> UpdateStatus.UpToDate(release)
    }

    /** "2026-10-02T17:27:00Z" → localized medium date; the raw text if it cannot be parsed. */
    fun formatDate(iso: String): String = runCatching {
        val parser = SimpleDateFormat(ISO_UTC, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(parser.parse(iso)!!)
    }.getOrDefault(iso)
}
