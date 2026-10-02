package com.appsventory.digiapps.scour.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Shared FocusRequester for the first focusable in the current content screen.
 * When focus enters the content area from the sidebar (D-pad Right / after tab select),
 * it always lands on this target — not the spatially nearest item.
 */
val LocalContentEnterFocus = staticCompositionLocalOf<FocusRequester> {
    error("LocalContentEnterFocus not provided")
}

/** Attach to the first focusable widget of a screen (search bar, first row, etc.). */
@Composable
fun Modifier.contentEnterFocus(): Modifier {
    val requester = LocalContentEnterFocus.current
    return this.focusRequester(requester)
}
