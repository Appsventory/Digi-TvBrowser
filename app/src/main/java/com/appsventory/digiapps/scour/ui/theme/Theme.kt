package com.appsventory.digiapps.scour.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** The app UI is always dark (see [DigiColors]); no template purple/pink palette. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DigiBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DigiColors.Accent,
            onPrimary = DigiColors.FocusOnAccent,
            secondary = DigiColors.TextMuted,
            background = DigiColors.BgDeep,
            surface = DigiColors.BgDeep,
            onBackground = DigiColors.TextPrimary,
            onSurface = DigiColors.TextPrimary
        ),
        typography = Typography,
        content = content
    )
}
