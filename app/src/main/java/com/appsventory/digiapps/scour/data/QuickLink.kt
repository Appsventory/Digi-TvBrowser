package com.appsventory.digiapps.scour.data

data class QuickLink(
    val title: String,
    val url: String,
    val colorHex: Long,
    val subtitle: String = ""
)

val defaultQuickLinks = listOf(
    QuickLink("YouTube", "https://www.youtube.com", 0xFFFF0000, "Video"),
    QuickLink("Netflix", "https://www.netflix.com", 0xFFE50914, "Streaming"),
    QuickLink("Google", "https://www.google.com", 0xFF4285F4, "Search"),
    QuickLink("Wikipedia", "https://www.wikipedia.org", 0xFF111111, "Knowledge"),
    QuickLink("X", "https://x.com", 0xFF1A1A1A, "Social"),
    QuickLink("Reddit", "https://www.reddit.com", 0xFFFF4500, "Community"),
    QuickLink("GitHub", "https://github.com", 0xFF24292E, "Code"),
    QuickLink("Example", "https://example.com", 0xFF10B981, "Test"),
)
