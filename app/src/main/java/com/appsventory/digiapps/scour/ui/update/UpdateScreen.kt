package com.appsventory.digiapps.scour.ui.update

import android.app.DownloadManager
import android.os.Environment
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.data.DownloadStatus
import com.appsventory.digiapps.scour.data.ReleaseAsset
import com.appsventory.digiapps.scour.data.UpdateChecker
import com.appsventory.digiapps.scour.data.UpdateStatus
import com.appsventory.digiapps.scour.data.release
import com.appsventory.digiapps.scour.ui.downloads.DownloadActions
import com.appsventory.digiapps.scour.ui.downloads.DownloadItem
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(16.dp)
private const val NOTES_SCROLL_STEP_PX = 240f
private const val PERCENT = 100
private const val BUTTON_HEIGHT_DP = 52
private const val BUTTON_LETTER_SPACING_SP = 2

private sealed interface ApkDownload {
    data object None : ApkDownload
    data class Running(val fraction: Float) : ApkDownload
    data class Done(val name: String) : ApkDownload
    data object Failed : ApkDownload
}

private fun queryDownload(dm: DownloadManager?, id: Long): ApkDownload {
    if (dm == null || id <= 0L) return ApkDownload.None
    dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
        if (!c.moveToFirst()) return ApkDownload.None
        val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val got = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        val title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)).orEmpty()
        return when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> ApkDownload.Done(title)
            DownloadManager.STATUS_FAILED -> ApkDownload.Failed
            else -> ApkDownload.Running(if (total > 0) got.toFloat() / total else 0f)
        }
    }
    return ApkDownload.None
}

/**
 * Update page. Header (title + ⋮ menu) is drawn by the scaffold; only the release notes scroll.
 * Bottom button: BACK, or DOWNLOAD / INSTALL when an update is available. With several APKs
 * attached to the release, DOWNLOAD first asks which one.
 */
