package com.appsventory.digiapps.scour

import android.app.DownloadManager
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.data.BrowserTab
import com.appsventory.digiapps.scour.data.DownloadStatus
import com.appsventory.digiapps.scour.data.hostOf
import com.appsventory.digiapps.scour.data.isRealUrl
import com.appsventory.digiapps.scour.data.DownloadEntry
import com.appsventory.digiapps.scour.data.HistoryEntry
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.data.UpdateChecker
import com.appsventory.digiapps.scour.data.UpdateStatus
import com.appsventory.digiapps.scour.data.appVersionName
import com.appsventory.digiapps.scour.data.release
import com.appsventory.digiapps.scour.ui.update.UpdateOverflowMenu
import com.appsventory.digiapps.scour.data.TabThumbs
import com.appsventory.digiapps.scour.player.PlayerPip
import com.appsventory.digiapps.scour.ui.bookmarks.BookmarksScreen
import com.appsventory.digiapps.scour.ui.browser.BrowserScreen
import com.appsventory.digiapps.scour.ui.browser.TabSwitcherOverlay
import com.appsventory.digiapps.scour.ui.downloads.DownloadActions
import com.appsventory.digiapps.scour.ui.downloads.DownloadItem
import com.appsventory.digiapps.scour.ui.downloads.DownloadsScreen
import com.appsventory.digiapps.scour.ui.history.HistoryItem
import com.appsventory.digiapps.scour.ui.history.HistoryScreen
import com.appsventory.digiapps.scour.ui.home.HomeScreen
import com.appsventory.digiapps.scour.ui.search.SearchScreen
import com.appsventory.digiapps.scour.ui.settings.SettingsScreen
import com.appsventory.digiapps.scour.ui.shell.AppScaffold
import com.appsventory.digiapps.scour.ui.shell.AppTab
import com.appsventory.digiapps.scour.ui.theme.DigiBrowserTheme
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import com.appsventory.digiapps.scour.ui.update.UpdateScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.SystemClock
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep content laid out inside system windows so SOFT_INPUT_ADJUST_RESIZE
        // can shrink the hierarchy when the IME opens (toolbar stays visible).
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            DigiBrowserTheme {
                // Explicit deep background so adjustResize / IME never reveals system white.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DigiColors.BgDeep
                ) {
                    DigiBrowserApp()
                }
            }
        }
    }

    /** WebView writes cookies lazily; flush so a login survives the process being killed. */
    override fun onStop() {
        super.onStop()
        CookieManager.getInstance().flush()
    }

    /** Home pressed while the native player is playing → shrink into picture-in-picture. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PlayerPip.onUserLeaveHint?.invoke()
    }
}

@Composable
private fun DigiBrowserApp() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AppTab.Home) }

    // Saved tabs — single source of truth for Home, sidebar and Browser
    var tabs by remember { mutableStateOf(Prefs.getTabs(context)) }
    var activeTabId by remember { mutableStateOf(Prefs.getActiveTabId(context)) }
    var showTabs by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    // Active browser page (null = not in browser UI)
    var browserUrl by remember {
        val session = Prefs.getSessionUrl(context)
        mutableStateOf(
            if (session == null) null
            else tabs.firstOrNull { it.id == activeTabId }?.url ?: session
        )
    }
    // Leave browser → History/etc., Back returns here (not Home)
    var resumeWebUrl by remember { mutableStateOf<String?>(null) }

    var history by remember {
        mutableStateOf(
            Prefs.getHistory(context).map {
                HistoryItem(it.title, it.url, it.timeLabel)
            }
        )
    }
    var downloads by remember {
        mutableStateOf(
            Prefs.getDownloads(context).map {
                // Re-query in-progress downloads on launch
                DownloadItem(it.id, it.name, it.sizeLabel, it.status, it.progress)
            }
        )
    }
    var searchEngine by remember { mutableStateOf(Prefs.getSearchEngine(context)) }
    var userAgentPreset by remember { mutableStateOf(Prefs.getUserAgentPreset(context)) }
    var cursorHoldSpeed by remember { mutableStateOf(Prefs.getCursorHoldSpeed(context)) }
    var adBlockEnabled by remember { mutableStateOf(Prefs.isAdBlockEnabled(context)) }
    var strongAdBlock by remember { mutableStateOf(Prefs.isStrongAdBlock(context)) }
    var jsEnabled by remember { mutableStateOf(Prefs.isJsEnabled(context)) }
    var blockPopups by remember { mutableStateOf(Prefs.isBlockPopups(context)) }
    var saveHistoryEnabled by remember { mutableStateOf(Prefs.isSaveHistory(context)) }
    var privateDns by remember { mutableStateOf(Prefs.getPrivateDns(context)) }

    fun persistHistory(list: List<HistoryItem>) {
        Prefs.setHistory(
            context,
            list.map { HistoryEntry(it.title, it.url, it.timeLabel) }
        )
    }

    fun persistDownloads(list: List<DownloadItem>) {
        Prefs.setDownloads(
            context,
            list.map {
                DownloadEntry(it.id, it.name, it.sizeLabel, it.status, it.progress)
            }
        )
    }

    fun recordHistory(title: String, url: String) {
        if (!url.isRealUrl()) return
        Prefs.setSessionUrl(context, url)
        if (!Prefs.isSaveHistory(context)) return
        val host = hostOf(url, title)
        val displayTitle = title.ifBlank { host }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val last = history.firstOrNull()
        val next = if (last?.url == url) {
            if (last.title != displayTitle) {
                listOf(last.copy(title = displayTitle)) + history.drop(1)
            } else {
                history
            }
        } else {
            listOf(HistoryItem(displayTitle, url, time)) + history
        }
        if (next !== history) {
            history = next
            persistHistory(next)
        }
    }

    fun saveTabs(list: List<BrowserTab>, active: Long) {
        tabs = list
        activeTabId = active
        Prefs.setTabs(context, list, active)
    }

    fun newTab(url: String, title: String): BrowserTab {
        val tab = BrowserTab(System.currentTimeMillis(), url, title)
        // Hard cap: drop the oldest tab (never the active one) instead of growing forever
        var base = tabs
        while (base.size >= BrowserConfig.MAX_TABS) {
            val drop = base.firstOrNull { it.id != activeTabId } ?: break
            TabThumbs.map.remove(drop.id)
            base = base - drop
        }
        saveTabs(base + tab, tab.id)
        return tab
    }

    fun openUrl(url: String) {
        newTab(
            url = url,
            title = hostOf(url)
        )
        recordHistory(
            title = hostOf(url),
            url = url
        )
        browserUrl = url
    }

    fun refreshDownloadStatuses() {
        val dm = context.getSystemService(DownloadManager::class.java) ?: return
        val updated = downloads.map { item ->
            if (item.id <= 0L) return@map item
            if (item.status == DownloadStatus.COMPLETED || item.status == DownloadStatus.FAILED) return@map item
            val q = DownloadManager.Query().setFilterById(item.id)
            dm.query(q)?.use { cursor ->
                if (!cursor.moveToFirst()) return@map item
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val progress = when {
                    total > 0L -> (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    status == DownloadManager.STATUS_SUCCESSFUL -> 1f
                    else -> item.progress.coerceAtLeast(0.05f)
                }
                val sizeLabel = when {
                    total > 0L -> String.format("%.1f MB", total / (1024.0 * 1024.0))
                    else -> item.sizeLabel
                }
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL ->
                        item.copy(status = DownloadStatus.COMPLETED, progress = 1f, sizeLabel = sizeLabel)
                    DownloadManager.STATUS_FAILED ->
                        item.copy(status = DownloadStatus.FAILED, progress = 0f)
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_PAUSED ->
                        item.copy(status = DownloadStatus.DOWNLOADING, progress = progress, sizeLabel = sizeLabel)
                    else -> item.copy(progress = progress)
                }
            } ?: item
        }
        if (updated != downloads) {
            downloads = updated
            persistDownloads(updated)
        }
    }

    // Fast poll while any download is active
    LaunchedEffect(downloads.any { it.status == DownloadStatus.DOWNLOADING }) {
        while (downloads.any { it.status == DownloadStatus.DOWNLOADING }) {
            refreshDownloadStatuses()
            delay(BrowserConfig.DOWNLOAD_POLL_MS)
        }
        // one more pass to catch completion edge
        refreshDownloadStatuses()
    }

    // On start, refresh any leftover DownloadStatus.DOWNLOADING rows
    LaunchedEffect(Unit) {
        if (downloads.any { it.status == DownloadStatus.DOWNLOADING }) {
            refreshDownloadStatuses()
        }
    }

    fun addDownload(id: Long, name: String, sizeLabel: String) {
        val next = listOf(
            DownloadItem(
                id = id,
                name = name,
                sizeLabel = sizeLabel,
                status = DownloadStatus.DOWNLOADING,
                progress = 0.02f
            )
        ) + downloads.filterNot { it.id == id }
        downloads = next
        persistDownloads(next)
    }

    // ---- App updates (GitHub releases) ----
    val currentVersion = remember { appVersionName(context) }
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var lastUpdateCheckMs by remember { mutableLongStateOf(0L) }
    var showUpdate by remember { mutableStateOf(false) }
    var updateDownloadId by remember { mutableLongStateOf(-1L) }

    fun checkForUpdates(force: Boolean) {
        if (updateStatus is UpdateStatus.Checking) return
        val fresh = updateStatus is UpdateStatus.Available || updateStatus is UpdateStatus.UpToDate
        val recent = SystemClock.elapsedRealtime() - lastUpdateCheckMs < BrowserConfig.UPDATE_CHECK_INTERVAL_MS
        if (!force && fresh && recent) return
        updateStatus = UpdateStatus.Checking
        scope.launch {
            updateStatus = UpdateChecker.evaluate(UpdateChecker.fetchLatest(), currentVersion)
            lastUpdateCheckMs = SystemClock.elapsedRealtime()
        }
    }

    // Opening Settings checks GitHub (throttled); leaving Settings closes the update page.
    LaunchedEffect(tab) {
        if (tab == AppTab.Settings) checkForUpdates(force = false) else showUpdate = false
    }

    // SearchScreen owns Back while open (IME dismiss first, then leave page).
    BackHandler(enabled = browserUrl == null && !showSearch && (tab != AppTab.Home || resumeWebUrl != null)) {
        val resume = resumeWebUrl
        if (resume != null) {
            resumeWebUrl = null
            browserUrl = resume
        } else if (tab != AppTab.Home) {
            tab = AppTab.Home
        }
    }

    // Registered after the one above → wins while the update page is open.
    BackHandler(enabled = browserUrl == null && showUpdate && tab == AppTab.Settings) {
        showUpdate = false
    }

    if (browserUrl != null) {
        BrowserScreen(
            initialUrl = browserUrl!!,
            initialTabs = tabs,
            initialActiveTabId = activeTabId,
            onTabsChanged = { list, active -> saveTabs(list, active) },
            searchEngine = searchEngine,
            privateDns = privateDns,
            onGoHome = {
                // Explicit Home → next cold start opens Home, not last web
                resumeWebUrl = null
                Prefs.setSessionUrl(context, null)
                browserUrl = null
                tab = AppTab.Home
            },
            onOpenHistory = { currentUrl ->
                resumeWebUrl = currentUrl
                Prefs.setSessionUrl(context, currentUrl)
                browserUrl = null
                tab = AppTab.History
            },
            onOpenDownloads = { currentUrl ->
                resumeWebUrl = currentUrl
                Prefs.setSessionUrl(context, currentUrl)
                browserUrl = null
                tab = AppTab.Downloads
            },
            onOpenSettings = { currentUrl ->
                resumeWebUrl = currentUrl
                Prefs.setSessionUrl(context, currentUrl)
                browserUrl = null
                tab = AppTab.Settings
            },
            onOpenBookmarks = { currentUrl ->
                resumeWebUrl = currentUrl
                Prefs.setSessionUrl(context, currentUrl)
                browserUrl = null
                tab = AppTab.Bookmarks
            },
            onHistoryVisit = { title, url -> recordHistory(title, url) },
            onDownload = { id, name, sizeLabel -> addDownload(id, name, sizeLabel) },
            knownDownloadNames = downloads.map { it.name }.toSet()
        )
        return
    }

    if (showSearch) {
        SearchScreen(
            searchEngine = searchEngine,
            onSearchEngineChange = { engine ->
                searchEngine = engine
                Prefs.setSearchEngine(context, engine)
            },
            onSubmit = { url ->
                showSearch = false
                openUrl(url)
            },
            onOpenHistory = {
                showSearch = false
                tab = AppTab.History
            },
            onDismiss = { showSearch = false }
        )
        return
    }

    AppScaffold(
        selected = tab,
        onSelect = { tab = it },
        tabCount = tabs.size,
        onOpenTabs = { showTabs = true },
        headerTitle = if (tab == AppTab.Settings && showUpdate) context.getString(R.string.update_title) else null,
        headerActions = if (tab == AppTab.Settings && showUpdate) {
            {
                UpdateOverflowMenu(
                    onCheck = { checkForUpdates(force = true) },
                    onViewGitHub = {
                        openUrl(updateStatus.release?.pageUrl ?: BrowserConfig.UPDATE_RELEASES_URL)
                    }
                )
            }
        } else null
    ) {
        when (tab) {
            AppTab.Home -> HomeScreen(
                onNavigate = { openUrl(it) },
                searchEngine = searchEngine,
                tabCount = tabs.size,
                onOpenTabs = { showTabs = true },
                onOpenSearch = { showSearch = true }
            )
            AppTab.Downloads -> DownloadsScreen(
                items = downloads,
                onClear = { deleteFiles ->
                    // Snapshot current list only — never touch files removed from the list earlier.
                    val snapshot = downloads.toList()
                    if (deleteFiles) {
                        snapshot.forEach { item ->
                            DownloadActions.deleteFromDisk(context, item)
                        }
                    }
                    downloads = emptyList()
                    persistDownloads(emptyList())
                },
                onRemove = { item, deleteFile ->
                    if (deleteFile) {
                        DownloadActions.deleteFromDisk(context, item)
                    }
                    val next = downloads.filterNot {
                        // Match by id when known; id+name otherwise (legacy rows).
                        if (item.id > 0L) it.id == item.id
                        else it.id == item.id && it.name == item.name
                    }
                    downloads = next
                    persistDownloads(next)
                }
            )
            AppTab.Bookmarks -> BookmarksScreen(onOpen = { openUrl(it) })
            AppTab.History -> HistoryScreen(
                items = history,
                onOpen = { openUrl(it) },
                onClear = {
                    history = emptyList()
                    persistHistory(emptyList())
                }
            )
            AppTab.Settings -> if (showUpdate) UpdateScreen(
                currentVersion = currentVersion,
                status = updateStatus,
                downloadId = updateDownloadId,
                onDownloadStarted = { id, name, sizeLabel ->
                    updateDownloadId = id
                    addDownload(id, name, sizeLabel)
                },
                onBack = { showUpdate = false }
            ) else SettingsScreen(
                updateStatus = updateStatus,
                onOpenUpdate = { showUpdate = true },
                searchEngine = searchEngine,
                onSearchEngineChange = { engine ->
                    searchEngine = engine
                    Prefs.setSearchEngine(context, engine)
                },
                userAgentPreset = userAgentPreset,
                onUserAgentPresetChange = { preset ->
                    userAgentPreset = preset
                    Prefs.setUserAgentPreset(context, preset)
                },
                cursorHoldSpeed = cursorHoldSpeed,
                onCursorHoldSpeedChange = { value ->
                    cursorHoldSpeed = value
                    Prefs.setCursorHoldSpeed(context, value)
                },
                adBlockEnabled = adBlockEnabled,
                onAdBlockChange = { enabled ->
                    adBlockEnabled = enabled
                    Prefs.setAdBlockEnabled(context, enabled)
                },
                strongAdBlock = strongAdBlock,
                onStrongAdBlockChange = { enabled ->
                    strongAdBlock = enabled
                    Prefs.setStrongAdBlock(context, enabled)
                },
                jsEnabled = jsEnabled,
                onJsChange = { enabled ->
                    jsEnabled = enabled
                    Prefs.setJsEnabled(context, enabled)
                },
                blockPopups = blockPopups,
                onBlockPopupsChange = { enabled ->
                    blockPopups = enabled
                    Prefs.setBlockPopups(context, enabled)
                },
                saveHistory = saveHistoryEnabled,
                onSaveHistoryChange = { enabled ->
                    saveHistoryEnabled = enabled
                    Prefs.setSaveHistory(context, enabled)
                },
                privateDns = privateDns,
                onPrivateDnsChange = { provider ->
                    privateDns = provider
                    Prefs.setPrivateDns(context, provider)
                }
            )
        }
    }

    if (showTabs) {
        TabSwitcherOverlay(
            tabs = tabs,
            activeTabId = activeTabId,
            thumbs = TabThumbs.map,
            allowCloseLast = true,
            onSelect = { id ->
                val target = tabs.firstOrNull { it.id == id } ?: return@TabSwitcherOverlay
                saveTabs(tabs, id)
                showTabs = false
                browserUrl = target.url
            },
            onClose = { id ->
                val idx = tabs.indexOfFirst { it.id == id }
                val next = tabs.filterNot { it.id == id }
                TabThumbs.map.remove(id)
                val newActive = if (activeTabId == id) {
                    next.getOrNull(idx.coerceAtMost(next.lastIndex))?.id ?: -1L
                } else activeTabId
                saveTabs(next, newActive)
            },
            onAdd = {
                val t = newTab(BrowserConfig.HOME_URL, "New tab")
                showTabs = false
                browserUrl = t.url
            },
            onClear = {
                TabThumbs.map.clear()
                saveTabs(emptyList(), -1L)
                showTabs = false
            },
            onDismiss = { showTabs = false }
        )
    }
}
