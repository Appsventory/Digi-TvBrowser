package com.appsventory.digiapps.scour.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.ui.shell.ClearButton
import com.appsventory.digiapps.scour.ui.shell.EmptyState
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

data class HistoryItem(
    val title: String,
    val url: String,
    val timeLabel: String
)

/**
 * Uses Column + verticalScroll (not LazyColumn).
 * LazyColumn is a focus group that traps D-pad Up/Down and blocks
 * moving focus to the Clear button. With a plain Column, Clear and
 * rows are siblings in one focus scope — system navigation works.
 */
@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onOpen: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.nav_history),
                    color = DigiColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (items.isEmpty()) stringResource(R.string.history_empty_count) else stringResource(R.string.history_count, items.size),
                    color = DigiColors.TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (items.isNotEmpty()) {
                ClearButton(onClick = onClear)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (items.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.History, null,
                        tint = DigiColors.TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    HistoryRow(
                        item = item,
                        isFirst = index == 0,
                        onClick = { onOpen(item.url) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit, isFirst: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFirst) Modifier.contentEnterFocus() else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                if (focused) DigiColors.BgSelected else DigiColors.BgCard,
                shape
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DigiColors.BgDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.title.take(1).uppercase(),
                color = DigiColors.Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                item.title,
                color = DigiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.url,
                color = DigiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(item.timeLabel, color = DigiColors.TextMuted, fontSize = 12.sp)
    }
}
