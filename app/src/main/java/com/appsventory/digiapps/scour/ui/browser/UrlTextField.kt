package com.appsventory.digiapps.scour.ui.browser

import android.os.SystemClock
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Address-bar editor for one editing session (only composed while urlEditing is true).
 *
 * Root cause of lost select-all: the IME InputConnection collapses selection on
 * connect/show without changing text. We treat that as an IME artifact and restore
 * full selection until the user types or intentionally moves the caret after a
 * short protection window.
 */
@Composable
fun UrlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    fontSize: TextUnit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val keyboard = LocalSoftwareKeyboardController.current

    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(0, value.length)
            )
        )
    }
    /** After this uptime, IME-driven selection collapses are treated as user intent. */
    var protectSelectAllUntil by remember { mutableLongStateOf(Long.MAX_VALUE) }
    var userEditedText by remember { mutableStateOf(false) }

    fun fullSelect(text: String) = TextFieldValue(text = text, selection = TextRange(0, text.length))

    LaunchedEffect(value) {
        if (value != fieldValue.text && !userEditedText) {
            fieldValue = fullSelect(value)
        }
    }

    LaunchedEffect(Unit) {
        for (i in 0 until BrowserConfig.URL_FOCUS_RETRIES) {
            withFrameNanos { }
            val ok = runCatching { focusRequester.requestFocus() }
            if (ok.isSuccess && ok.getOrNull() != false) break
        }
        fieldValue = fullSelect(fieldValue.text)
        delay(BrowserConfig.KEYBOARD_SHOW_DELAY_MS.milliseconds)
        keyboard?.show()
        // Keep restoring select-all against IME collapse for a short window.
        protectSelectAllUntil = SystemClock.uptimeMillis() + 600L
        fieldValue = fullSelect(fieldValue.text)
        delay(100.milliseconds)
        if (!userEditedText) fieldValue = fullSelect(fieldValue.text)
        delay(200.milliseconds)
        if (!userEditedText) fieldValue = fullSelect(fieldValue.text)
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { next ->
            val now = SystemClock.uptimeMillis()
            val textSame = next.text == fieldValue.text
            val collapsed = next.selection.collapsed
            val wasFull =
                fieldValue.selection.start == 0 &&
                    fieldValue.selection.end == fieldValue.text.length &&
                    fieldValue.text.isNotEmpty()

            // IME often reports selection=cursor at end with no text change on show.
            if (
                !userEditedText &&
                textSame &&
                collapsed &&
                wasFull &&
                now < protectSelectAllUntil
            ) {
                fieldValue = fullSelect(next.text)
                return@BasicTextField
            }

            if (!textSame) {
                userEditedText = true
                protectSelectAllUntil = 0L
            } else if (!collapsed && !(next.selection.start == 0 && next.selection.end == next.text.length)) {
                // Partial selection by user
                userEditedText = true
                protectSelectAllUntil = 0L
            } else if (collapsed && wasFull && now >= protectSelectAllUntil) {
                // User moved caret after protection window
                userEditedText = true
            }

            fieldValue = next
            if (!textSame) onValueChange(next.text)
        },
        singleLine = true,
        textStyle = TextStyle(color = DigiColors.TextPrimary, fontSize = fontSize),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Uri),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        cursorBrush = SolidColor(DigiColors.Accent),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { fs ->
                onFocusChange(fs.isFocused)
                if (fs.isFocused) keyboard?.show()
            }
    )
}
