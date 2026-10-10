package com.appsventory.digiapps.scour.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay

private const val BUTTON_DP = 44
private const val MENU_WIDTH_DP = 200

/**
 * ⋮ button in the page header → "Check again" / "View on GitHub".
 * Own popup (not DropdownMenu) so the focus ring matches the rest of the app on a remote.
 */
@Composable
fun UpdateOverflowMenu(onCheck: () -> Unit, onViewGitHub: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val below = with(LocalDensity.current) { (BUTTON_DP.dp + 6.dp).roundToPx() }

    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(BUTTON_DP.dp)
                .background(if (focused) DigiColors.FocusFill else DigiColors.BgCard, shape)
                .border(
                    if (focused) 2.dp else 1.dp,
                    if (focused) DigiColors.FocusRing else DigiColors.Border,
                    shape
                )
                .onFocusChanged { focused = it.isFocused }
                .clickable { open = true }
        ) {
            Icon(
                Icons.Default.MoreVert, stringResource(R.string.update_more_options),
                tint = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (open) {
            val first = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(60)
                runCatching { first.requestFocus() }
            }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, below),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(MENU_WIDTH_DP.dp)
                        .background(DigiColors.BgPanel, shape)
                        .border(1.dp, DigiColors.Border, shape)
                        .padding(6.dp)
                ) {
                    MenuItem(stringResource(R.string.update_check_again), first) {
                        open = false
                        onCheck()
                    }
                    MenuItem(stringResource(R.string.update_view_github), null) {
                        open = false
                        onViewGitHub()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, focusRequester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Text(
        text,
        color = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(if (focused) DigiColors.FocusFill else androidx.compose.ui.graphics.Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
