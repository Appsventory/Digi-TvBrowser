package com.appsventory.digiapps.scour.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.ui.theme.DigiColors

/** Row used by every context menu / dialog list in the app. */
@Composable
fun MenuItem(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(if (focused) DigiColors.AccentDim else Color.Transparent)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) DigiColors.FocusRing else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = if (focused) DigiColors.Accent else DigiColors.TextPrimary,
            fontSize = 15.sp
        )
    }
}

/** "Clear" button shown in the History and Downloads headers. */
@Composable
fun ClearButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(if (focused) DigiColors.AccentDim else DigiColors.BgSelected, shape)
            .border(
                1.5.dp,
                if (focused) DigiColors.FocusRing else DigiColors.BorderMuted,
                shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Delete, null,
            tint = if (focused) DigiColors.Accent else DigiColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(
            stringResource(R.string.action_clear),
            color = if (focused) DigiColors.Accent else DigiColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(DigiColors.BgCard, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(16.dp))
        Text(title, color = DigiColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            color = DigiColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
