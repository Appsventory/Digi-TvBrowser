package com.appsventory.digiapps.scour.ui.player

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.hostOf
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.player.StreamPreview
import com.appsventory.digiapps.scour.player.StreamType
import com.appsventory.digiapps.scour.ui.theme.DigiColors

private val THUMB_WIDTH = 120.dp
private val THUMB_HEIGHT = 68.dp
private const val URL_MAX_LINES = 3

/** File name from the URL path (decoded); host when the path has none. */
fun streamFileName(url: String): String =
    Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: hostOf(url)

/**
 * Every stream the page loaded, so a wrong guess can be fixed by picking another.
 * Row: thumbnail left · file name · full URL · size · duration · type.
 */
@Composable
fun StreamListPanel(
    streams: List<DetectedStream>,
    currentUrl: String,
    previews: Map<String, StreamPreview>,
    width: Dp,
    onSelect: (DetectedStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val requesters = remember(streams.size) { List(streams.size) { FocusRequester() } }
    val startIndex = streams.indexOfFirst { it.url == currentUrl }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        if (streams.isNotEmpty()) {
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
            stringResource(R.string.player_streams, streams.size),
            color = DigiColors.OnDark, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(streams) { index, s ->
                StreamRow(
                    stream = s,
                    current = s.url == currentUrl,
                    preview = previews[s.url],
                    focusRequester = requesters[index],
                    onClick = { onSelect(s) }
                )
            }
        }
    }
}

@Composable
private fun StreamRow(
    stream: DetectedStream,
    current: Boolean,
    preview: StreamPreview?,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val res = context.resources
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val primary = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    val muted = if (focused) DigiColors.FocusOnAccent else DigiColors.TextMuted
    val unknown = stringResource(R.string.player_unknown)

    val info = stream.info
    val durationMs = (info?.durationMs ?: 0L).takeIf { it > 0L } ?: preview?.durationMs ?: 0L
    val meta = buildList {
        add(preview?.sizeBytes?.takeIf { it > 0L }?.let { Formatter.formatShortFileSize(context, it) } ?: unknown)
        add(
            when {
                info?.live == true -> stringResource(R.string.player_live)
                durationMs > 0L -> formatTime(durationMs)
                else -> unknown
            }
        )
        info?.height?.takeIf { it > 0 }?.let { add(res.getString(R.string.player_quality_height, it)) }
        add(
            stringResource(
                when (stream.type) {
                    StreamType.HLS -> R.string.stream_type_hls
                    StreamType.DASH -> R.string.stream_type_dash
                    StreamType.PROGRESSIVE -> R.string.stream_type_file
                }
            )
        )
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .background(
                when {
                    focused -> DigiColors.FocusFill
                    current -> DigiColors.AccentSoft
                    else -> Color.Transparent
                },
                shape
            )
            .then(if (focused) Modifier.border(2.dp, DigiColors.FocusRing, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(DigiColors.BgCard),
            contentAlignment = Alignment.Center
        ) {
            val bmp = preview?.thumbnail
            if (bmp != null) {
                val image = remember(bmp) { bmp.asImageBitmap() }
                Image(
                    bitmap = image, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().fillMaxHeight()
                )
            } else {
                Icon(
                    Icons.Outlined.PlayCircle, contentDescription = null,
                    tint = DigiColors.TextMuted, modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                streamFileName(stream.url), color = primary, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                stream.url, color = muted, fontSize = 11.sp,
                maxLines = URL_MAX_LINES, overflow = TextOverflow.Ellipsis
            )
            Text(
                meta, color = if (focused) DigiColors.FocusOnAccent else DigiColors.Accent,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (current) {
            Icon(
                Icons.Default.Check, contentDescription = null,
                tint = if (focused) DigiColors.FocusOnAccent else DigiColors.Accent,
                modifier = Modifier.padding(start = 8.dp).size(20.dp)
            )
        }
    }
}
