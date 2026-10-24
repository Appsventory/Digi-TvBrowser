package com.appsventory.digiapps.scour.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.ui.player.streamSummary
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay

private val ToastRadius = 20.dp
private val ToastPad = 16.dp
private const val FOCUS_DELAY_MS = 80L

/**
 * Shared bottom toast shell for browser overlays (download, video detected, …).
 * TV-first: one [clickable] per action (no extra focusable), clear focus ring, no clip of focus.
 */
@Composable
fun BrowserToastCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .background(DigiColors.BgCard, RoundedCornerShape(ToastRadius))
            .border(1.5.dp, DigiColors.Accent.copy(alpha = 0.55f), RoundedCornerShape(ToastRadius))
            .padding(ToastPad)
    ) {
        content()
    }
}

@Composable
fun ToastIconBadge(
    icon: ImageVector,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DigiColors.AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = DigiColors.Accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Primary / secondary / ghost action pill.
 * Focused → solid accent + dark label. Unfocused primary → accent outline.
 */
@Composable
fun ToastAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        focused -> DigiColors.Accent
        primary -> DigiColors.Accent.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val border = when {
        focused -> DigiColors.Accent
        primary -> DigiColors.Accent.copy(alpha = 0.7f)
        else -> DigiColors.BorderMuted
    }
    val fg = when {
        focused -> DigiColors.BgDeep
        primary -> DigiColors.Accent
        else -> DigiColors.TextSecondary
    }

    Text(
        text = label,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .background(bg, shape)
            .border(1.5.dp, border, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun ToastDismiss(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(36.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .background(
                if (focused) DigiColors.Accent else DigiColors.BgSelected,
                CircleShape
            )
            .border(
                1.5.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Close,
            contentDescription = stringResource(R.string.panel_close),
            tint = if (focused) DigiColors.BgDeep else DigiColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Download started — View opens Downloads, dismiss closes the chip. Auto-hides. */
@Composable
fun DownloadToast(
    fileName: String,
    onView: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryFocus = remember { FocusRequester() }
    // Same focus path as StreamPrompt: request after composition, no AnimatedVisibility delay.
    LaunchedEffect(fileName) {
        delay(FOCUS_DELAY_MS)
        runCatching { primaryFocus.requestFocus() }
        delay(BrowserConfig.DOWNLOAD_BANNER_MS)
        onDismiss()
    }

    BrowserToastCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToastIconBadge(Icons.Outlined.Download)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.toast_download_title),
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    fileName,
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            ToastAction(
                label = stringResource(R.string.action_view),
                onClick = onView,
                primary = true,
                focusRequester = primaryFocus
            )
            ToastDismiss(onClick = onDismiss)
        }
    }
}

/** Same-name conflict: Cancel or Download anyway (unique renamed file). */
@Composable
fun DownloadConflictToast(
    fileName: String,
    onCancel: () -> Unit,
    onDownloadAnyway: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryFocus = remember { FocusRequester() }
    LaunchedEffect(fileName) {
        delay(FOCUS_DELAY_MS)
        runCatching { primaryFocus.requestFocus() }
    }

    BrowserToastCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToastIconBadge(Icons.Outlined.Download)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.toast_download_exists_title),
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.toast_download_exists_body, fileName),
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToastAction(
                        label = stringResource(R.string.toast_download_cancel),
                        onClick = onCancel,
                        focusRequester = primaryFocus
                    )
                    ToastAction(
                        label = stringResource(R.string.toast_download_anyway),
                        onClick = onDownloadAnyway,
                        primary = true
                    )
                }
            }
            ToastDismiss(onClick = onCancel)
        }
    }
}

/**
 * Video detected card — redesigned on the same toast language as [DownloadToast].
 * Non-modal; page stays usable until the user answers.
 */
@Composable
fun StreamPrompt(
    streams: List<DetectedStream>,
    onPlayInApp: (DetectedStream) -> Unit,
    onPlayInBrowser: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primary = streams.firstOrNull() ?: return
    val others = streams.drop(1)
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    var showOthers by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(FOCUS_DELAY_MS)
        runCatching { firstFocus.requestFocus() }
    }

    BrowserToastCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToastIconBadge(Icons.Outlined.PlayCircle)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stream_prompt_title),
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    streamSummary(res, primary),
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToastAction(
                        label = stringResource(R.string.stream_prompt_play_app),
                        onClick = { onPlayInApp(primary) },
                        primary = true,
                        focusRequester = firstFocus
                    )
                    ToastAction(
                        label = stringResource(R.string.stream_prompt_play_browser),
                        onClick = onPlayInBrowser
                    )
                    if (others.isNotEmpty()) {
                        ToastAction(
                            label = stringResource(R.string.stream_prompt_more, others.size),
                            onClick = { showOthers = !showOthers }
                        )
                    }
                }
                if (showOthers) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        others.forEach { s ->
                            ToastAction(
                                label = streamSummary(res, s),
                                onClick = { onPlayInApp(s) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            if (onDismiss != null) {
                ToastDismiss(onClick = onDismiss)
            }
        }
    }
}
