package com.appsventory.digiapps.scour.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.ui.theme.DigiColors

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L

fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L)) / MS_PER_SECOND
    val s = total % SECONDS_PER_MINUTE
    val m = (total / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
    val h = total / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Remote-friendly button: filled accent background + ring on focus (same language as the browser panel). */
@Composable
fun PlayerButton(
    icon: ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    size: Dp = 48.dp,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (text == null) CircleShape else RoundedCornerShape(size / 2)
    val tint = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    Row(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .height(size)
            .then(if (text == null) Modifier.width(size) else Modifier.defaultMinSize(minWidth = size))
            .background(if (focused) DigiColors.FocusFill else DigiColors.BgCard, shape)
            .then(if (focused) Modifier.border(2.dp, DigiColors.FocusRing, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = if (text == null) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size / 2)
            )
        }
        if (icon != null && text != null) Spacer(Modifier.width(8.dp))
        if (text != null) {
            Text(text, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * Seek bar that is also a D-pad target: Left/Right seek, OK toggles play/pause.
 * Touch: tap or drag to scrub.
 */
@Composable
fun PlayerSeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onSeekBy: (direction: Int, repeatCount: Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    var dragMs by remember { mutableStateOf<Long?>(null) }

    fun msAt(x: Float): Long =
        if (widthPx <= 0 || durationMs <= 0) 0L
        else ((x / widthPx).coerceIn(0f, 1f) * durationMs).toLong()

    val shown = dragMs ?: positionMs
    val playedFrac = if (durationMs > 0) (shown.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedFrac = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val dragging = dragMs != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.key) {
                    Key.DirectionLeft -> {
                        if (enabled) onSeekBy(-1, ev.nativeKeyEvent.repeatCount)
                        true
                    }
                    Key.DirectionRight -> {
                        if (enabled) onSeekBy(1, ev.nativeKeyEvent.repeatCount)
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onTogglePlay()
                        true
                    }
                    else -> false
                }
            }
            .onSizeChanged { widthPx = it.width }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset -> onSeekTo(msAt(offset.x)) }
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragMs = msAt(offset.x) },
                    onHorizontalDrag = { change, _ -> dragMs = msAt(change.position.x) },
                    onDragEnd = {
                        dragMs?.let(onSeekTo)
                        dragMs = null
                    },
                    onDragCancel = { dragMs = null }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackH = (if (focused || dragging) 8.dp else 4.dp).toPx()
            val top = (size.height - trackH) / 2f
            val radius = CornerRadius(trackH / 2f)
            drawRoundRect(DigiColors.PlayerTrack, Offset(0f, top), Size(size.width, trackH), radius)
            drawRoundRect(
                DigiColors.PlayerBuffered, Offset(0f, top),
                Size(size.width * bufferedFrac, trackH), radius
            )
            drawRoundRect(
                DigiColors.Accent, Offset(0f, top),
                Size(size.width * playedFrac, trackH), radius
            )
            if (focused || dragging) {
                val c = Offset(size.width * playedFrac, size.height / 2f)
                drawCircle(DigiColors.OnDark, 12.dp.toPx(), c)
                drawCircle(DigiColors.Accent, 8.dp.toPx(), c)
            }
        }
    }
}

/** Top bar + bottom transport. Pure UI — all state and actions come from the screen. */
@Composable
fun PlayerControlsOverlay(
    title: String,
    subtitleLine: String,
    isTv: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    isLive: Boolean,
    seekable: Boolean,
    showStreams: Boolean,
    qualityLabel: String?,
    showSubtitles: Boolean,
    speedLabel: String,
    showPip: Boolean,
    seekFocus: FocusRequester,
    onClose: () -> Unit,
    onSeekBy: (direction: Int, repeatCount: Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onStreams: () -> Unit,
    onQuality: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit,
    onAspect: () -> Unit,
    onPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val btn = if (isTv) 56.dp else 48.dp
    val edge = if (isTv) 48.dp else 16.dp
    val playFocus = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to DigiColors.PlayerScrim,
                    0.3f to androidx.compose.ui.graphics.Color.Transparent,
                    0.65f to androidx.compose.ui.graphics.Color.Transparent,
                    1f to DigiColors.PlayerScrim
                )
            )
    ) {
        // ——— Top: back + title ———
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.panel_back),
                onClick = onClose,
                size = btn
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title, color = DigiColors.OnDark, fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1
                )
                Text(subtitleLine, color = DigiColors.TextMuted, fontSize = 13.sp, maxLines = 1)
            }
        }

        // ——— Bottom: time, seek bar, buttons ———
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = DigiColors.OnDark, fontSize = 14.sp)
                if (isLive || durationMs <= 0L) {
                    Text(
                        stringResource(R.string.player_live), color = DigiColors.Accent,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(formatTime(durationMs), color = DigiColors.TextSecondary, fontSize = 14.sp)
                }
            }
            PlayerSeekBar(
                positionMs = positionMs,
                bufferedMs = bufferedMs,
                durationMs = durationMs,
                enabled = seekable && durationMs > 0L,
                focusRequester = seekFocus,
                onSeekBy = onSeekBy,
                onSeekTo = onSeekTo,
                onTogglePlay = onTogglePlay,
                modifier = Modifier.focusProperties { down = playFocus }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { up = seekFocus },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = stringResource(R.string.player_rewind),
                        onClick = { onSeekBy(-1, 0) },
                        size = btn
                    )
                    Spacer(Modifier.width(12.dp))
                    PlayerButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.player_pause else R.string.player_play
                        ),
                        onClick = onTogglePlay,
                        size = btn,
                        focusRequester = playFocus
                    )
                    Spacer(Modifier.width(12.dp))
                    PlayerButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = stringResource(R.string.player_forward),
                        onClick = { onSeekBy(1, 0) },
                        size = btn
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showStreams) {
                        PlayerButton(
                            icon = Icons.Default.VideoLibrary,
                            contentDescription = stringResource(R.string.player_streams_button),
                            onClick = onStreams,
                            size = btn
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    if (qualityLabel != null) {
                        PlayerButton(
                            icon = null,
                            contentDescription = stringResource(R.string.player_quality),
                            text = qualityLabel,
                            onClick = onQuality,
                            size = btn
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    if (showSubtitles) {
                        PlayerButton(
                            icon = Icons.Default.ClosedCaption,
                            contentDescription = stringResource(R.string.player_subtitles),
                            onClick = onSubtitles,
                            size = btn
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    PlayerButton(
                        icon = null,
                        contentDescription = stringResource(R.string.player_speed),
                        text = speedLabel,
                        onClick = onSpeed,
                        size = btn
                    )
                    Spacer(Modifier.width(12.dp))
                    PlayerButton(
                        icon = Icons.Default.AspectRatio,
                        contentDescription = stringResource(R.string.player_aspect),
                        onClick = onAspect,
                        size = btn
                    )
                    if (showPip) {
                        Spacer(Modifier.width(12.dp))
                        PlayerButton(
                            icon = Icons.Default.PictureInPictureAlt,
                            contentDescription = stringResource(R.string.player_pip),
                            onClick = onPip,
                            size = btn
                        )
                    }
                }
            }
        }
    }
}
