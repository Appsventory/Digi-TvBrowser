package com.appsventory.digiapps.scour.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class SuggestionKind { Query, History, Bookmark }

/** [url] is set for History / Bookmark (open it directly); null for a plain search query. */
data class Suggestion(val text: String, val kind: SuggestionKind, val url: String? = null)

/** Address-bar style suggestions: own bookmarks + history first, then the search engine's. */
object SearchSuggestions {

    private fun endpoint(engine: SearchEngine, encoded: String): String = when (engine) {
        SearchEngine.Google -> "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
        SearchEngine.Bing -> "https://api.bing.com/osjson.aspx?query=$encoded"
        // Yahoo has no stable public endpoint; DuckDuckGo's is the neutral fallback.
        SearchEngine.DuckDuckGo, SearchEngine.Yahoo -> "https://duckduckgo.com/ac/?type=list&q=$encoded"
    }

    /** OpenSearch JSON: ["query", ["a", "b", …]]. Empty on any failure. */
    suspend fun fetchRemote(engine: SearchEngine, query: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(endpoint(engine, URLEncoder.encode(query, "UTF-8")))
                .openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = BrowserConfig.SUGGEST_TIMEOUT_MS
                conn.readTimeout = BrowserConfig.SUGGEST_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", UserAgents.MOBILE)
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching emptyList<String>()
                val list = JSONArray(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray(1)
                (0 until list.length()).map { list.getString(it) }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    fun local(context: Context, query: String): List<Suggestion> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val bookmarks = Prefs.getBookmarks(context)
            .filter { it.title.contains(q, true) || it.url.contains(q, true) }
            .take(BrowserConfig.SUGGEST_MAX_LOCAL)
            .map { Suggestion(it.title.ifBlank { it.url }, SuggestionKind.Bookmark, it.url) }
        val history = Prefs.getHistory(context)
            .filter { it.title.contains(q, true) || it.url.contains(q, true) }
            .distinctBy { it.url }
            .take(BrowserConfig.SUGGEST_MAX_LOCAL)
            .map { Suggestion(it.title.ifBlank { it.url }, SuggestionKind.History, it.url) }
        return (bookmarks + history).distinctBy { it.url }
    }

    fun merge(local: List<Suggestion>, remote: List<String>, query: String): List<Suggestion> =
        local + remote
            .filter { !it.equals(query.trim(), ignoreCase = true) || local.isEmpty() }
            .take(BrowserConfig.SUGGEST_MAX_REMOTE)
            .map { Suggestion(it, SuggestionKind.Query) }
}
