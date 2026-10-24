package com.appsventory.digiapps.scour.data

/** App-wide constants — no magic numbers / URLs scattered in UI code. */
object BrowserConfig {
    const val HOME_URL = "https://www.google.com"
    const val DEFAULT_SHORTCUT_COLOR = 0xFF22D3EE

    /** Hard cap so "open in new tab" can never grow memory/storage without bound. */
    const val MAX_TABS = 20

    /** Live WebViews kept in memory (rest are saved + released). */
    const val MAX_LIVE_WEBVIEWS = 3
    const val MAX_LIVE_WEBVIEWS_LOW_RAM = 2

    // Tab preview thumbnail
    const val THUMB_SCALE = 0.4f
    const val THUMB_MAX_WIDTH = 720
    const val THUMB_MAX_HEIGHT = 960

    const val LOAD_TIMEOUT_MS = 20_000L
    const val TOAST_MS = 1_800L

    // Search suggestions
    const val SUGGEST_DEBOUNCE_MS = 250L
    const val SUGGEST_TIMEOUT_MS = 4_000
    const val SUGGEST_MAX_REMOTE = 6
    const val SUGGEST_MAX_LOCAL = 2

    // Address-bar editing
    const val URL_FOCUS_RETRIES = 10
    const val KEYBOARD_SHOW_DELAY_MS = 80L

    // "Video detected" card hides itself so it never lingers over the page
    const val STREAM_PROMPT_TIMEOUT_MS = 10_000L

    // App updates (GitHub releases)
    const val GITHUB_REPO = "Appsventory/Digi-TvBrowser"
    const val UPDATE_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    const val UPDATE_RELEASES_URL = "https://github.com/$GITHUB_REPO/releases"
    const val UPDATE_CHECK_INTERVAL_MS = 10 * 60 * 1000L
    const val UPDATE_TIMEOUT_MS = 10_000
    const val UPDATE_PROGRESS_POLL_MS = 500L

    // Downloads
    const val DOWNLOAD_POLL_MS = 400L
    const val DOWNLOAD_BANNER_MS = 10_000L

    // Cursor (TV / D-pad browsing mode)
    /** Discrete movement distance for a single directional key press (px). */
    const val CURSOR_STEP_PX = 10f
    /**
     * Sustained-movement baseline speed (px/s) at the minimum hold multiplier.
     * Single-press step is independent of this value.
     */
    const val CURSOR_BASE_SPEED_PX_S = 100f
    /** Default long-press speed multiplier applied to continuous movement only. */
    const val CURSOR_HOLD_SPEED_DEFAULT = 10f
    const val CURSOR_HOLD_SPEED_MIN = 5f
    const val CURSOR_HOLD_SPEED_MAX = 20f
    /** Seconds at base speed before the hold ramp begins. */
    const val CURSOR_HOLD_DELAY_S = 0.12f
    /** Seconds to ramp from base to (base × multiplier) after the delay. */
    const val CURSOR_HOLD_RAMP_S = 0.35f
}

/** Download row status values (persisted in Prefs). */
object DownloadStatus {
    const val DOWNLOADING = "downloading"
    const val COMPLETED = "completed"
    const val FAILED = "failed"
}
