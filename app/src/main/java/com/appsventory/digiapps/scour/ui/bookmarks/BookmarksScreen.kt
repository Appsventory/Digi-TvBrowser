package com.appsventory.digiapps.scour.ui.bookmarks

import android.view.ViewConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.appsventory.digiapps.scour.data.BookmarkEntry
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.ui.shell.EmptyState
import com.appsventory.digiapps.scour.ui.shell.MenuItem
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

@Composable
fun BookmarksScreen(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(Prefs.getBookmarks(context)) }
    var menuEntry by remember { mutableStateOf<BookmarkEntry?>(null) }

    // Always sync from disk when screen is shown
    LaunchedEffect(Unit) {
        items = Prefs.getBookmarks(context)
    }

    fun refresh() {
        items = Prefs.getBookmarks(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.nav_bookmarks),
            color = DigiColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (items.isEmpty()) stringResource(R.string.bookmarks_empty_count) else stringResource(R.string.bookmarks_count, items.size),
            color = DigiColors.TextMuted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        if (items.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.BookmarkBorder, null,
                        tint = DigiColors.TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = stringResource(R.string.bookmarks_empty_title),
                body = stringResource(R.string.bookmarks_empty_body)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, entry ->
                    BookmarkRow(
                        entry = entry,
                        isFirst = index == 0,
                        onOpen = { onOpen(entry.url) },
                        onMenu = { menuEntry = entry }
                    )
                }
            }
        }
    }

    menuEntry?.let { entry ->
        Dialog(onDismissRequest = { menuEntry = null }) {
            Column(
                modifier = Modifier
                    .widthIn(min = 260.dp)
                    .background(DigiColors.BgPanel, RoundedCornerShape(16.dp))
                    .border(1.dp, DigiColors.Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    entry.title,
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                MenuItem(stringResource(R.string.action_open), onClick = {
                    menuEntry = null
                    onOpen(entry.url)
                })
                MenuItem(stringResource(R.string.action_remove), onClick = {
                    Prefs.removeBookmark(context, entry.url)
                    menuEntry = null
                    refresh()
                })
                MenuItem(stringResource(R.string.action_cancel), onClick = { menuEntry = null })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    entry: BookmarkEntry,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    isFirst: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val longPressTimeout = remember { ViewConfiguration.getLongPressTimeout().toLong() }
    var okDownAt by remember { mutableStateOf(0L) }

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
            .combinedClickable(onClick = onOpen, onLongClick = onMenu)
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
                .background(Color(entry.colorHex), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.title.take(1).uppercase(),
                color = DigiColors.OnDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                entry.title,
                color = DigiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.url,
                color = DigiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
