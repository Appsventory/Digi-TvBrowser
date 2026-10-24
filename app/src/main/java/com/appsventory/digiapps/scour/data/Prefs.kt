package com.appsventory.digiapps.scour.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class SearchEngine(val id: String, val label: String, val queryUrl: String) {
    Google("google", "Google", "https://www.google.com/search?q="),
    Bing("bing", "Bing", "https://www.bing.com/search?q="),
    Yahoo("yahoo", "Yahoo", "https://search.yahoo.com/search?p="),
    DuckDuckGo("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=");

    companion object {
        fun fromId(id: String): SearchEngine =
            entries.find { it.id == id } ?: Google
    }
}

enum class PrivateDnsProvider(
    val id: String,
    val label: String,
    val host: String,
    val description: String
) {
    Off("off", "Off (System Default)", "", "Uses default system network DNS"),
    AdGuard("adguard", "AdGuard DNS", "dns.adguard-dns.com", "Blocks ads, malicious domains & trackers"),
    Cloudflare("cloudflare", "Cloudflare (1.1.1.1)", "one.one.one.one", "Fast, privacy-focused public DNS"),
    Google("google", "Google DNS (8.8.8.8)", "dns.google", "High performance global DNS"),
    Quad9("quad9", "Quad9 DNS", "dns.quad9.net", "Blocks malware & phishing domains");

    companion object {
        fun fromId(id: String): PrivateDnsProvider =
            entries.find { it.id == id } ?: Off
    }
}

/** User-Agent selection mode. Default uses the WebView engine's real default string. */
/**
 * Browser-specific User-Agent presets.
 *
 * Each non-Default preset maps to an explicit, centralized UA string in [UserAgents].
 * Default always restores the WebView engine's real default via
 * [android.webkit.WebSettings.getDefaultUserAgent].
 *
 * These strings influence website browser detection only. They do not change the
 * underlying Chromium/WebView engine, Client Hints, navigator.userAgentData,
 * rendering behavior, or platform capabilities.
 */
enum class UserAgentPreset(
    val id: String,
    val brand: String,
    val label: String
) {
    Default("default", "Default", "Default"),
    ChromeDesktop("chrome_desktop", "Google Chrome", "Chrome Desktop"),
    ChromeTablet("chrome_tablet", "Google Chrome", "Chrome Tablet"),
    ChromeMobile("chrome_mobile", "Google Chrome", "Chrome Mobile"),
    FirefoxDesktop("firefox_desktop", "Mozilla Firefox", "Firefox Desktop"),
    FirefoxTablet("firefox_tablet", "Mozilla Firefox", "Firefox Tablet"),
    FirefoxMobile("firefox_mobile", "Mozilla Firefox", "Firefox Mobile"),
    EdgeDesktop("edge_desktop", "Microsoft Edge", "Edge Desktop"),
    SafariDesktop("safari_desktop", "Apple Safari", "Safari Desktop"),
    AppleTv("apple_tv", "Apple TV", "Apple TV Browser");

    companion object {
        fun fromId(id: String): UserAgentPreset =
            entries.find { it.id == id } ?: Default

        /** Brand groups for UI presentation (Default is standalone). */
        val brandGroups: List<Pair<String, List<UserAgentPreset>>> = listOf(
            "Google Chrome" to listOf(ChromeDesktop, ChromeTablet, ChromeMobile),
            "Mozilla Firefox" to listOf(FirefoxDesktop, FirefoxTablet, FirefoxMobile),
            "Microsoft Edge" to listOf(EdgeDesktop),
            "Apple Safari" to listOf(SafariDesktop),
            "Apple TV" to listOf(AppleTv)
        )
    }
}

data class BookmarkEntry(
    val title: String,
    val url: String,
    val colorHex: Long = BrowserConfig.DEFAULT_SHORTCUT_COLOR
)

data class ShortcutEntry(
    val title: String,
    val url: String,
    val colorHex: Long = BrowserConfig.DEFAULT_SHORTCUT_COLOR,
    val iconEmoji: String = ""
)

/** Persisted browsing history row. */
data class HistoryEntry(
    val title: String,
    val url: String,
    val timeLabel: String
)

