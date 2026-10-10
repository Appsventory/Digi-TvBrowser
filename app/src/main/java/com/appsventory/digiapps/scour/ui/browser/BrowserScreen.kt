package com.appsventory.digiapps.scour.ui.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.os.Environment
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.SslErrorHandler
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.focusProperties
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.appsventory.digiapps.scour.data.AdBlock
import com.appsventory.digiapps.scour.data.BookmarkEntry
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.data.BrowserTab
import com.appsventory.digiapps.scour.data.hostOf
import com.appsventory.digiapps.scour.data.isRealUrl
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.data.PrivateDnsProvider
import com.appsventory.digiapps.scour.data.TabThumbs
import com.appsventory.digiapps.scour.data.SearchEngine
import com.appsventory.digiapps.scour.data.UserAgents
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.player.DetectedSubtitle
import com.appsventory.digiapps.scour.player.ManifestProbe
import com.appsventory.digiapps.scour.player.MediaHit
import com.appsventory.digiapps.scour.player.MediaStreamDetector
import com.appsventory.digiapps.scour.player.PlayerConfig
import com.appsventory.digiapps.scour.player.PlayerRequest
import com.appsventory.digiapps.scour.player.StreamType
import com.appsventory.digiapps.scour.player.StreamRanking
import com.appsventory.digiapps.scour.player.VideoBridge
import com.appsventory.digiapps.scour.ui.player.VideoPlayerScreen
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import com.appsventory.digiapps.scour.ui.home.normalizeInput
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

