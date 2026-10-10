package com.appsventory.digiapps.scour.ui.player

import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.ui.PlayerView
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.player.DetectedSubtitle
import com.appsventory.digiapps.scour.player.PlayerConfig
import com.appsventory.digiapps.scour.player.PlayerFactory
import com.appsventory.digiapps.scour.player.PlayerPip
import com.appsventory.digiapps.scour.player.StreamPreview
import com.appsventory.digiapps.scour.player.StreamPreviewLoader
import com.appsventory.digiapps.scour.player.SubtitleCue
import com.appsventory.digiapps.scour.player.SubtitleLoader
import com.appsventory.digiapps.scour.player.SubtitleParser
import com.appsventory.digiapps.scour.player.findActivity
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

private data class Feedback(val id: Long, val text: String)

/**
 * Full-screen native player (Media3), drawn in the same window as the browser.
 *
 * Remote:   ←/→ seek (hold = bigger steps) · OK play/pause · ↑/↓ show controls & move between them
 *           Back closes menu, then the player.  Media keys (play/pause/FF/RW) always work.
 * Touch:    tap = show/hide controls · double-tap left/right = ∓10 s · drag the seek bar.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    initialStream: DetectedStream,
    subtitles: List<DetectedSubtitle>,
    streams: List<DetectedStream>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val res = context.resources
    val activity = remember(context) { context.findActivity() }
    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val pipSupported = remember { PlayerPip.isSupported(context) }

    // ——— Player (recreated when the user picks another stream) ———
    var stream by remember(initialStream) { mutableStateOf(initialStream) }
    val resumeKey = remember(stream.url) { PlayerFactory.resumeKey(stream.url) }
    val resumeMs = remember(stream.url) { Prefs.getResumePosition(context, resumeKey) }
    val exo = remember(stream.url) { PlayerFactory.create(context, stream, resumeMs) }

    fun persistResume() {
        if (exo.playbackState == Player.STATE_IDLE || exo.isCurrentMediaItemLive) return
        val dur = exo.duration
        if (dur == C.TIME_UNSET || dur <= 0L) return
        val pos = exo.currentPosition
        if (pos < PlayerConfig.RESUME_MIN_POSITION_MS || pos > dur - PlayerConfig.RESUME_END_MARGIN_MS) {
            Prefs.clearResumePosition(context, resumeKey)
        } else {
            Prefs.setResumePosition(context, resumeKey, pos)
        }
    }

    // Declared before rememberPlayerState so it is disposed after the state's listener is removed.
    DisposableEffect(exo) {
        onDispose {
            persistResume()
            exo.release()
        }
    }
    val state = rememberPlayerState(exo)

    // ——— UI state ———
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableLongStateOf(0L) }
    var menu by remember { mutableStateOf<PlayerMenuType?>(null) }
    var aspectMode by remember { mutableStateOf(AspectMode.Fit) }
    var message by remember { mutableStateOf<String?>(null) }
    var seekFeedback by remember { mutableStateOf<Feedback?>(null) }
    var inPip by remember { mutableStateOf(PlayerPip.isInPip(activity)) }

    // Side-loaded subtitles (own renderer so timing can be nudged)
    var activeSideloaded by remember { mutableStateOf<DetectedSubtitle?>(null) }
    var sideloadedCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var subtitleText by remember { mutableStateOf<String?>(null) }

    // Stream list: thumbnails / size / duration are fetched lazily when the list is opened.
    val previews = remember { mutableStateMapOf<String, StreamPreview>() }

    val rootFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    // ——— Track lists ———
    val qualityOptions = remember(state.tracks, state.params) {
        PlayerTracks.qualityOptions(state.tracks, state.params, res)
    }
    val embeddedSubs = remember(state.tracks) { PlayerTracks.embeddedSubtitles(state.tracks, res) }
    val sideloadedLabels = remember(subtitles) {
        subtitles.mapIndexed { i, s ->
            PlayerTracks.languageName(s.language)
                ?: res.getString(R.string.player_subtitle_n, embeddedSubs.size + i + 1)
        }
    }
    val hasSubtitles = embeddedSubs.isNotEmpty() || subtitles.isNotEmpty()

    // ——— Actions ———
    fun showControls() {
        controlsVisible = true
        interactionTick = SystemClock.uptimeMillis()
    }

    fun togglePlay() {
        if (exo.isPlaying) {
            exo.pause()
        } else {
            if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0L)
            exo.play()
        }
        showControls()
    }

    fun seekBy(direction: Int, repeatCount: Int) {
        if (!exo.isCurrentMediaItemSeekable) return
        val step = if (repeatCount >= PlayerConfig.SEEK_LONG_AFTER_REPEATS)
            PlayerConfig.SEEK_STEP_LONG_MS else PlayerConfig.SEEK_STEP_MS
        val delta = direction * step
        val dur = exo.duration
        val raw = exo.currentPosition + delta
        exo.seekTo(if (dur == C.TIME_UNSET) raw.coerceAtLeast(0L) else raw.coerceIn(0L, dur))
        seekFeedback = Feedback(
            SystemClock.uptimeMillis(),
            res.getString(R.string.player_seconds_fmt, "%+d".format(delta / 1000))
        )
        interactionTick = SystemClock.uptimeMillis()
    }

    fun applyQuality(option: QualityOption) {
        val b = exo.trackSelectionParameters.buildUpon()
        if (option.override == null) b.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        else b.setOverrideForType(option.override)
        exo.trackSelectionParameters = b.build()
    }

    fun subtitlesOff() {
        activeSideloaded = null
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    fun selectEmbedded(option: EmbeddedSubtitleOption) {
        activeSideloaded = null
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(option.override)
            .build()
    }

    fun selectSideloaded(sub: DetectedSubtitle) {
        // Our renderer draws these; silence ExoPlayer's own text so nothing shows twice.
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        activeSideloaded = sub
    }

    fun enterPip() {
        activity?.let { PlayerPip.enter(it, state.videoAspect) }
    }

    fun closeMenu() {
        menu = null
        showControls()
    }

    // ——— Window / system integration ———
    ImmersiveEffect(activity, lockLandscape = !isTv)

    DisposableEffect(activity) {
        val owner = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        owner?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { owner?.removeOnPictureInPictureModeChangedListener(listener) }
    }

    DisposableEffect(pipSupported) {
        if (pipSupported) PlayerPip.onUserLeaveHint = { if (exo.isPlaying) enterPip() }
        onDispose { PlayerPip.onUserLeaveHint = null }
    }

    DisposableEffect(activity) {
        val owner = activity as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                persistResume()
                if (!inPip) exo.pause()
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    // ——— Effects ———
    LaunchedEffect(exo) {
        if (resumeMs >= PlayerConfig.RESUME_MIN_POSITION_MS) {
            message = res.getString(R.string.player_resumed, formatTime(resumeMs))
        }
        while (true) {
            delay(PlayerConfig.RESUME_SAVE_INTERVAL_MS)
            persistResume()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(PlayerConfig.MESSAGE_MS)
            message = null
        }
    }

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(PlayerConfig.SEEK_FEEDBACK_MS)
            seekFeedback = null
        }
    }

    LaunchedEffect(inPip) {
        if (inPip) {
            menu = null
            controlsVisible = false
        } else {
            showControls()
        }
    }

    // Auto-hide while playing; any key / tap (interactionTick) restarts the timer.
    LaunchedEffect(controlsVisible, interactionTick, state.isPlaying, menu) {
        if (controlsVisible && state.isPlaying && menu == null) {
            delay(PlayerConfig.CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }

    // Keep D-pad focus somewhere that can receive keys.
    LaunchedEffect(controlsVisible, menu, state.error != null) {
        withFrameNanos { }
        runCatching {
            when {
                menu != null -> Unit // the menu panel claims focus itself
                state.error != null -> retryFocus.requestFocus()
                controlsVisible -> seekFocus.requestFocus()
                else -> rootFocus.requestFocus()
            }
        }
    }

    // Side-loaded subtitle: fetch once, then follow playback position (+ user nudge).
    LaunchedEffect(activeSideloaded) {
        sideloadedCues = emptyList()
        subtitleText = null
        val sub = activeSideloaded ?: return@LaunchedEffect
        val cues = try {
            SubtitleLoader.load(sub, stream)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        if (cues.isEmpty()) {
            message = res.getString(R.string.player_subtitle_failed)
            activeSideloaded = null
        } else {
            sideloadedCues = cues
        }
    }
    LaunchedEffect(sideloadedCues, subtitleOffsetMs) {
        if (sideloadedCues.isEmpty()) return@LaunchedEffect
        while (true) {
            subtitleText = SubtitleParser.activeText(sideloadedCues, exo.currentPosition - subtitleOffsetMs)
            delay(PlayerConfig.SUBTITLE_POLL_MS)
        }
    }

    LaunchedEffect(menu) {
        if (menu != PlayerMenuType.Streams) return@LaunchedEffect
        streams.filter { it.url !in previews }.forEach { s ->
            launch { previews[s.url] = StreamPreviewLoader.load(context.cacheDir, s) }
        }
    }

    fun switchStream(next: DetectedStream) {
        menu = null
        if (next.url == stream.url) { showControls(); return }
        activeSideloaded = null
        subtitleOffsetMs = 0L
        stream = next
        showControls()
    }

    BackHandler {
        if (menu != null) closeMenu() else onClose()
    }

    // ——— Remote / keyboard ———
    fun onKey(event: KeyEvent): Boolean {
        interactionTick = SystemClock.uptimeMillis()
        if (event.type != KeyEventType.KeyDown || inPip) return false
        val repeat = event.nativeKeyEvent.repeatCount

        // Transport keys work everywhere (even with a menu open).
        when (event.key) {
            Key.MediaPlayPause -> { togglePlay(); return true }
            Key.MediaPlay -> { if (!exo.isPlaying) togglePlay(); return true }
            Key.MediaPause -> { if (exo.isPlaying) togglePlay(); return true }
            Key.MediaFastForward -> { seekBy(1, repeat); return true }
            Key.MediaRewind -> { seekBy(-1, repeat); return true }
            Key.MediaStop -> { onClose(); return true }
        }
        if (menu != null || state.error != null) return false

        return if (!controlsVisible) {
            when (event.key) {
                Key.DirectionLeft -> { seekBy(-1, repeat); true }
                Key.DirectionRight -> { seekBy(1, repeat); true }
                Key.DirectionUp, Key.DirectionDown, Key.Menu, Key.Info, Key.Guide -> { showControls(); true }
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> { togglePlay(); true }
                else -> false
            }
        } else false // controls visible: the focused control handles the key
    }

    // ——— Layout ———
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiColors.PlayerBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        when {
                            menu != null -> closeMenu()
                            controlsVisible -> controlsVisible = false
                            else -> showControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        seekBy(if (offset.x < size.width / 2) -1 else 1, 0)
                    }
                )
            }
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { onKey(it) }
    ) {
        // Video
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .then(
                    aspectMode.ratio?.let { Modifier.aspectRatio(it) } ?: Modifier.fillMaxSize()
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        isFocusable = false
                        player = exo
                    }
                },
                update = {
                    it.resizeMode = aspectMode.resizeMode
                    if (it.player !== exo) it.player = exo
                },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Side-loaded subtitle
        if (!inPip) {
            subtitleText?.let { text ->
                Text(
                    text = text,
                    color = DigiColors.OnDark,
                    fontSize = if (isTv) 26.sp else 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (controlsVisible) 168.dp else 48.dp, start = 64.dp, end = 64.dp)
                        .background(DigiColors.PlayerSubtitleBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Buffering
        if (state.error == null && state.playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DigiColors.Accent
            )
        }

        // Seek feedback + info message
        if (!inPip) {
            seekFeedback?.let { PlayerPill(it.text, Modifier.align(Alignment.Center)) }
            message?.let { PlayerPill(it, Modifier.align(Alignment.TopCenter).padding(top = 32.dp)) }
        }

        // Controls
        AnimatedVisibility(
            visible = controlsVisible && !inPip && menu == null && state.error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                title = stream.title,
                subtitleLine = streamSummary(res, stream),
                isTv = isTv,
                isPlaying = state.isPlaying,
                positionMs = state.positionMs,
                bufferedMs = state.bufferedMs,
                durationMs = state.durationMs,
                isLive = state.isLive,
                seekable = state.isSeekable,
                qualityLabel = qualityOptions.takeIf { it.isNotEmpty() }
                    ?.let { opts -> (opts.firstOrNull { it.selected } ?: opts.first()).label },
                showStreams = streams.isNotEmpty(),
                showSubtitles = hasSubtitles,
                speedLabel = formatSpeed(state.speed),
                showPip = pipSupported,
                seekFocus = seekFocus,
                onClose = onClose,
                onSeekBy = ::seekBy,
                onSeekTo = { exo.seekTo(it); interactionTick = SystemClock.uptimeMillis() },
                onTogglePlay = ::togglePlay,
                onStreams = { menu = PlayerMenuType.Streams },
                onQuality = { menu = PlayerMenuType.Quality },
                onSubtitles = { menu = PlayerMenuType.Subtitles },
                onSpeed = { menu = PlayerMenuType.Speed },
                onAspect = { menu = PlayerMenuType.Aspect },
                onPip = { enterPip() }
            )
        }

        // Menus
        val panelWidth = if (isTv) 360.dp else 300.dp
        val panelModifier = Modifier.align(Alignment.CenterEnd)
        when (menu) {
            PlayerMenuType.Streams -> StreamListPanel(
                streams = streams,
                currentUrl = stream.url,
                previews = previews,
                width = if (isTv) 760.dp else 440.dp,
                onSelect = ::switchStream,
                modifier = panelModifier
            )

            PlayerMenuType.Quality -> PlayerMenuPanel(
                title = stringResource(R.string.player_quality),
                entries = qualityOptions.map { o ->
                    MenuEntry(o.label, o.selected) { applyQuality(o); closeMenu() }
                },
                width = panelWidth,
                modifier = panelModifier
            )

            PlayerMenuType.Subtitles -> {
                val entries = buildList {
                    add(
                        MenuEntry(
                            stringResource(R.string.player_subtitles_off),
                            selected = activeSideloaded == null && embeddedSubs.none { it.selected }
                        ) { subtitlesOff(); closeMenu() }
                    )
                    embeddedSubs.forEach { o ->
                        add(MenuEntry(o.label, o.selected && activeSideloaded == null) {
                            selectEmbedded(o); closeMenu()
                        })
                    }
                    subtitles.forEachIndexed { i, s ->
                        add(MenuEntry(sideloadedLabels[i], activeSideloaded?.url == s.url) {
                            selectSideloaded(s); closeMenu()
                        })
                    }
                }
                PlayerMenuPanel(
                    title = stringResource(R.string.player_subtitles),
                    entries = entries,
                    width = panelWidth,
                    modifier = panelModifier,
                    footer = {
                        SubtitleTimingRow(
                            offsetMs = subtitleOffsetMs,
                            enabled = activeSideloaded != null,
                            onChange = { subtitleOffsetMs = it },
                            onReset = { subtitleOffsetMs = 0L }
                        )
                    }
                )
            }

            PlayerMenuType.Speed -> PlayerMenuPanel(
                title = stringResource(R.string.player_speed),
                entries = PlayerConfig.SPEEDS.map { s ->
                    MenuEntry(formatSpeed(s), state.speed == s) {
                        exo.setPlaybackSpeed(s); closeMenu()
                    }
                },
                width = panelWidth,
                modifier = panelModifier
            )

            PlayerMenuType.Aspect -> PlayerMenuPanel(
                title = stringResource(R.string.player_aspect),
                entries = AspectMode.entries.map { m ->
                    MenuEntry(stringResource(m.labelRes), aspectMode == m) {
                        aspectMode = m; closeMenu()
                    }
                },
                width = panelWidth,
                modifier = panelModifier
            )

            null -> Unit
        }

        // Playback error
        state.error?.let { err ->
            PlayerError(
                message = errorMessage(err),
                retryFocus = retryFocus,
                onRetry = {
                    state.clearError()
                    exo.prepare()
                    exo.play()
                },
                onClose = onClose,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun errorMessage(err: PlaybackException): String {
    val http = err.cause as? HttpDataSource.InvalidResponseCodeException
    return if (http != null && (http.responseCode == HTTP_FORBIDDEN || http.responseCode == HTTP_UNAUTHORIZED)) {
        stringResource(R.string.player_error_denied, http.responseCode)
    } else {
        stringResource(R.string.player_error_generic, err.errorCodeName)
    }
}

@Composable
private fun PlayerPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = DigiColors.OnDark,
        fontSize = 18.sp,
        modifier = modifier
            .background(DigiColors.PlayerSubtitleBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun PlayerError(
    message: String,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DigiColors.BgPanel, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = DigiColors.OnDark, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerButton(
                icon = null,
                contentDescription = stringResource(R.string.player_retry),
                text = stringResource(R.string.player_retry),
                onClick = onRetry,
                focusRequester = retryFocus
            )
            PlayerButton(
                icon = null,
                contentDescription = stringResource(R.string.panel_close),
                text = stringResource(R.string.panel_close),
                onClick = onClose
            )
        }
    }
}