@Composable
fun UpdateScreen(
    currentVersion: String,
    status: UpdateStatus,
    downloadId: Long,
    onDownloadStarted: (id: Long, name: String, sizeLabel: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dm = remember { context.getSystemService(DownloadManager::class.java) }
    var apk by remember { mutableStateOf<ApkDownload>(ApkDownload.None) }
    var enqueueFailed by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }

    LaunchedEffect(downloadId) {
        while (downloadId > 0L) {
            val state = queryDownload(dm, downloadId)
            apk = state
            if (state !is ApkDownload.Running) break
            delay(BrowserConfig.UPDATE_PROGRESS_POLL_MS)
        }
    }

    val release = status.release
    val available = (status as? UpdateStatus.Available)?.release

    fun startDownload(asset: ReleaseAsset) {
        enqueueFailed = false
        choosing = false
        // Same file name would be renamed "(1)" by DownloadManager; clear the old copy first.
        DownloadActions.deleteFromDisk(
            context, DownloadItem(name = asset.name, sizeLabel = "", status = DownloadStatus.COMPLETED)
        )
        val id = runCatching {
            dm?.enqueue(
                DownloadManager.Request(android.net.Uri.parse(asset.url)).apply {
                    setTitle(asset.name)
                    setDescription(context.getString(R.string.download_desc, asset.name))
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, asset.name)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
            )
        }.getOrNull() ?: -1L
        if (id > 0L) {
            onDownloadStarted(id, asset.name, Formatter.formatShortFileSize(context, asset.sizeBytes))
        } else enqueueFailed = true
    }

    val running = apk as? ApkDownload.Running
    val done = apk as? ApkDownload.Done
    val buttonText = when {
        available == null -> stringResource(R.string.panel_back)
        available.assets.isEmpty() -> stringResource(R.string.panel_back)
        done != null -> stringResource(R.string.update_install)
        running != null -> stringResource(R.string.update_downloading, (running.fraction * PERCENT).toInt())
        else -> stringResource(R.string.update_download)
    }.uppercase()
    val buttonAction: () -> Unit = when {
        available == null || available.assets.isEmpty() -> onBack
        done != null -> {
            {
                DownloadActions.openFile(
                    context,
                    DownloadItem(id = downloadId, name = done.name, sizeLabel = "", status = DownloadStatus.COMPLETED)
                )
            }
        }
        running != null -> {
            {}
        }
        available.assets.size == 1 -> {
            { startDownload(available.assets.first()) }
        }
        else -> {
            { choosing = true }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ——— App + versions (fixed) ———
        Card {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.app_name),
                        color = DigiColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (status is UpdateStatus.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = DigiColors.Accent
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(
                                when (status) {
                                    is UpdateStatus.Available -> R.string.update_status_available
                                    is UpdateStatus.UpToDate -> R.string.update_status_latest
                                    UpdateStatus.Failed -> R.string.update_status_failed
                                    else -> R.string.update_status_checking
                                }
                            ),
                            color = if (status is UpdateStatus.Available) DigiColors.Accent else DigiColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
                Box(
                    Modifier.padding(horizontal = 16.dp).width(1.dp).height(56.dp).background(DigiColors.Border)
                )
                Column(horizontalAlignment = Alignment.End) {
                    VersionLine(stringResource(R.string.update_installed), currentVersion, DigiColors.TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    VersionLine(
                        stringResource(R.string.update_latest),
                        release?.tag ?: stringResource(R.string.settings_update_unknown_version),
                        if (available != null) DigiColors.Accent else DigiColors.TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ——— What's new: title fixed, only the description scrolls ———
        if (release != null) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        stringResource(R.string.update_whats_new),
                        color = DigiColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        release.title, color = DigiColors.TextPrimary, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        stringResource(R.string.update_released, UpdateChecker.formatDate(release.publishedAt)),
                        color = DigiColors.TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    ScrollableNotes(release.notes, Modifier.weight(1f))
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        if (running != null) {
            LinearProgressIndicator(
                progress = { running.fraction },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = DigiColors.Accent, trackColor = DigiColors.BgSelected
            )
        }
        if (apk is ApkDownload.Failed || enqueueFailed) {
            Text(
                stringResource(R.string.update_download_failed),
                color = DigiColors.Accent, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (available != null && available.assets.isEmpty()) {
            Text(
                stringResource(R.string.update_no_apk),
                color = DigiColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        BottomButton(
            text = buttonText,
            primary = available != null && available.assets.isNotEmpty(),
            onClick = buttonAction
        )
    }

    if (choosing && available != null) {
        AssetChooser(
            assets = available.assets,
            onPick = { startDownload(it) },
            onDismiss = { choosing = false }
        )
    }
}

@Composable
private fun VersionLine(label: String, value: String, valueColor: Color) {
    Text(label, color = DigiColors.TextMuted, fontSize = 11.sp, textAlign = TextAlign.End)
    Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
}

/** D-pad scrollable description: ↑/↓ scroll, and hand focus on once an end is reached. */
@Composable
private fun ScrollableNotes(notes: String, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) DigiColors.FocusRing else DigiColors.Border, shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.key) {
                    Key.DirectionDown -> if (scroll.value < scroll.maxValue) {
                        scope.launch { scroll.animateScrollBy(NOTES_SCROLL_STEP_PX) }; true
                    } else false
                    Key.DirectionUp -> if (scroll.value > 0) {
                        scope.launch { scroll.animateScrollBy(-NOTES_SCROLL_STEP_PX) }; true
                    } else false
                    else -> false
                }
            }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(12.dp)) {
            if (notes.isBlank()) {
                Text(stringResource(R.string.update_no_notes), color = DigiColors.TextMuted, fontSize = 14.sp)
            } else {
                MarkdownText(notes)
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DigiColors.BgCard)
            .border(1.dp, DigiColors.Border, CardShape)
    ) { content() }
}

/** Full-width bottom button; first focus target of the page (D-pad lands here). */
@Composable
private fun BottomButton(text: String, primary: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT_DP.dp)
            .contentEnterFocus()
            .background(
                when {
                    focused -> DigiColors.FocusFill
                    primary -> DigiColors.Accent
                    else -> DigiColors.BgSelected
                },
                shape
            )
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (focused || primary) DigiColors.FocusOnAccent else DigiColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = BUTTON_LETTER_SPACING_SP.sp
        )
    }
}

/** Modal list of the release's APKs with sizes. D-pad: ↑/↓ + OK; Back closes. */
@Composable
private fun AssetChooser(
    assets: List<ReleaseAsset>,
    onPick: (ReleaseAsset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { first.requestFocus() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(DigiColors.BgPanel)
                .border(1.dp, DigiColors.Border, CardShape)
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.update_choose_title),
                color = DigiColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                itemsIndexed(assets) { index, asset ->
                    AssetRow(
                        asset = asset,
                        sizeLabel = Formatter.formatShortFileSize(context, asset.sizeBytes),
                        recommended = asset.name.contains("universal", ignoreCase = true),
                        focusRequester = if (index == 0) first else null,
                        onClick = { onPick(asset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    asset: ReleaseAsset,
    sizeLabel: String,
    recommended: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val primary = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    val muted = if (focused) DigiColors.FocusOnAccent else DigiColors.TextMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(if (focused) DigiColors.FocusFill else DigiColors.BgCard, shape)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(asset.name, color = primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            if (recommended) {
                Text(
                    stringResource(R.string.update_choose_recommended),
                    color = if (focused) DigiColors.FocusOnAccent else DigiColors.Accent, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(sizeLabel, color = muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