/** Persisted download row. */
data class DownloadEntry(
    val id: Long = 0L,
    val name: String,
    val sizeLabel: String,
    val status: String,
    val progress: Float = 1f
)

object Prefs {
    private const val NAME = "digi_browser_prefs"
    private const val KEY_ENGINE = "search_engine"
    private const val KEY_DESKTOP = "desktop_mode"
    private const val KEY_UA_PRESET = "ua_preset"
    private const val KEY_UA_MODE = "ua_mode" // legacy key, migrated once
    private const val KEY_BOOKMARKS = "bookmarks_json"
    private const val KEY_SHORTCUTS = "shortcuts_json"
    private const val KEY_HISTORY = "history_json"
    private const val KEY_DOWNLOADS = "downloads_json"
    private const val KEY_SESSION_URL = "session_url"
    private const val KEY_TABS = "tabs_json"
    private const val KEY_ACTIVE_TAB = "active_tab_id"
    private const val KEY_ADBLOCK = "adblock_enabled"
    private const val KEY_STRONG_ADBLOCK = "strong_adblock_enabled"
    private const val KEY_JS = "javascript_enabled"
    private const val KEY_BLOCK_POPUPS = "block_popups"
    private const val KEY_SAVE_HISTORY = "save_history"
    private const val KEY_PRIVATE_DNS = "private_dns"
    private const val KEY_VIDEO_RESUME = "video_resume_json"
    private const val KEY_CURSOR_HOLD_SPEED = "cursor_hold_speed"
    private const val MAX_HISTORY = 200
    private const val MAX_DOWNLOADS = 100
    private const val MAX_VIDEO_RESUME = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getSearchEngine(context: Context): SearchEngine {
        val id = prefs(context).getString(KEY_ENGINE, SearchEngine.Google.id)
            ?: SearchEngine.Google.id
        return SearchEngine.fromId(id)
    }

    fun setSearchEngine(context: Context, engine: SearchEngine) {
        prefs(context).edit().putString(KEY_ENGINE, engine.id).apply()
    }

    /** Legacy helper — true when a desktop-oriented preset is active. */
    fun isDesktopMode(context: Context): Boolean {
        val p = getUserAgentPreset(context)
        return p == UserAgentPreset.ChromeDesktop ||
            p == UserAgentPreset.FirefoxDesktop ||
            p == UserAgentPreset.EdgeDesktop ||
            p == UserAgentPreset.SafariDesktop
    }

    fun getUserAgentPreset(context: Context): UserAgentPreset {
        val p = prefs(context)
        val stored = p.getString(KEY_UA_PRESET, null)
        if (stored != null) return UserAgentPreset.fromId(stored)

        // Migrate from previous generic ua_mode values
        when (p.getString(KEY_UA_MODE, null)) {
            "desktop" -> return UserAgentPreset.ChromeDesktop
            "mobile" -> return UserAgentPreset.ChromeMobile
            "default", "custom" -> return UserAgentPreset.Default
        }
        // Migrate from original desktop_mode boolean
        return if (p.getBoolean(KEY_DESKTOP, false)) {
            UserAgentPreset.ChromeDesktop
        } else {
            UserAgentPreset.Default
        }
    }

    fun setUserAgentPreset(context: Context, preset: UserAgentPreset) {
        prefs(context).edit()
            .putString(KEY_UA_PRESET, preset.id)
            .putBoolean(KEY_DESKTOP, isDesktopOriented(preset))
            .apply()
    }

    private fun isDesktopOriented(preset: UserAgentPreset): Boolean =
        preset == UserAgentPreset.ChromeDesktop ||
            preset == UserAgentPreset.FirefoxDesktop ||
            preset == UserAgentPreset.EdgeDesktop ||
            preset == UserAgentPreset.SafariDesktop

    /**
     * Resolves the effective User-Agent string for the given preset.
     * Default always uses the WebView engine's real default (never invented).
     */
    fun resolveUserAgent(
        context: Context,
        preset: UserAgentPreset = getUserAgentPreset(context)
    ): String = UserAgents.forPreset(context, preset)

