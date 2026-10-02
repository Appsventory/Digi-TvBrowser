package com.appsventory.digiapps.scour.ui.downloads

import android.view.ViewConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.appsventory.digiapps.scour.data.DownloadStatus
import com.appsventory.digiapps.scour.ui.shell.ClearButton
import com.appsventory.digiapps.scour.ui.shell.EmptyState
import com.appsventory.digiapps.scour.ui.shell.MenuItem
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

data class DownloadItem(
    val id: Long = 0L,
    val name: String,
    val sizeLabel: String,
    val status: String, // "completed" | "downloading" | "failed"
    val progress: Float = 1f
)

@Composable
fun DownloadsScreen(
    items: List<DownloadItem>,
    onClear: () -> Unit,
    onRemove: (DownloadItem, deleteFile: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuItem by remember { mutableStateOf<DownloadItem?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.nav_downloads),
                    color = DigiColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (items.isEmpty()) stringResource(R.string.downloads_empty_count) else stringResource(R.string.downloads_count, items.size),
                    color = DigiColors.TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (items.isNotEmpty()) {
                ClearButton(onClick = onClear)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (items.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.Download, null,
                        tint = DigiColors.TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = stringResource(R.string.downloads_empty_title),
                body = stringResource(R.string.downloads_empty_body)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    DownloadRow(
                        item = item,
                        isFirst = index == 0,
                        onOpen = {
                            if (item.status == DownloadStatus.COMPLETED) {
                                val ok = DownloadActions.openFile(context, item)
                                if (!ok) toast = context.getString(R.string.download_cant_open)
                            }
                        },
                        onMenu = { menuItem = item }
                    )
                }
            }
        }
    }

    menuItem?.let { target ->
        DownloadContextMenu(
            item = target,
            onDismiss = { menuItem = null },
            onOpen = {
                menuItem = null
                val ok = DownloadActions.openFile(context, target)
                if (!ok) toast = context.getString(R.string.download_cant_open)
            },
            onOpenFolder = {
                menuItem = null
                DownloadActions.openInFolder(context)
            },
            onDeleteList = {
                menuItem = null
                onRemove(target, false)
            },
            onDeleteFile = {
                menuItem = null
                DownloadActions.deleteFromDisk(context, target)
                onRemove(target, true)
            }
        )
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            delay(2000)
            toast = null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(
    item: DownloadItem,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    isFirst: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val longPressTimeout = remember { ViewConfiguration.getLongPressTimeout().toLong() }
    var okDownAt by remember { mutableStateOf(0L) }
    val isApk = DownloadActions.isApk(item.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFirst) Modifier.contentEnterFocus() else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                val isOk =
                    ev.key == Key.DirectionCenter ||
                        ev.key == Key.Enter ||
                        ev.key == Key.NumPadEnter
                if (!isOk) return@onPreviewKeyEvent false
                when (ev.type) {
                    KeyEventType.KeyDown -> {
                        if (okDownAt == 0L) okDownAt = android.os.SystemClock.uptimeMillis()
                        true
                    }
                    KeyEventType.KeyUp -> {
                        val held = android.os.SystemClock.uptimeMillis() - okDownAt
                        okDownAt = 0L
                        if (held >= longPressTimeout) onMenu() else onOpen()
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onMenu
            )
            .background(
                if (focused) DigiColors.BgSelected else DigiColors.BgCard,
                shape
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DigiColors.BgDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.InsertDriveFile, null,
                tint = DigiColors.Accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                item.name,
                color = DigiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.sizeLabel} · ${
                    when (item.status) {
                        DownloadStatus.COMPLETED -> if (isApk) stringResource(R.string.download_done_install) else stringResource(R.string.download_done)
                        DownloadStatus.DOWNLOADING -> stringResource(R.string.download_in_progress)
                        DownloadStatus.FAILED -> stringResource(R.string.download_failed)
                        else -> item.status
                    }
                }",
                color = DigiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = DigiColors.Accent,
                    trackColor = DigiColors.Border
                )
            }
        }
    }
}
@Composable
private fun DownloadContextMenu(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenFolder: () -> Unit,
    onDeleteList: () -> Unit,
    onDeleteFile: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(1)
        runCatching { firstFocus.requestFocus() }
    }
    val openLabel = if (DownloadActions.isApk(item.name)) stringResource(R.string.download_menu_install) else stringResource(R.string.download_menu_view)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp)
                .background(DigiColors.BgPanel, RoundedCornerShape(16.dp))
                .border(1.dp, DigiColors.Border, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            Text(
                item.name,
                color = DigiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            MenuItem(openLabel, onOpen, firstFocus)
            MenuItem(stringResource(R.string.download_show_folder), onOpenFolder)
            MenuItem(stringResource(R.string.download_remove_list), onDeleteList)
            MenuItem(stringResource(R.string.download_delete_file), onDeleteFile)
            MenuItem(stringResource(R.string.action_cancel), onDismiss)
        }
    }
}
