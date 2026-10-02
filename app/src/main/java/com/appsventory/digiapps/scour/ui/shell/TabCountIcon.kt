package com.appsventory.digiapps.scour.ui.shell

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Chrome-style tabs icon: rounded square with the tab count inside. */
@Composable
fun TabCountIcon(count: Int, tint: Color, size: Dp = 22.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .border(2.dp, tint, RoundedCornerShape(6.dp))
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = tint,
            fontSize = if (count > 99) 8.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
