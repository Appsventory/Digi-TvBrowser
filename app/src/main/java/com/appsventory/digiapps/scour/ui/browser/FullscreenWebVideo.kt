package com.appsventory.digiapps.scour.ui.browser

import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.player.PlayerConfig
import com.appsventory.digiapps.scour.player.WebVideoRemote
import com.appsventory.digiapps.scour.player.findActivity
import com.appsventory.digiapps.scour.ui.player.ImmersiveEffect
import com.appsventory.digiapps.scour.ui.player.formatSpeed
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UNSET = -1L

/**
 * Page-requested fullscreen (WebChromeClient.onShowCustomView): in-page and DRM players keep working.
 *
 * Remote mode (default): ←/→ seek (hold = bigger), OK play/pause, ↑/↓ speed.
 * Cursor mode (MENU toggles): D-pad moves a cursor, OK taps the page — for the site's own controls.
 */
@Composable
fun FullscreenWebVideo(
    view: View,
    webView: WebView?,
    callback: WebChromeClient.CustomViewCallback?,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val res = context.resources
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val rootFocus = remember { FocusRequester() }

    var cursorMode by remember { mutableStateOf(false) }
    var cursorX by remember { mutableFloatStateOf(0f) }
    var cursorY by remember { mutableFloatStateOf(0f) }
    var boxW by remember { mutableIntStateOf(0) }
    var boxH by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(res.getString(R.string.web_fullscreen_hint)) }
    var messageAt by remember { mutableStateOf(UNSET) }

    ImmersiveEffect(activity, lockLandscape = !isTv)

    BackHandler {
        callback?.onCustomViewHidden()
        onExit()
    }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { rootFocus.requestFocus() }
    }
    LaunchedEffect(message, messageAt) {
        if (message != null) {
            delay(PlayerConfig.WEB_HINT_MS.takeIf { messageAt == UNSET } ?: PlayerConfig.SEEK_FEEDBACK_MS)
            message = null
        }
    }

    fun say(text: String) {
        messageAt = SystemClock.uptimeMillis()
        message = text
    }

    fun show(token: String) {
        val kind = token.substringBefore(':')
        val arg = token.substringAfter(':', "")
        when (kind) {
            "seek" -> say(res.getString(R.string.player_seconds_fmt, arg))
            "speed" -> say(arg.toFloatOrNull()?.let { formatSpeed(it) } ?: arg)
            "play" -> say(res.getString(R.string.player_play))
            "pause" -> say(res.getString(R.string.player_pause))
        }
    }

    fun command(cmd: WebVideoRemote.Command, repeat: Int) {
        val step = if (repeat >= PlayerConfig.SEEK_LONG_AFTER_REPEATS)
            PlayerConfig.SEEK_STEP_LONG_MS else PlayerConfig.SEEK_STEP_MS
        WebVideoRemote.run(webView, cmd, (step / 1000).toInt()) { show(it) }
    }

    fun tap() {
        val t = SystemClock.uptimeMillis()
        fun ev(action: Int, time: Long) = MotionEvent.obtain(t, time, action, cursorX, cursorY, 0).also {
            it.source = InputDevice.SOURCE_TOUCHSCREEN
        }
        val down = ev(MotionEvent.ACTION_DOWN, t)
        view.dispatchTouchEvent(down)
        down.recycle()
        scope.launch {
            delay(PlayerConfig.WEB_CLICK_PRESS_MS)
            val up = ev(MotionEvent.ACTION_UP, SystemClock.uptimeMillis())
            view.dispatchTouchEvent(up)
            up.recycle()
        }
    }

    fun moveCursor(dx: Int, dy: Int, repeat: Int) {
        val step = with(density) { PlayerConfig.WEB_CURSOR_STEP_DP.dp.toPx() } *
            (1 + repeat.coerceAtMost(PlayerConfig.WEB_CURSOR_ACCEL_REPEATS) / 3f)
        cursorX = (cursorX + dx * step).coerceIn(0f, boxW.toFloat())
        cursorY = (cursorY + dy * step).coerceIn(0f, boxH.toFloat())
    }

    fun onKey(ev: KeyEvent): Boolean {
        if (ev.type != KeyEventType.KeyDown) return false
        val repeat = ev.nativeKeyEvent.repeatCount
        // Always available, whatever the mode
        when (ev.key) {
            Key.Menu, Key.Info, Key.Guide -> {
                cursorMode = !cursorMode
                say(res.getString(if (cursorMode) R.string.web_fullscreen_cursor_on else R.string.web_fullscreen_cursor_off))
                return true
            }
            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> { command(WebVideoRemote.Command.TogglePlay, 0); return true }
            Key.MediaFastForward -> { command(WebVideoRemote.Command.SeekForward, repeat); return true }
            Key.MediaRewind -> { command(WebVideoRemote.Command.SeekBack, repeat); return true }
        }
        return if (cursorMode) {
            when (ev.key) {
                Key.DirectionLeft -> { moveCursor(-1, 0, repeat); true }
                Key.DirectionRight -> { moveCursor(1, 0, repeat); true }
                Key.DirectionUp -> { moveCursor(0, -1, repeat); true }
                Key.DirectionDown -> { moveCursor(0, 1, repeat); true }
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { tap(); true }
                else -> false
            }
        } else {
            when (ev.key) {
                Key.DirectionLeft -> { command(WebVideoRemote.Command.SeekBack, repeat); true }
                Key.DirectionRight -> { command(WebVideoRemote.Command.SeekForward, repeat); true }
                Key.DirectionUp -> { command(WebVideoRemote.Command.SpeedUp, 0); true }
                Key.DirectionDown -> { command(WebVideoRemote.Command.SpeedDown, 0); true }
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                    command(WebVideoRemote.Command.TogglePlay, 0); true
                }
                else -> false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiColors.PlayerBg)
            .onSizeChanged {
                if (boxW == 0) { cursorX = it.width / 2f; cursorY = it.height / 2f }
                boxW = it.width
                boxH = it.height
            }
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { onKey(it) }
    ) {
        AndroidView(
            factory = {
                (view.parent as? ViewGroup)?.removeView(view)
                // Keys stay with Compose (above); the page is driven by script / synthetic taps.
                view.isFocusable = false
                (view as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                view
            },
            modifier = Modifier.fillMaxSize()
        )

        if (cursorMode) {
            val radius = with(density) { PlayerConfig.WEB_CURSOR_RADIUS_DP.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(cursorX, cursorY)
                drawCircle(DigiColors.CursorOuter, radius, c)
                drawCircle(DigiColors.CursorMid, radius * 0.8f, c)
                drawCircle(DigiColors.CursorDot, radius * 0.4f, c)
            }
        }

        message?.let {
            Text(
                it,
                color = DigiColors.OnDark,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .background(DigiColors.PlayerSubtitleBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}
