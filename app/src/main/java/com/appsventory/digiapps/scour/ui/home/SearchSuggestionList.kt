package com.appsventory.digiapps.scour.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.data.Suggestion
import com.appsventory.digiapps.scour.data.SuggestionKind
import com.appsventory.digiapps.scour.ui.theme.DigiColors

/** Dropdown under the search bar. D-pad: ↑ from the first row goes back to [upTarget]. */
@Composable
fun SearchSuggestionList(
    suggestions: List<Suggestion>,
    firstFocus: FocusRequester,
    upTarget: FocusRequester,
    downTarget: FocusRequester,
    onPick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(DigiColors.BgPanel, shape)
            .border(1.dp, DigiColors.Border, shape)
            .padding(6.dp)
    ) {
        suggestions.forEachIndexed { index, s ->
            SuggestionRow(
                suggestion = s,
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                    .focusProperties {
                        if (index == 0) up = upTarget
                        if (index == suggestions.lastIndex) down = downTarget
                    },
                onClick = { onPick(s) }
            )
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: Suggestion, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val tint = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) DigiColors.FocusFill else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (suggestion.kind) {
                SuggestionKind.Query -> Icons.Default.Search
                SuggestionKind.History -> Icons.Outlined.History
                SuggestionKind.Bookmark -> Icons.Outlined.BookmarkBorder
            },
            contentDescription = null,
            tint = if (focused) DigiColors.FocusOnAccent else DigiColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            suggestion.text, color = tint, fontSize = 16.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
