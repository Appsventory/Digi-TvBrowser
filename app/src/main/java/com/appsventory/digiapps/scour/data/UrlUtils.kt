package com.appsventory.digiapps.scour.data

/** True for a real navigable page (not blank / about:blank). */
fun String?.isRealUrl(): Boolean = !isNullOrBlank() && !startsWith("about:")

/** "https://www.example.com/x" → "example.com" (falls back to [fallback], default = the url). */
fun hostOf(url: String, fallback: String = url): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: fallback