    // —— Bookmarks ——
    fun getBookmarks(context: Context): List<BookmarkEntry> {
        val raw = prefs(context).getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BookmarkEntry(
                            title = o.getString("title"),
                            url = o.getString("url"),
                            colorHex = o.optLong("color", BrowserConfig.DEFAULT_SHORTCUT_COLOR)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setBookmarks(context: Context, items: List<BookmarkEntry>) {
        val arr = JSONArray()
        items.forEach { b ->
            arr.put(
                JSONObject()
                    .put("title", b.title)
                    .put("url", b.url)
                    .put("color", b.colorHex)
            )
        }
        prefs(context).edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun addBookmark(context: Context, entry: BookmarkEntry): Boolean {
        val current = getBookmarks(context)
        if (current.any { it.url == entry.url }) return false
        setBookmarks(context, listOf(entry) + current)
        return true
    }

    fun isBookmarked(context: Context, url: String): Boolean =
        getBookmarks(context).any { it.url == url }

    fun removeBookmark(context: Context, url: String): Boolean {
        val current = getBookmarks(context)
        val next = current.filterNot { it.url == url }
        if (next.size == current.size) return false
        setBookmarks(context, next)
        return true
    }

    /** Add if missing, remove if present. Returns true if now bookmarked. */
    fun toggleBookmark(context: Context, entry: BookmarkEntry): Boolean {
        return if (isBookmarked(context, entry.url)) {
            removeBookmark(context, entry.url)
            false
        } else {
            addBookmark(context, entry)
            true
        }
    }

    // —— Shortcuts ——
    fun getShortcuts(context: Context): List<ShortcutEntry> {
        val raw = prefs(context).getString(KEY_SHORTCUTS, null)
        if (raw.isNullOrBlank()) {
            val seed = defaultQuickLinks.map {
                ShortcutEntry(it.title, it.url, it.colorHex)
            }
            setShortcuts(context, seed)
            return seed
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ShortcutEntry(
                            title = o.getString("title"),
                            url = o.getString("url"),
                            colorHex = o.optLong("color", BrowserConfig.DEFAULT_SHORTCUT_COLOR),
                            iconEmoji = o.optString("icon", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setShortcuts(context: Context, items: List<ShortcutEntry>) {
        val arr = JSONArray()
        items.forEach { s ->
            arr.put(
                JSONObject()
                    .put("title", s.title)
                    .put("url", s.url)
                    .put("color", s.colorHex)
                    .put("icon", s.iconEmoji)
            )
        }
        prefs(context).edit().putString(KEY_SHORTCUTS, arr.toString()).apply()
    }

    // —— History (persist across process death) ——
    fun getHistory(context: Context): List<HistoryEntry> {
        val raw = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HistoryEntry(
                            title = o.getString("title"),
                            url = o.getString("url"),
                            timeLabel = o.optString("time", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setHistory(context: Context, items: List<HistoryEntry>) {
        val trimmed = items.take(MAX_HISTORY)
        val arr = JSONArray()
        trimmed.forEach { h ->
            arr.put(
                JSONObject()
                    .put("title", h.title)
                    .put("url", h.url)
                    .put("time", h.timeLabel)
            )
        }
        prefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    // —— Downloads ——
    fun getDownloads(context: Context): List<DownloadEntry> {
        val raw = prefs(context).getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        DownloadEntry(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            sizeLabel = o.optString("size", "—"),
                            status = o.optString("status", DownloadStatus.COMPLETED),
                            progress = o.optDouble("progress", 1.0).toFloat()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setDownloads(context: Context, items: List<DownloadEntry>) {
        val trimmed = items.take(MAX_DOWNLOADS)
        val arr = JSONArray()
        trimmed.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("size", d.sizeLabel)
                    .put("status", d.status)
                    .put("progress", d.progress.toDouble())
            )
        }
        prefs(context).edit().putString(KEY_DOWNLOADS, arr.toString()).apply()
    }
    // —— Tabs (saved so they survive app exit / new WebView) ——
    fun getTabs(context: Context): List<BrowserTab> {
        val raw = prefs(context).getString(KEY_TABS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val url = o.optString("url")
                if (url.isBlank()) null
                else BrowserTab(o.getLong("id"), url, o.optString("title"))
            }
        }.getOrDefault(emptyList())
    }

    fun getActiveTabId(context: Context): Long =
        prefs(context).getLong(KEY_ACTIVE_TAB, -1L)

    fun setTabs(context: Context, tabs: List<BrowserTab>, activeId: Long) {
        val arr = JSONArray()
        tabs.forEach {
            arr.put(JSONObject().put("id", it.id).put("url", it.url).put("title", it.title))
        }
        prefs(context).edit()
            .putString(KEY_TABS, arr.toString())
            .putLong(KEY_ACTIVE_TAB, activeId)
            .apply()
    }

    // —— Video resume positions (newest first, capped) ——
    private fun readResume(context: Context): MutableList<Pair<String, Long>> {
        val raw = prefs(context).getString(KEY_VIDEO_RESUME, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val key = o.optString("k")
                if (key.isBlank()) null else key to o.optLong("p")
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun writeResume(context: Context, items: List<Pair<String, Long>>) {
        val arr = JSONArray()
        items.take(MAX_VIDEO_RESUME).forEach { (k, p) ->
            arr.put(JSONObject().put("k", k).put("p", p))
        }
        prefs(context).edit().putString(KEY_VIDEO_RESUME, arr.toString()).apply()
    }

    fun getResumePosition(context: Context, key: String): Long =
        readResume(context).firstOrNull { it.first == key }?.second ?: 0L

    fun setResumePosition(context: Context, key: String, positionMs: Long) {
        val items = readResume(context)
        items.removeAll { it.first == key }
        items.add(0, key to positionMs)
        writeResume(context, items)
    }

    fun clearResumePosition(context: Context, key: String) {
        val items = readResume(context)
        if (items.removeAll { it.first == key }) writeResume(context, items)
    }

    // —— Session (last open page) ——
    fun getSessionUrl(context: Context): String? =
        prefs(context).getString(KEY_SESSION_URL, null)?.takeIf { it.isNotBlank() }

    fun setSessionUrl(context: Context, url: String?) {
        prefs(context).edit().apply {
            if (url.isNullOrBlank() || url.startsWith("about:")) remove(KEY_SESSION_URL)
            else putString(KEY_SESSION_URL, url)
        }.apply()
    }

    fun isAdBlockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ADBLOCK, true)

    fun setAdBlockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ADBLOCK, enabled).apply()
    }

    fun isStrongAdBlock(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STRONG_ADBLOCK, true)

    fun setStrongAdBlock(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STRONG_ADBLOCK, enabled).apply()
    }

    fun isJsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_JS, true)

    fun setJsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_JS, enabled).apply()
    }

    fun isBlockPopups(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_POPUPS, true)

    fun setBlockPopups(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_POPUPS, enabled).apply()
    }

    fun isSaveHistory(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SAVE_HISTORY, true)

    fun setSaveHistory(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
    }

    fun getPrivateDns(context: Context): PrivateDnsProvider {
        val id = prefs(context).getString(KEY_PRIVATE_DNS, PrivateDnsProvider.Off.id)
            ?: PrivateDnsProvider.Off.id
        return PrivateDnsProvider.fromId(id)
    }

    fun setPrivateDns(context: Context, provider: PrivateDnsProvider) {
        prefs(context).edit().putString(KEY_PRIVATE_DNS, provider.id).apply()
    }

    /**
     * Long-press cursor speed multiplier (1×–10×).
     * Applies only to continuous hold movement; single-press step is unchanged.
     * Invalid or missing values fall back to [BrowserConfig.CURSOR_HOLD_SPEED_DEFAULT].
     */
    fun getCursorHoldSpeed(context: Context): Float {
        val raw = prefs(context).getFloat(
            KEY_CURSOR_HOLD_SPEED,
            BrowserConfig.CURSOR_HOLD_SPEED_DEFAULT
        )
        return if (raw in BrowserConfig.CURSOR_HOLD_SPEED_MIN..BrowserConfig.CURSOR_HOLD_SPEED_MAX) {
            // Snap to whole steps for a stable UI (1×, 2×, … 10×)
            raw.toInt().toFloat().coerceIn(
                BrowserConfig.CURSOR_HOLD_SPEED_MIN,
                BrowserConfig.CURSOR_HOLD_SPEED_MAX
            )
        } else {
            BrowserConfig.CURSOR_HOLD_SPEED_DEFAULT
        }
    }

    fun setCursorHoldSpeed(context: Context, multiplier: Float) {
        val value = multiplier
            .toInt()
            .toFloat()
            .coerceIn(BrowserConfig.CURSOR_HOLD_SPEED_MIN, BrowserConfig.CURSOR_HOLD_SPEED_MAX)
        prefs(context).edit().putFloat(KEY_CURSOR_HOLD_SPEED, value).apply()
    }
}


/**
 * Centralized User-Agent profiles for [UserAgentPreset].
 *
 * Strings are plausible, widely-recognized patterns for each browser family and
 * form factor. They are not claimed to be the absolute latest release of any
 * browser. DigiBrowser remains powered by Android WebView (Chromium); changing
 * the UA string does not change Client Hints, navigator.userAgentData, or the
 * rendering engine.
 *
 * Sources / assumptions:
 * - Chrome / Edge: standard Chromium desktop & Android form-factor patterns.
 * - Firefox: standard Gecko desktop & Android form-factor patterns.
 * - Safari Desktop: standard macOS Safari WebKit pattern.
 * - Apple TV: Apple TV / tvOS WebKit-style pattern. Exact current tvOS UA is not
 *   published as a stable public contract; this is a documented, defensible
 *   approximation — not a verified live Apple TV browser string.
 */
object UserAgents {
    /** Fallback used only if WebSettings.getDefaultUserAgent is unavailable. */
    const val MOBILE =
        "Mozilla/5.0 (Linux; Android 13; TV) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val CHROME_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val CHROME_TABLET =
        "Mozilla/5.0 (Linux; Android 13; Pixel Tablet) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val CHROME_MOBILE =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    const val FIREFOX_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"

    const val FIREFOX_TABLET =
        "Mozilla/5.0 (Android 13; Tablet; rv:121.0) Gecko/121.0 Firefox/121.0"

    const val FIREFOX_MOBILE =
        "Mozilla/5.0 (Android 13; Mobile; rv:121.0) Gecko/121.0 Firefox/121.0"

    const val EDGE_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"

    const val SAFARI_DESKTOP =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/17.0 Safari/605.1.15"

    /**
     * Apple TV / tvOS WebKit-style UA.
     * Note: Apple does not publish a stable public contract for the current
     * Apple TV browser UA. This is a defensible approximation based on known
     * tvOS WebKit patterns, not a verified live device string.
     */
    const val APPLE_TV =
        "Mozilla/5.0 (Apple TV; CPU iPhone OS 17_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"

    /** Legacy alias kept for other modules that still reference DESKTOP. */
    const val DESKTOP = CHROME_DESKTOP

    fun defaultOf(context: android.content.Context): String {
        return runCatching {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        }.getOrDefault(MOBILE)
    }

    fun forPreset(context: android.content.Context, preset: UserAgentPreset): String {
        return when (preset) {
            UserAgentPreset.Default -> defaultOf(context)
            UserAgentPreset.ChromeDesktop -> CHROME_DESKTOP
            UserAgentPreset.ChromeTablet -> CHROME_TABLET
            UserAgentPreset.ChromeMobile -> CHROME_MOBILE
            UserAgentPreset.FirefoxDesktop -> FIREFOX_DESKTOP
            UserAgentPreset.FirefoxTablet -> FIREFOX_TABLET
            UserAgentPreset.FirefoxMobile -> FIREFOX_MOBILE
            UserAgentPreset.EdgeDesktop -> EDGE_DESKTOP
            UserAgentPreset.SafariDesktop -> SAFARI_DESKTOP
            UserAgentPreset.AppleTv -> APPLE_TV
        }
    }
}