/**
 * Layout:
 *   Box
 *     Box(fillMaxSize) → WebView + cursor   (always full size)
 *     BottomPanel      → solid, aligned bottom, drawn over the WebView (overlay)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String,
    initialTabs: List<BrowserTab> = emptyList(),
    initialActiveTabId: Long = -1L,
    onTabsChanged: (List<BrowserTab>, Long) -> Unit = { _, _ -> },
    onGoHome: () -> Unit,
    onOpenHistory: (currentUrl: String) -> Unit = {},
    onOpenDownloads: (currentUrl: String) -> Unit = {},
    onOpenSettings: (currentUrl: String) -> Unit = {},
    onOpenBookmarks: (currentUrl: String) -> Unit = {},
    onHistoryVisit: (title: String, url: String) -> Unit = { _, _ -> },
    onDownload: (id: Long, name: String, sizeLabel: String) -> Unit = { _, _, _ -> },
    searchEngine: SearchEngine = SearchEngine.Google,
    privateDns: PrivateDnsProvider = PrivateDnsProvider.Off,
    modifier: Modifier = Modifier
) {
    val webFocusRequester = remember { FocusRequester() }
    val urlChromeFocus = remember { FocusRequester() }
    val urlTextFocus = remember { FocusRequester() }
    val homeBtnFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeVisible = imeBottom > 0
    val isTv = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val isCompactChrome = LocalConfiguration.current.screenWidthDp < 600 && !isTv

    var tabsExpanded by remember { mutableStateOf(false) }
    var tabs by remember {
        mutableStateOf(
            initialTabs.ifEmpty {
                listOf(
                    BrowserTab(
                        id = 1L,
                        url = initialUrl,
                        title = hostOf(initialUrl, "Tab")
                    )
                )
            }
        )
    }
    var activeTabId by remember {
        mutableStateOf(
            initialActiveTabId.takeIf { id -> tabs.any { it.id == id } } ?: tabs.last().id
        )
    }
    val tabThumbs = TabThumbs.map


    var desktopMode by remember { mutableStateOf(Prefs.isDesktopMode(context)) }
    var isBookmarked by remember { mutableStateOf(Prefs.isBookmarked(context, initialUrl)) }
    var toastMsg by remember { mutableStateOf<String?>(null) }
    var downloadBanner by remember { mutableStateOf<String?>(null) }

    // ---- Native video player: streams seen on the current page (reactive) ----
    var detectedStreams by remember { mutableStateOf<List<DetectedStream>>(emptyList()) }
    var detectedSubtitles by remember { mutableStateOf<List<DetectedSubtitle>>(emptyList()) }
    var streamPromptDismissed by remember { mutableStateOf(false) }
    /** User pressed the Video panel button: show the prompt even if every stream looks like an ad. */
    var streamPromptForced by remember { mutableStateOf(false) }
    /** Duration of the long <video> the page is really playing (JS bridge), 0 = none seen yet. */
    var playingDurationMs by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    // Page-requested fullscreen (in-page players, DRM sites): the WebView hands us a View to show.
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var playerRequest by remember { mutableStateOf<PlayerRequest?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var urlText by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember {
        mutableStateOf(
            tabs.firstOrNull { it.id == activeTabId }?.title?.ifBlank { null } ?: "Digi Browser"
        )
    }

    /** Live URL (SPA-aware), not only the initial open URL. */
    fun currentPageUrl(): String {
        val live = urlText.takeIf { it.isRealUrl() }
        return live ?: initialUrl
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadStarted by remember { mutableStateOf(false) }

    var chromeVisible by remember { mutableStateOf(false) }
    var urlFieldFocused by remember { mutableStateOf(false) }
    var urlEditing by remember { mutableStateOf(false) }
    val isSecure = urlText.startsWith("https://", ignoreCase = true)

    var cursorX by remember { mutableFloatStateOf(0f) }
    var cursorY by remember { mutableFloatStateOf(0f) }
    var boxW by remember { mutableIntStateOf(0) }
    var boxH by remember { mutableIntStateOf(0) }
    var cursorVisible by remember { mutableStateOf(true) }
    var cursorInitialized by remember { mutableStateOf(false) }

    var dirX by remember { mutableFloatStateOf(0f) }
    var dirY by remember { mutableFloatStateOf(0f) }
    var holdStartMs by remember { mutableLongStateOf(0L) }

    fun clampCursor() {
        if (boxW <= 0 || boxH <= 0) return
        cursorX = min(max(0f, cursorX), (boxW - 1).toFloat())
        cursorY = min(max(0f, cursorY), (boxH - 1).toFloat())
    }

    fun applyMove(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) return
        cursorX += dx
        cursorY += dy
        clampCursor()
        cursorVisible = true
        val wv = webView ?: return
        if (dy > 0f && cursorY >= (boxH - 1).toFloat()) {
            if (wv.canScrollVertically(1)) wv.scrollBy(0, max(6, (dy * 2f).toInt()))
        } else if (dy < 0f && cursorY <= 0f) {
            if (wv.canScrollVertically(-1)) wv.scrollBy(0, min(-6, (dy * 2f).toInt()))
        }
    }

    fun clickAtCursor() {
        if (chromeVisible) return
        val wv = webView ?: return
        // Stop continuous D-pad movement (stuck dir → endless scroll)
        dirX = 0f
        dirY = 0f

        val x = cursorX.coerceIn(0f, (boxW - 1).coerceAtLeast(0).toFloat())
        val y = cursorY.coerceIn(0f, (boxH - 1).coerceAtLeast(0).toFloat())

        // JS click at viewport point (CSS px). Not a CSS cursor — injected click.
        val density = context.resources.displayMetrics.density.coerceAtLeast(0.5f)
        val cssX = x / density
        val cssY = y / density
        val js = """
            (function(){
              var el = document.elementFromPoint($cssX, $cssY);
              if (!el) return 'miss';
              var t = el;
              for (var i = 0; i < 8 && t; i++) {
                var tag = (t.tagName || '').toUpperCase();
                if (tag === 'A' || tag === 'BUTTON' || tag === 'INPUT' ||
                    tag === 'TEXTAREA' || tag === 'SELECT' ||
                    t.getAttribute('role') === 'button' ||
                    t.onclick != null || t.href) break;
                t = t.parentElement;
              }
              if (!t) t = el;
              try { t.focus(); } catch(e) {}
              try {
                var opts = {bubbles:true,cancelable:true,view:window,clientX:$cssX,clientY:$cssY};
                t.dispatchEvent(new MouseEvent('mousedown', opts));
                t.dispatchEvent(new MouseEvent('mouseup', opts));
                t.dispatchEvent(new MouseEvent('click', opts));
              } catch(e) {
                try { t.click(); } catch(e2) {}
              }
              return (t.tagName || '?');
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)

        // Real touch events as backup (view coordinates)
        val now = SystemClock.uptimeMillis()
        fun touch(action: Int, time: Long): MotionEvent {
            val props = MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
            val coords = MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1f
                size = 0.1f
            }
            return MotionEvent.obtain(
                now, time, action,
                1, arrayOf(props), arrayOf(coords),
                0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0
            )
        }
        val down = touch(MotionEvent.ACTION_DOWN, now)
        val up = touch(MotionEvent.ACTION_UP, now + 40)
        wv.dispatchTouchEvent(down)
        wv.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
        // No requestFocus() — that scrolls the page to the focused node (often top)
    }

    fun captureActiveThumb() {
        val wv = webView ?: return
        if (wv.width <= 0 || wv.height <= 0) return
        runCatching {
            val scale = BrowserConfig.THUMB_SCALE
            val w = (wv.width * scale).toInt().coerceIn(1, BrowserConfig.THUMB_MAX_WIDTH)
            val h = (wv.height * scale).toInt().coerceIn(1, BrowserConfig.THUMB_MAX_HEIGHT)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val canvas = AndroidCanvas(bmp)
            canvas.scale(scale, scale)
            wv.draw(canvas)
            TabThumbs.map[activeTabId] = bmp.asImageBitmap()
        }
    }

    fun doLoad(raw: String) {
        val target = normalizeInput(raw, searchEngine)
        if (target.isBlank()) return
        urlText = target
        loadError = null
        isLoading = true
        loadStarted = false
        chromeVisible = false
        keyboard?.hide()
        if (isTv) webFocusRequester.requestFocus()
        webView?.loadUrl(target)
    }

    fun persistActiveTab(url: String, title: String) {
        tabs = tabs.map {
            if (it.id == activeTabId) it.copy(
                url = url,
                title = title.ifBlank { it.title }
            ) else it
        }
    }

    // ---- WebView pool: one live WebView per recent tab (LRU) → instant switch, bounded memory ----
    val pool = remember { LinkedHashMap<Long, WebView>(4, 0.75f, true) }
    val savedStates = remember { HashMap<Long, Bundle>() }
    val maxLive = remember {
        val am = context.getSystemService(android.app.ActivityManager::class.java)
        if (am?.isLowRamDevice == true || isTv) BrowserConfig.MAX_LIVE_WEBVIEWS_LOW_RAM
        else BrowserConfig.MAX_LIVE_WEBVIEWS
    }
    var webFrame by remember { mutableStateOf<FrameLayout?>(null) }

    fun updateTabMeta(tabId: Long, url: String, title: String) {
        if (!url.isRealUrl()) return
        tabs = tabs.map { if (it.id == tabId) it.copy(url = url, title = title.ifBlank { it.title }) else it }
    }

    fun releaseTab(id: Long) {
        savedStates.remove(id)
        pool.remove(id)?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.webChromeClient = null
            wv.destroy()
        }
    }

    fun switchTab(id: Long) {
        captureActiveThumb()
        persistActiveTab(currentPageUrl(), pageTitle)
        if (id != activeTabId) {
            val target = tabs.firstOrNull { it.id == id } ?: return
            activeTabId = id          // pool swaps the live WebView, no reload
            urlText = target.url
            pageTitle = target.title
        }
        // One shot: close switcher (no second press needed)
        tabsExpanded = false
        chromeVisible = false
    }

    fun addTab() {
        if (tabs.size >= BrowserConfig.MAX_TABS) {
            toastMsg = "Tab limit reached (${BrowserConfig.MAX_TABS})"
            return
        }
        persistActiveTab(currentPageUrl(), pageTitle)
        val id = System.currentTimeMillis()
        val start = BrowserConfig.HOME_URL
        tabs = tabs + BrowserTab(id, start, "New tab")
        activeTabId = id
        urlText = start
        pageTitle = context.getString(R.string.tab_new)
        tabsExpanded = true
        // stay in tab switcher so user can see the new card
    }

    fun closeTab(id: Long) {
        if (tabs.size <= 1) return
        val idx = tabs.indexOfFirst { it.id == id }
        val next = tabs.filterNot { it.id == id }
        tabs = next
        TabThumbs.map.remove(id)
        if (activeTabId == id) {
            val newActive = next.getOrNull(idx.coerceAtMost(next.lastIndex)) ?: next.first()
            activeTabId = newActive.id
            urlText = newActive.url
            pageTitle = newActive.title
        }
        releaseTab(id)
    }

    fun clearAllTabs() {
        val id = System.currentTimeMillis()
        val fresh = BrowserTab(id, BrowserConfig.HOME_URL, context.getString(R.string.tab_new))
        TabThumbs.map.clear()
        val old = tabs.map { it.id }
        tabs = listOf(fresh)
        activeTabId = id
        urlText = fresh.url
        pageTitle = fresh.title
        old.forEach { releaseTab(it) }
        tabsExpanded = false
        chromeVisible = false
    }

    // Keep active tab metadata in sync with live navigation
    LaunchedEffect(urlText, pageTitle, activeTabId) {
        if (urlText.isRealUrl()) {
            tabs = tabs.map {
                if (it.id == activeTabId) it.copy(url = urlText, title = pageTitle.ifBlank { it.title })
                else it
            }
        }
    }

    // Push every change up so tabs are saved and Home sees the same list
    LaunchedEffect(tabs, activeTabId) {
        onTabsChanged(tabs, activeTabId)
    }

    LaunchedEffect(tabsExpanded) {
        if (tabsExpanded) {
            captureActiveThumb()
        }
    }

    fun hideChrome() {
        chromeVisible = false
        tabsExpanded = false
        keyboard?.hide()
        urlFieldFocused = false
        urlEditing = false
        if (isTv) webFocusRequester.requestFocus()
        else webView?.requestFocus()
    }

    fun showChrome() {
        chromeVisible = true
        dirX = 0f
        dirY = 0f
        keyboard?.hide()
        webView?.clearFocus()
    }

    LaunchedEffect(boxW, boxH) {
        if (boxW > 0 && boxH > 0 && !cursorInitialized) {
            cursorX = boxW / 2f
            cursorY = boxH / 2f
            cursorInitialized = true
        }
        clampCursor()
    }

    // Reset cursor center when panel toggles (web area size changes)
    /** Real URL of the page now showing (WebView first, then tracked state). */
    fun liveUrl(): String = webView?.url?.takeIf { it.isRealUrl() } ?: currentPageUrl()

    fun syncNavFromWebView() {
        webView?.let {
            canGoBack = it.canGoBack()
            canGoForward = it.canGoForward()
            it.url?.let { u -> urlText = u }
        }
    }

    fun goBack() { webView?.goBack(); syncNavFromWebView() }
    fun goForward() { webView?.goForward(); syncNavFromWebView() }

    fun reloadPage() {
        isLoading = true
        webView?.reload()
        hideChrome()
    }

    fun toggleDesktop() {
        desktopMode = !desktopMode
        Prefs.setDesktopMode(context, desktopMode)
        webView?.settings?.userAgentString =
            if (desktopMode) UserAgents.DESKTOP else UserAgents.MOBILE
        webView?.reload()
        toastMsg = context.getString(if (desktopMode) R.string.msg_desktop_on else R.string.msg_mobile_on)
    }

    fun showSecurityToast() {
        toastMsg = context.getString(
            if (isSecure) R.string.msg_https_secure else R.string.msg_http_not_secure
        )
    }

    fun toggleBookmark() {
        val u = webView?.url ?: urlText
        if (!u.isRealUrl()) {
            toastMsg = context.getString(R.string.msg_no_page_bookmark)
            return
        }
        val title = pageTitle.ifBlank { hostOf(u) }
        isBookmarked = Prefs.toggleBookmark(context, BookmarkEntry(title, u))
        toastMsg = context.getString(
            if (isBookmarked) R.string.msg_bookmarked else R.string.msg_bookmark_removed
        )
    }

    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            delay(150)
            runCatching { homeBtnFocus.requestFocus() }
        } else {
            cursorInitialized = false
        }
    }

    LaunchedEffect(isLoading, loadStarted, urlText) {
        if (!isLoading) return@LaunchedEffect
        delay(BrowserConfig.LOAD_TIMEOUT_MS)
        if (isLoading) {
            isLoading = false
            if (loadError == null) {
                loadError = context.getString(
                    if (!loadStarted) R.string.msg_webview_not_started else R.string.msg_timeout
                )
            }
        }
    }

    LaunchedEffect(dirX, dirY) {
        if (dirX == 0f && dirY == 0f) return@LaunchedEffect
        holdStartMs = SystemClock.uptimeMillis()
        var lastNanos = withFrameNanos { it }
        while (dirX != 0f || dirY != 0f) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNanos = now
            val heldFor = (SystemClock.uptimeMillis() - holdStartMs) / 1000f
            val accel = (heldFor / 0.4f).coerceIn(0f, 1f)
            val speed = 120f + 240f * accel
            val len = sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(0.001f)
            applyMove((dirX / len) * speed * dt, (dirY / len) * speed * dt)
        }
    }

    LaunchedEffect(Unit) {
        if (isTv) {
            keyboard?.hide()
            delay(200)
            runCatching { webFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(urlEditing) {
        if (urlEditing) {
            delay(32)
            runCatching { urlTextFocus.requestFocus() }
            delay(60)
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    val rankedStreams = remember(detectedStreams, playingDurationMs) {
        StreamRanking.rank(detectedStreams, playingDurationMs)
    }
    val streamPromptVisible = rankedStreams.isNotEmpty() && !streamPromptDismissed &&
        (streamPromptForced || rankedStreams.any { StreamRanking.isEligible(it) }) &&
        !chromeVisible && !tabsExpanded && playerRequest == null

    // The player screen has its own BackHandler; this one stays out of its way.
    BackHandler(enabled = playerRequest == null) {
        when {
            tabsExpanded -> {
                tabsExpanded = false
            }
            streamPromptVisible -> streamPromptDismissed = true
            urlEditing -> {
                urlEditing = false
                keyboard?.hide()
                runCatching { homeBtnFocus.requestFocus() }
            }
            urlFieldFocused -> {
                keyboard?.hide()
                urlFieldFocused = false
                runCatching { homeBtnFocus.requestFocus() }
            }
            chromeVisible -> hideChrome()
            else -> showChrome()
        }
    }


    // ---- Stream detection (called from WebView's IO thread → state is updated on main) ----
    fun resetDetection() {
        detectedStreams = emptyList()
        detectedSubtitles = emptyList()
        streamPromptDismissed = false
        streamPromptForced = false
        playingDurationMs = 0L
    }

    fun strongAdBlockNow(ctx: Context): Boolean =
        (Prefs.isAdBlockEnabled(ctx) || privateDns == PrivateDnsProvider.AdGuard) &&
            Prefs.isStrongAdBlock(ctx)

    /** Main thread. [requestHeaders] = what the WebView sent (empty when the URL came from the DOM). */
    fun addDetected(hit: MediaHit, requestHeaders: Map<String, String>) {
        when (hit) {
            is MediaHit.Stream -> {
                if (detectedStreams.size >= PlayerConfig.MAX_STREAM_CANDIDATES ||
                    detectedStreams.any { it.url == hit.url }
                ) return
                if (strongAdBlockNow(context) && AdBlock.looksLikeAdStream(hit.url)) return
                val page = urlText.takeIf { it.isRealUrl() }
                val stream = DetectedStream(
                    url = hit.url,
                    type = hit.type,
                    title = pageTitle.ifBlank { hostOf(hit.url) },
                    userAgent = MediaStreamDetector.userAgentOf(requestHeaders)
                        ?: webView?.settings?.userAgentString,
                    headers = MediaStreamDetector.forwardedHeaders(requestHeaders, page),
                    probed = hit.type == StreamType.PROGRESSIVE
                )
                detectedStreams = detectedStreams + stream
                if (!stream.probed) {
                    // Read duration / height from the manifest so ads can be told from the film.
                    scope.launch {
                        val info = ManifestProbe.probe(stream)
                        detectedStreams = detectedStreams.map {
                            if (it.url == stream.url) it.copy(info = info, probed = true) else it
                        }
                    }
                }
            }
            is MediaHit.Subtitle -> {
                if (detectedSubtitles.size >= PlayerConfig.MAX_SUBTITLE_CANDIDATES ||
                    detectedSubtitles.any { it.url == hit.url }
                ) return
                detectedSubtitles = detectedSubtitles + DetectedSubtitle(hit.url, hit.format, hit.language)
            }
        }
    }

    fun handleMediaRequest(request: WebResourceRequest) {
        if (request.method != "GET") return
        val hit = MediaStreamDetector.classify(request.url) ?: return
        val requestHeaders = request.requestHeaders ?: emptyMap()
        mainHandler.post { addDetected(hit, requestHeaders) }
    }

    fun openPlayer(stream: DetectedStream) {
        streamPromptDismissed = true
        streamPromptForced = false
        // Silence the page so it does not play under the native player.
        webView?.evaluateJavascript(PlayerConfig.PAUSE_WEB_MEDIA_JS, null)
        playerRequest = PlayerRequest(stream, detectedSubtitles, rankedStreams)
    }

    fun closePlayer() {
        playerRequest = null
        if (isTv) runCatching { webFocusRequester.requestFocus() }
    }

    // New tab in front → detections of the old page no longer apply
    LaunchedEffect(activeTabId) {
        resetDetection()
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    fun createWebView(ctx: Context, tab: BrowserTab): WebView =
        WebView(ctx).apply {
            WebView.setWebContentsDebuggingEnabled(
                (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.WHITE)
            // Phone: must be focusable so tap on <input> opens soft keyboard
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = Prefs.isJsEnabled(ctx)
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = !Prefs.isBlockPopups(ctx)
                domStorageEnabled = true
                databaseEnabled = true
                // Fit wide desktop pages on TV without clipping
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                @Suppress("DEPRECATION")
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                userAgentString =
                    if (Prefs.isDesktopMode(ctx)) UserAgents.DESKTOP else UserAgents.MOBILE
            }
            // Scale so desktop layout fits screen width
            setInitialScale(1)
            if (Prefs.isJsEnabled(ctx)) {
                // Page tells us which <video> is really playing → picks the film over ads.
                addJavascriptInterface(
                    VideoBridge(
                        onLongVideoPlaying = { ms ->
                            mainHandler.post { if (tab.id == activeTabId) playingDurationMs = ms }
                        },
                        onMediaUrl = { url ->
                            mainHandler.post {
                                if (tab.id == activeTabId) {
                                    MediaStreamDetector.classifyMediaElement(url)?.let { addDetected(it, emptyMap()) }
                                }
                            }
                        },
                        onTrackUrl = { url, lang ->
                            mainHandler.post {
                                if (tab.id == activeTabId) {
                                    MediaStreamDetector.classifyTrack(url, lang)?.let { addDetected(it, emptyMap()) }
                                }
                            }
                        }
                    ),
                    PlayerConfig.VIDEO_BRIDGE_NAME
                )
            }
            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                val fileName = android.webkit.URLUtil.guessFileName(
                    url, contentDisposition, mimeType
                )
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription(ctx.getString(R.string.download_desc, fileName))
                        setTitle(fileName)
                        setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            fileName
                        )
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(true)
                    }
                    val dm = ctx.getSystemService(DownloadManager::class.java)
                    val id = dm?.enqueue(request) ?: -1L
                    val sizeLabel = if (contentLength > 0) {
                        val mb = contentLength / (1024.0 * 1024.0)
                        String.format("%.1f MB", mb)
                    } else "—"
                    if (id >= 0L) {
                        onDownload(id, fileName, sizeLabel)
                        downloadBanner = fileName
                        toastMsg = null
                    }
                } catch (e: Exception) {
                    toastMsg = ctx.getString(
                        R.string.msg_download_failed,
                        e.message ?: ctx.getString(R.string.msg_download_failed_generic)
                    )
                }
            }
            fun active() = tab.id == activeTabId
            fun syncNav(v: WebView?) {
                if (v == null || !active()) return
                canGoBack = v.canGoBack()
                canGoForward = v.canGoForward()
            }
            fun syncUrl(v: WebView?, url: String?) {
                if (url.isNullOrBlank() || url == "about:blank") return
                val title = v?.title?.takeIf { it.isNotBlank() }
                    ?: hostOf(url)
                if (!active()) {
                    updateTabMeta(tab.id, url, title)
                    return
                }
                urlText = url
                pageTitle = title
                isBookmarked = Prefs.isBookmarked(ctx, url)
                onHistoryVisit(title, url)
                Prefs.setSessionUrl(ctx, url)
                // tab title/url refreshed via composition state outside factory
                syncNav(v)
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = false
                override fun shouldInterceptRequest(
                    v: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val u = request?.url?.toString()
                    if ((Prefs.isAdBlockEnabled(ctx) || privateDns == PrivateDnsProvider.AdGuard) &&
                        AdBlock.shouldBlock(u, pageUrl = urlText, strong = Prefs.isStrongAdBlock(ctx))
                    ) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            java.io.ByteArrayInputStream(ByteArray(0))
                        )
                    }
                    if (active() && request != null) handleMediaRequest(request)
                    return super.shouldInterceptRequest(v, request)
                }
                override fun onPageStarted(v: WebView?, url: String?, f: Bitmap?) {
                    if (!active()) return
                    resetDetection()
                    loadStarted = true
                    isLoading = true
                    loadError = null
                    if (url != null) urlText = url
                    syncNav(v)
                }
                override fun onPageFinished(v: WebView, url: String) {
                    if (Prefs.isJsEnabled(ctx)) {
                        v.evaluateJavascript(PlayerConfig.VIDEO_HOOK_JS, null)
                        if (strongAdBlockNow(ctx)) v.evaluateJavascript(AdBlock.COSMETIC_JS, null)
                    }
                    if (!active()) { syncUrl(v, url); return }
                    isLoading = false
                    loadError = null
                    dirX = 0f
                    dirY = 0f
                    syncUrl(v, url)
                }
                // SPA / pushState (YouTube etc.) — URL & history without full reload
                override fun doUpdateVisitedHistory(v: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(v, url, isReload)
                    syncUrl(v, url)
                }
                override fun onReceivedError(
                    v: WebView?, r: WebResourceRequest?, e: WebResourceError?
                ) {
                    if (r?.isForMainFrame == true && active()) {
                        isLoading = false
                        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) e?.errorCode else null
                        loadError = ctx.getString(
                            R.string.msg_load_error,
                            code?.toString() ?: "?",
                            e?.description?.toString() ?: ctx.getString(R.string.msg_error_unknown)
                        )
                    }
                }
                override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                    // Never proceed past an invalid certificate — block and tell the user.
                    h?.cancel()
                    if (active()) {
                        isLoading = false
                        loadError = ctx.getString(R.string.msg_ssl_error)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customViewCallback?.onCustomViewHidden()
                    customView = view
                    customViewCallback = callback
                }
                override fun onHideCustomView() {
                    customView = null
                    customViewCallback = null
                }
                override fun onProgressChanged(v: WebView?, p: Int) {
                    if (!active()) return
                    if (p > 0) loadStarted = true
                    if (p >= 100) {
                        isLoading = false
                        syncNav(v)
                    }
                }
                override fun onReceivedTitle(v: WebView?, title: String?) {
                    if (!title.isNullOrBlank() && !active()) {
                        v?.url?.takeIf { it.isNotBlank() }?.let { updateTabMeta(tab.id, it, title) }
                    } else if (!title.isNullOrBlank()) {
                        pageTitle = title
                        val u = v?.url
                        if (!u.isNullOrBlank()) onHistoryVisit(title, u)
                    }
                }
                // Block window.open / target=_blank popups when enabled
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    if (Prefs.isBlockPopups(ctx)) {
                        toastMsg = ctx.getString(R.string.msg_popup_blocked)
                        return false
                    }
                    // Open in same tab instead of a new window
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = view
                    resultMsg?.sendToTarget()
                    return true
                }
            }
            // Evicted tab → restore history/scroll; else plain load
            val saved = savedStates.remove(tab.id)
            if (saved == null || restoreState(saved) == null) {
                post { loadUrl(tab.url) }
            }
        }

    /** Free background WebViews beyond [keep]; their state is saved so they restore without losing history. */
    fun trimPool(keep: Int) {
        while (pool.size > keep) {
            val id = pool.keys.firstOrNull { it != activeTabId } ?: break
            val wv = pool[id]
            if (wv != null) {
                val b = Bundle()
                wv.saveState(b)
                savedStates[id] = b
            }
            pool.remove(id)?.let { dead ->
                (dead.parent as? ViewGroup)?.removeView(dead)
                dead.stopLoading()
                dead.webChromeClient = null
                dead.destroy()
            }
        }
    }

    fun attachTab(tabId: Long) {
        val fl = webFrame ?: return
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        val existing = pool[tabId]
        val wv = existing ?: createWebView(fl.context, tab).also { pool[tabId] = it }
        for (i in 0 until fl.childCount) {
            (fl.getChildAt(i) as? WebView)?.takeIf { it !== wv }?.onPause()
        }
        if (wv.parent !== fl) {
            (wv.parent as? ViewGroup)?.removeView(wv)
            fl.removeAllViews()
            fl.addView(wv, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        wv.onResume()
        wv.settings.userAgentString =
            if (Prefs.isDesktopMode(fl.context)) UserAgents.DESKTOP else UserAgents.MOBILE
        webView = wv
        if (existing != null) {
            // Already alive → no reload, just reflect its state
            urlText = wv.url?.takeIf { it.isRealUrl() } ?: tab.url
            pageTitle = wv.title?.takeIf { it.isNotBlank() } ?: tab.title
            isLoading = wv.progress < 100
            loadError = null
            canGoBack = wv.canGoBack()
            canGoForward = wv.canGoForward()
            isBookmarked = Prefs.isBookmarked(fl.context, urlText)
        } else {
            isLoading = true
            loadStarted = false
            loadError = null
        }
        trimPool(maxLive)
    }

    LaunchedEffect(activeTabId, webFrame) { attachTab(activeTabId) }

    // System low on memory → keep only the visible tab alive
    DisposableEffect(Unit) {
        val cb = object : android.content.ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) trimPool(1)
            }
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            override fun onLowMemory() { trimPool(1) }
        }
        context.applicationContext.registerComponentCallbacks(cb)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(cb)
            webFrame?.removeAllViews()
            pool.values.toList().forEach { wv ->
                wv.stopLoading()
                wv.webChromeClient = null
                wv.destroy()
            }
            pool.clear()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DigiColors.BgDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ===== Web area (always full size; panel is drawn over it) =====
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiColors.BgWeb)
                .onSizeChanged { size ->
                    boxW = size.width
                    boxH = size.height
                    clampCursor()
                }
                // TV only: D-pad target. On phone this steals focus from WebView inputs.
                .then(
                    if (isTv) {
                        Modifier
                            .focusRequester(webFocusRequester)
                            .focusable()
                    } else Modifier
                )
                .onPreviewKeyEvent { event ->
                    if (!isTv) return@onPreviewKeyEvent false
                    val down = event.type == KeyEventType.KeyDown
                    val up = event.type == KeyEventType.KeyUp
                    val step = 10f

                    if (down && (event.key == Key.Menu || event.key == Key.Guide || event.key == Key.Info)) {
                        if (chromeVisible) hideChrome() else showChrome()
                        return@onPreviewKeyEvent true
                    }

                    if (chromeVisible) return@onPreviewKeyEvent false

                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (down) {
                                applyMove(-step, 0f)
                                if (dirX != -1f) { dirX = -1f; holdStartMs = SystemClock.uptimeMillis() }
                            }
                            if (up) dirX = 0f
                            true
                        }
                        Key.DirectionRight -> {
                            if (down) {
                                applyMove(step, 0f)
                                if (dirX != 1f) { dirX = 1f; holdStartMs = SystemClock.uptimeMillis() }
                            }
                            if (up) dirX = 0f
                            true
                        }
                        Key.DirectionUp -> {
                            if (down) {
                                applyMove(0f, -step)
                                if (dirY != -1f) { dirY = -1f; holdStartMs = SystemClock.uptimeMillis() }
                            }
                            if (up) dirY = 0f
                            true
                        }
                        Key.DirectionDown -> {
                            if (down) {
                                applyMove(0f, step)
                                if (dirY != 1f) { dirY = 1f; holdStartMs = SystemClock.uptimeMillis() }
                            }
                            if (up) dirY = 0f
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (down) clickAtCursor()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            AndroidView(
                factory = { ctx -> FrameLayout(ctx).also { webFrame = it } },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading && loadError == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DigiColors.Accent
                )
            }

            loadError?.let { err ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(DigiColors.BgCard, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.msg_cannot_load), color = DigiColors.OnDark, fontSize = 18.sp)
                    Text(
                        err,
                        color = DigiColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }
            }

            
            // Download toast — D-pad focusable View / dismiss
            downloadBanner?.let { fileName ->
                val bannerViewFocus = remember { FocusRequester() }
                var viewFocused by remember { mutableStateOf(false) }
                var closeFocused by remember { mutableStateOf(false) }
                LaunchedEffect(fileName) {
                    delay(80)
                    runCatching { bannerViewFocus.requestFocus() }
                    delay(BrowserConfig.DOWNLOAD_BANNER_MS)
                    if (downloadBanner == fileName) downloadBanner = null
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .fillMaxWidth(0.92f)
                        .background(DigiColors.BgPanel, RoundedCornerShape(12.dp))
                        .border(2.dp, DigiColors.Accent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.msg_downloading_file, fileName),
                        color = DigiColors.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        stringResource(R.string.action_view),
                        color = if (viewFocused) DigiColors.BgDeep else DigiColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .focusRequester(bannerViewFocus)
                            .onFocusChanged { viewFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                downloadBanner = null
                                onOpenDownloads(liveUrl())
                            }
                            .background(
                                if (viewFocused) DigiColors.Accent else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                DigiColors.Accent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "✕",
                        color = if (closeFocused) DigiColors.BgDeep else DigiColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .onFocusChanged { closeFocused = it.isFocused }
                            .focusable()
                            .clickable { downloadBanner = null }
                            .background(
                                if (closeFocused) DigiColors.Accent else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (isTv && cursorVisible && boxW > 0 && !chromeVisible) {
                val cursorAlpha = if (imeVisible) 0f else 1f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = 14.dp.toPx()
                    val c = Offset(cursorX, cursorY)
                    drawCircle(DigiColors.CursorOuter.copy(alpha = cursorAlpha), r, c)
                    drawCircle(DigiColors.CursorMid.copy(alpha = cursorAlpha), r - 3.dp.toPx(), c)
                    drawCircle(DigiColors.CursorDot.copy(alpha = cursorAlpha), 4.dp.toPx(), c)
                }
            }


        }

        // ===== "Video detected" prompt =====
        if (streamPromptVisible) {
            StreamPrompt(
                streams = rankedStreams,
                onPlayInApp = { openPlayer(it) },
                onPlayInBrowser = {
                    streamPromptDismissed = true
                    if (isTv) runCatching { webFocusRequester.requestFocus() }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ===== Bottom panel — overlay on top of WebView, solid background =====
        if (chromeVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(DigiColors.BgPanel)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            // Swipe up (bottom→top) → open full tab switcher
                            if (dragAmount < -16f) tabsExpanded = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // ——— Chrome controls ———
                if (isCompactChrome) {
                    // Group 1: icon-only Secure · Address · Mobile — centered
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PanelButton(
                            icon = Icons.Outlined.Shield,
                            label = if (isSecure) stringResource(R.string.panel_secure) else stringResource(R.string.panel_http),
                            enabled = true,
                            active = isSecure,
                            size = 44.dp,
                            showLabel = false,
                            onClick = { showSecurityToast() }
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .height(44.dp)
                                .background(DigiColors.BgCard, RoundedCornerShape(22.dp))
                                .border(
                                    width = if (urlFieldFocused || urlEditing) 2.dp else 1.dp,
                                    color = if (urlFieldFocused || urlEditing) DigiColors.FocusRing
                                    else DigiColors.Border,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .focusRequester(urlChromeFocus)
                                .onFocusChanged { fs -> urlFieldFocused = fs.isFocused }
                                .focusable()
                                .clickable { urlEditing = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search, null,
                                tint = DigiColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (urlEditing) {
                                BasicTextField(
                                    value = urlText,
                                    onValueChange = { urlText = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = DigiColors.TextPrimary,
                                        fontSize = 13.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                                        keyboardType = KeyboardType.Uri
                                    ),
                                    keyboardActions = KeyboardActions(onGo = {
                                        doLoad(urlText)
                                        urlEditing = false
                                        keyboard?.hide()
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(urlTextFocus),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(DigiColors.Accent)
                                )
                            } else {
                                Text(
                                    urlText,
                                    color = DigiColors.TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        PanelButton(
                            icon = Icons.Outlined.Computer,
                            label = if (desktopMode) stringResource(R.string.panel_desktop) else stringResource(R.string.panel_mobile),
                            active = desktopMode,
                            size = 44.dp,
                            showLabel = false,
                            onClick = { toggleDesktop() }
                        )
                    }
                    // Group 2: Close · Back · Forward · Reload
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PanelButton(Icons.Default.Close, stringResource(R.string.panel_close), onClick = { hideChrome() })
                        PanelButton(
                            Icons.Default.ArrowBack, stringResource(R.string.panel_back), enabled = canGoBack,
                            onClick = { goBack() }
                        )
                        PanelButton(
                            Icons.Default.ArrowForward, stringResource(R.string.panel_forward), enabled = canGoForward,
                            onClick = { goForward() }
                        )
                        PanelButton(
                            Icons.Default.Refresh, stringResource(R.string.panel_reload),
                            onClick = { reloadPage() }
                        )
                    }
                    // Group 3: Home · Bookmark · History · Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PanelButton(
                            Icons.Default.Home, stringResource(R.string.nav_home),
                            focusRequester = homeBtnFocus,
                            onClick = { keyboard?.hide(); captureActiveThumb(); onGoHome() }
                        )
                        PanelButton(
                            icon = if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                            label = if (isBookmarked) stringResource(R.string.panel_saved) else stringResource(R.string.panel_bookmark),
                            active = isBookmarked,
                            onClick = { toggleBookmark() }
                        )
                        PanelButton(
                            Icons.Outlined.History, stringResource(R.string.nav_history),
                            onClick = { hideChrome(); onOpenHistory(liveUrl()) }
                        )
                        if (detectedStreams.isNotEmpty()) {
                            PanelButton(
                                Icons.Outlined.PlayCircle, stringResource(R.string.panel_video),
                                onClick = { hideChrome(); streamPromptDismissed = false; streamPromptForced = true }
                            )
                        }
                        PanelButton(
                            Icons.Outlined.Settings, stringResource(R.string.nav_settings),
                            onClick = { hideChrome(); onOpenSettings(liveUrl()) }
                        )
                    }
                } else {
                    // TV / wide layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(44.dp)
                                .background(DigiColors.BgCard, RoundedCornerShape(22.dp))
                                .border(
                                    width = if (urlFieldFocused || urlEditing) 2.dp else 1.dp,
                                    color = if (urlFieldFocused || urlEditing) DigiColors.FocusRing
                                    else DigiColors.Border,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .focusRequester(urlChromeFocus)
                                .onFocusChanged { fs -> urlFieldFocused = fs.isFocused }
                                .focusable()
                                .clickable { urlEditing = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search, null,
                                tint = DigiColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            if (urlEditing) {
                                BasicTextField(
                                    value = urlText,
                                    onValueChange = { urlText = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = DigiColors.TextPrimary,
                                        fontSize = 15.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                                        keyboardType = KeyboardType.Uri
                                    ),
                                    keyboardActions = KeyboardActions(onGo = {
                                        doLoad(urlText)
                                        urlEditing = false
                                        keyboard?.hide()
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(urlTextFocus),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(DigiColors.Accent)
                                )
                            } else {
                                Text(
                                    urlText,
                                    color = DigiColors.TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PanelButton(
                            Icons.Default.Home, stringResource(R.string.nav_home),
                            focusRequester = homeBtnFocus,
                            onClick = { keyboard?.hide(); captureActiveThumb(); onGoHome() }
                        )
                        PanelButton(Icons.Default.Close, stringResource(R.string.panel_close), onClick = { hideChrome() })
                        PanelButton(
                            Icons.Default.ArrowBack, stringResource(R.string.panel_back), enabled = canGoBack,
                            onClick = { goBack() }
                        )
                        PanelButton(
                            Icons.Default.ArrowForward, stringResource(R.string.panel_forward), enabled = canGoForward,
                            onClick = { goForward() }
                        )
                        PanelButton(
                            Icons.Default.Refresh, stringResource(R.string.panel_reload),
                            onClick = { reloadPage() }
                        )
                        PanelButton(
                            icon = if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                            label = if (isBookmarked) stringResource(R.string.panel_saved) else stringResource(R.string.panel_bookmark),
                            active = isBookmarked,
                            onClick = { toggleBookmark() }
                        )
                        PanelButton(
                            Icons.Outlined.History, stringResource(R.string.nav_history),
                            onClick = { hideChrome(); onOpenHistory(liveUrl()) }
                        )
                        PanelButton(
                            Icons.Outlined.Shield,
                            label = if (isSecure) stringResource(R.string.panel_secure) else stringResource(R.string.panel_not_secure),
                            active = isSecure,
                            onClick = { showSecurityToast() }
                        )
                        PanelButton(
                            Icons.Outlined.Computer,
                            label = if (desktopMode) stringResource(R.string.panel_desktop) else stringResource(R.string.panel_mobile),
                            active = desktopMode,
                            onClick = { toggleDesktop() }
                        )
                        if (detectedStreams.isNotEmpty()) {
                            PanelButton(
                                Icons.Outlined.PlayCircle, stringResource(R.string.panel_video),
                                onClick = { hideChrome(); streamPromptDismissed = false; streamPromptForced = true }
                            )
                        }
                        PanelButton(
                            Icons.Outlined.Settings, stringResource(R.string.nav_settings),
                            onClick = { hideChrome(); onOpenSettings(liveUrl()) }
                        )
                    }
                }

                // Hint only — Down from address/buttons goes here first, not straight to tabs
                var tabsHintFocused by remember { mutableStateOf(false) }
                Text(
                    text = stringResource(R.string.panel_hint_tabs, tabs.size),
                    color = if (tabsHintFocused) DigiColors.Accent else DigiColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                        .onFocusChanged { tabsHintFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (ev.key) {
                                Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    tabsExpanded = true
                                    true
                                }
                                else -> false
                            }
                        }
                        .clickable { tabsExpanded = true }
                )

                toastMsg?.let { msg ->
                    Text(
                        text = msg,
                        color = DigiColors.Accent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    LaunchedEffect(msg) {
                        delay(BrowserConfig.TOAST_MS)
                        toastMsg = null
                    }
                }

                Text(
                    text = pageTitle,
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    } // Box (web + panel)

        // ===== Full-screen tab switcher (Chrome-style) =====
        AnimatedVisibility(
            visible = tabsExpanded,
            enter = slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(280)),
            exit = slideOutVertically(
                animationSpec = tween(260),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(200))
        ) {
            TabSwitcherOverlay(
                tabs = tabs,
                activeTabId = activeTabId,
                thumbs = tabThumbs,
                onSelect = { switchTab(it) },
                onClose = { closeTab(it) },
                onAdd = { addTab() },
                onClear = { clearAllTabs() },
                onDismiss = { tabsExpanded = false }
            )
        }
    } // Box

    // ===== Page fullscreen (WebChromeClient custom view) =====
    customView?.let { view ->
        FullscreenWebVideo(
            view = view,
            callback = customViewCallback,
            onExit = { customView = null; customViewCallback = null }
        )
    }

    // ===== Native video player (full screen, ignores system-bar paddings) =====
    playerRequest?.let { req ->
        VideoPlayerScreen(
            initialStream = req.stream,
            subtitles = req.subtitles,
            streams = req.streams,
            onClose = { closePlayer() }
        )
    }
    } // Box (browser + player)
}

@Composable
private fun PanelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    focusRequester: FocusRequester? = null,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    showLabel: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .background(
                    color = when {
                        focused -> DigiColors.FocusFill
                        active -> DigiColors.AccentSoft
                        else -> DigiColors.BgCard
                    },
                    shape = CircleShape
                )
                .then(
                    if (focused) Modifier.border(2.dp, DigiColors.FocusRing, CircleShape)
                    else if (active) Modifier.border(1.dp, DigiColors.Accent, CircleShape)
                    else Modifier
                )
                .onFocusChanged { focused = it.isFocused }
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> DigiColors.TextDisabled
                    focused -> DigiColors.FocusOnAccent
                    active -> DigiColors.Accent
                    else -> DigiColors.TextPrimary
                },
                modifier = Modifier.size(if (size < 44.dp) 20.dp else 22.dp)
            )
        }
        if (showLabel) {
            Text(
                text = label,
                color = when {
                    focused -> DigiColors.Accent
                    active -> DigiColors.Accent
                    !enabled -> DigiColors.TextDisabled
                    else -> DigiColors.TextMuted
                },
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun TabSwitcherOverlay(
    tabs: List<BrowserTab>,
    activeTabId: Long,
    thumbs: Map<Long, ImageBitmap>,
    allowCloseLast: Boolean = false,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val ids = tabs.map { it.id }
    val tabFr = remember(ids) { ids.associateWith { FocusRequester() } }
    val clearFr = remember { FocusRequester() }
    val addFr = remember { FocusRequester() }
    val doneFr = remember { FocusRequester() }

    val screenW = LocalConfiguration.current.screenWidthDp
    // Phone: 2 cols · tablet/TV: at least 3
    val columns = when {
        screenW < 600 -> 2
        screenW < 900 -> 3
        else -> 4
    }

    LaunchedEffect(activeTabId, ids) {
        kotlinx.coroutines.delay(80)
        val fr = tabFr[activeTabId] ?: tabFr.values.firstOrNull() ?: addFr
        runCatching { fr.requestFocus() }
    }

    fun tabUp(index: Int): FocusRequester =
        if (index < columns) clearFr
        else tabFr[ids.getOrNull(index - columns)] ?: clearFr

    fun tabDown(index: Int): FocusRequester? =
        ids.getOrNull(index + columns)?.let { tabFr[it] }

    fun tabLeft(index: Int): FocusRequester? =
        if (index % columns != 0) tabFr[ids.getOrNull(index - 1)] else null

    fun tabRight(index: Int): FocusRequester? =
        if (index % columns != columns - 1) ids.getOrNull(index + 1)?.let { tabFr[it] } else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiColors.BgDeep)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 28f) onDismiss()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(DigiColors.BorderMuted, RoundedCornerShape(2.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.nav_tabs),
                        color = DigiColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${tabs.size}",
                        color = DigiColors.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    // Connected pill: Clear | + | Done (Digi theme)
                    var clearFocused by remember { mutableStateOf(false) }
                    var addFocused by remember { mutableStateOf(false) }
                    var doneFocused by remember { mutableStateOf(false) }
                    val pillBg = DigiColors.BgCard
                    val pillBorder = DigiColors.Border

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(pillBg, RoundedCornerShape(28.dp))
                            .border(1.dp, pillBorder, RoundedCornerShape(28.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.tabs_clear),
                            color = if (clearFocused) DigiColors.FocusOnAccent else DigiColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier
                                .focusRequester(clearFr)
                                .focusProperties {
                                    left = doneFr
                                    right = addFr
                                    down = tabFr[ids.firstOrNull()] ?: FocusRequester.Cancel
                                    up = FocusRequester.Cancel
                                }
                                .onFocusChanged { clearFocused = it.isFocused }
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (clearFocused) DigiColors.Accent else Color.Transparent)
                                .clickable(onClick = onClear)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .focusRequester(addFr)
                                .focusProperties {
                                    left = clearFr
                                    right = doneFr
                                    down = tabFr[ids.firstOrNull()] ?: FocusRequester.Cancel
                                    up = FocusRequester.Cancel
                                }
                                .onFocusChanged { addFocused = it.isFocused }
                                .background(
                                    if (addFocused) DigiColors.Accent else DigiColors.AccentDim,
                                    CircleShape
                                )
                                .border(
                                    if (addFocused) 2.dp else 0.dp,
                                    DigiColors.FocusRing,
                                    CircleShape
                                )
                                .clickable(onClick = onAdd)
                        ) {
                            Text(
                                "+",
                                color = if (addFocused) DigiColors.BgDeep else DigiColors.Accent,
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Text(
                            stringResource(R.string.tabs_done),
                            color = if (doneFocused) DigiColors.FocusOnAccent else DigiColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier
                                .focusRequester(doneFr)
                                .focusProperties {
                                    left = addFr
                                    right = clearFr
                                    down = tabFr[ids.firstOrNull()] ?: FocusRequester.Cancel
                                    up = FocusRequester.Cancel
                                }
                                .onFocusChanged { doneFocused = it.isFocused }
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (doneFocused) DigiColors.Accent else Color.Transparent)
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                Text(
                    stringResource(R.string.tabs_help),
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                if (tabs.isEmpty()) {
                    Text(
                        stringResource(R.string.tabs_none),
                        color = DigiColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    tabs.chunked(columns).forEachIndexed { rowIndex, rowTabs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTabs.forEachIndexed { colIndex, tab ->
                                val index = rowIndex * columns + colIndex
                                val selected = tab.id == activeTabId
                                var focused by remember(tab.id) { mutableStateOf(false) }
                                val fr = tabFr[tab.id]!!
                                val thumb = thumbs[tab.id]
                                val shape = RoundedCornerShape(16.dp)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(4f / 3f) // width:height = 4:3
                                        .focusRequester(fr)
                                        .focusProperties {
                                            up = tabUp(index)
                                            down = tabDown(index) ?: FocusRequester.Cancel
                                            left = tabLeft(index) ?: FocusRequester.Cancel
                                            right = tabRight(index) ?: FocusRequester.Cancel
                                        }
                                        .onFocusChanged { focused = it.isFocused }
                                        .onPreviewKeyEvent { ev ->
                                            val isOk = ev.key == Key.DirectionCenter ||
                                                ev.key == Key.Enter ||
                                                ev.key == Key.NumPadEnter
                                            if (!isOk) return@onPreviewKeyEvent false
                                            if (ev.type == KeyEventType.KeyUp) {
                                                onSelect(tab.id)
                                                true
                                            } else true
                                        }
                                        .clickable { onSelect(tab.id) }
                                        .background(DigiColors.BgCard, shape)
                                        .border(
                                            width = when {
                                                focused -> 2.5.dp
                                                selected -> 2.dp
                                                else -> 1.dp
                                            },
                                            color = when {
                                                focused -> DigiColors.FocusRing
                                                selected -> DigiColors.Accent
                                                else -> DigiColors.Border
                                            },
                                            shape = shape
                                        )
                                        .clip(shape)
                                ) {
                                    // Header strip
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DigiColors.BgPanel)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                tab.title.take(18).ifBlank { stringResource(R.string.tab_default_title) },
                                                color = DigiColors.TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                tab.url.take(28),
                                                color = DigiColors.TextMuted,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                        if (allowCloseLast || tabs.size > 1) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(DigiColors.BgCard, CircleShape)
                                                    .clickable { onClose(tab.id) }
                                            ) {
                                                Text("×", color = DigiColors.TextMuted, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    // Preview fills remaining card area (Crop, anchored top)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(DigiColors.PreviewBg)
                                    ) {
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                alignment = Alignment.TopCenter,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                hostOf(tab.url, "…"),
                                                color = DigiColors.TextMuted,
                                                fontSize = 12.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        if (selected) {
                                            Text(
                                                stringResource(R.string.tabs_current),
                                                color = DigiColors.Accent,
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .background(
                                                        DigiColors.BgDeep.copy(alpha = 0.8f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Pad incomplete last row
                            repeat(columns - rowTabs.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
