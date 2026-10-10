package com.appsventory.digiapps.scour.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.ui.player.streamSummary
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay

private const val FOCUS_DELAY_MS = 80L

/**
 * "Video detected" card. Non-modal; the page stays usable until the user answers.
 * Each row shows duration · resolution · type · host so the user can tell the film from an ad.
 */
@Composable
fun StreamPrompt(
    /** Best guess first (see StreamRanking.rank). */
    streams: List<DetectedStream>,
    onPlayInApp: (DetectedStream) -> Unit,
    onPlayInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = streams.firstOrNull() ?: return
    val others = streams.drop(1)
    val res = LocalContext.current.resources
    var showOthers by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(FOCUS_DELAY_MS)
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth(0.92f)
            .background(DigiColors.BgPanel, RoundedCornerShape(12.dp))
            .border(2.dp, DigiColors.Accent, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PlayCircle, contentDescription = null,
                tint = DigiColors.Accent, modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stream_prompt_title),
                    color = DigiColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    streamSummary(res, primary),
                    color = DigiColors.TextMuted, fontSize = 12.sp, maxLines = 1
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PromptButton(
                stringResource(R.string.stream_prompt_play_app),
                onClick = { onPlayInApp(primary) },
                focusRequester = firstFocus
            )
            PromptButton(stringResource(R.string.stream_prompt_play_browser), onClick = onPlayInBrowser)
            if (others.isNotEmpty()) {
                PromptButton(
                    stringResource(R.string.stream_prompt_more, others.size),
                    onClick = { showOthers = !showOthers }
                )
            }
        }
        if (showOthers) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                others.forEach { s ->
                    PromptButton(streamSummary(res, s), onClick = { onPlayInApp(s) }, fill = true)
                }
            }
        }
    }
}

/** Same focus language as the download banner: accent fill + dark text when focused. */
@Composable
private fun PromptButton(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    fill: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = text,
        color = if (focused) DigiColors.BgDeep else DigiColors.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(if (focused) DigiColors.Accent else Color.Transparent, shape)
            .border(1.dp, DigiColors.Accent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
