package com.appsventory.digiapps.scour.player

import android.webkit.JavascriptInterface

/**
 * JS → app bridge (registered as [PlayerConfig.VIDEO_BRIDGE_NAME]).
 * Methods run on a WebView background thread; callbacks must hop to the main thread themselves.
 */
class VideoBridge(
    private val onLongVideoPlaying: (durationMs: Long) -> Unit,
    private val onMediaUrl: (url: String) -> Unit,
    private val onTrackUrl: (url: String, lang: String) -> Unit
) {

    @JavascriptInterface
    fun onPlaying(durationSec: Double, height: Int) {
        val ms = (durationSec * 1000).toLong()
        // Ads and previews also fire "playing"; only a long video says anything about the main one.
        if (ms >= PlayerConfig.MIN_MAIN_DURATION_MS) onLongVideoPlaying(ms)
    }

    /** currentSrc / src / <source src> of a <video> element. */
    @JavascriptInterface
    fun onMedia(url: String) = onMediaUrl(url)

    /** <track src srclang> of a <video> element. */
    @JavascriptInterface
    fun onTrack(url: String, lang: String) = onTrackUrl(url, lang)
}
