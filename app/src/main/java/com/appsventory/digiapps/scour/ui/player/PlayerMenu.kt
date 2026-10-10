package com.appsventory.digiapps.scour.ui.player

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.player.PlayerConfig
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import java.util.Locale

enum class PlayerMenuType { Streams, Quality, Subtitles, Speed, Aspect }

/** [ratio] != null forces a fixed frame (video is stretched into it). */
@OptIn(UnstableApi::class)
enum class AspectMode(
    @StringRes val labelRes: Int,
    val resizeMode: Int,
    val ratio: Float?
) {
    Fit(R.string.player_aspect_fit, AspectRatioFrameLayout.RESIZE_MODE_FIT, null),
    Fill(R.string.player_aspect_fill, AspectRatioFrameLayout.RESIZE_MODE_FILL, null),
    Zoom(R.string.player_aspect_zoom, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, null),
    Wide(R.string.player_aspect_16_9, AspectRatioFrameLayout.RESIZE_MODE_FILL, 16f / 9f),
    Classic(R.string.player_aspect_4_3, AspectRatioFrameLayout.RESIZE_MODE_FILL, 4f / 3f)
}

data class MenuEntry(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

/** Right-hand side panel. Lives in the same window as the player so D-pad focus stays reliable. */
@Composable
fun PlayerMenuPanel(
    title: String,
    entries: List<MenuEntry>,
    width: Dp,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val requesters = remember(entries.size) { List(entries.size) { FocusRequester() } }
    val startIndex = entries.indexOfFirst { it.selected }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        if (entries.isNotEmpty()) {
            listState.scrollToItem(startIndex)
            withFrameNanos { }
            runCatching { requesters[startIndex].requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(DigiColors.BgPanel)
            .padding(16.dp)
    ) {
        Text(
            title, color = DigiColors.OnDark, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                MenuRow(entry, requesters[index])
            }
        }
        if (footer != null) {
            Spacer(Modifier.size(8.dp))
            footer()
        }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry, focusRequester: FocusRequester) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val tint = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .background(
                when {
                    focused -> DigiColors.FocusFill
                    entry.selected -> DigiColors.AccentSoft
                    else -> Color.Transparent
                },
                shape
            )
            .then(if (focused) Modifier.border(2.dp, DigiColors.FocusRing, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.label, color = tint, fontSize = 16.sp, maxLines = 1,
            fontWeight = if (entry.selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (entry.selected) {
            Icon(
                Icons.Default.Check, contentDescription = null,
                tint = if (focused) DigiColors.FocusOnAccent else DigiColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Subtitle timing nudge. D-pad: Left/Right = ∓100 ms, OK = reset. Touch: tap the arrows.
 * Positive = subtitles appear later.
 */
@Composable
fun SubtitleTimingRow(
    offsetMs: Long,
    enabled: Boolean,
    onChange: (Long) -> Unit,
    onReset: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val tint = when {
        !enabled -> DigiColors.TextDisabled
        focused -> DigiColors.FocusOnAccent
        else -> DigiColors.TextPrimary
    }
    val step = PlayerConfig.SUBTITLE_NUDGE_STEP_MS
    val max = PlayerConfig.SUBTITLE_NUDGE_MAX_MS

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused && enabled) DigiColors.FocusFill else DigiColors.BgCard, shape)
                .then(if (focused && enabled) Modifier.border(2.dp, DigiColors.FocusRing, shape) else Modifier)
                .then(
                    if (enabled) {
                        Modifier
                            .onFocusChanged { focused = it.isFocused }
                            .focusable()
                            .onKeyEvent { ev ->
                                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                                when (ev.key) {
                                    Key.DirectionLeft -> { onChange((offsetMs - step).coerceIn(-max, max)); true }
                                    Key.DirectionRight -> { onChange((offsetMs + step).coerceIn(-max, max)); true }
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { onReset(); true }
                                    else -> false
                                }
                            }
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.player_subtitle_timing), color = tint, fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronLeft, contentDescription = null, tint = tint,
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(offsetMs, enabled) {
                        if (enabled) detectTapGestures { onChange((offsetMs - step).coerceIn(-max, max)) }
                    }
            )
            Text(
                stringResource(
                    R.string.player_seconds_fmt,
                    String.format(Locale.US, "%+.1f", offsetMs / 1000f)
                ),
                color = tint, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Icon(
                Icons.Default.ChevronRight, contentDescription = null, tint = tint,
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(offsetMs, enabled) {
                        if (enabled) detectTapGestures { onChange((offsetMs + step).coerceIn(-max, max)) }
                    }
            )
        }
        if (!enabled) {
            Text(
                stringResource(R.string.player_subtitle_timing_unavailable),
                color = DigiColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}
