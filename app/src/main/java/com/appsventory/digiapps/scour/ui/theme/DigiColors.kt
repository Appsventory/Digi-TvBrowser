package com.appsventory.digiapps.scour.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for Digi Browser colors.
 * Use these everywhere — Home, Browser chrome, Settings, etc.
 */
object DigiColors {
    // Brand
    val Accent = Color(0xFF22D3EE)
    val AccentDim = Color(0xFF22D3EE).copy(alpha = 0.25f)
    val AccentSoft = Color(0xFF22D3EE).copy(alpha = 0.12f)

    // Backgrounds
    val BgDeep = Color(0xFF0A0E17)
    val BgGradientTop = Color(0xFF0F172A)
    val BgGradientMid = Color(0xFF0A0E17)
    val BgGradientBottom = Color(0xFF020617)
    val BgRail = Color(0xFF0B1220)
    val BgPanel = Color(0xFF0B1220)
    val BgCard = Color(0xFF141B2D)
    val BgWeb = Color(0xFF080B10)
    val BgSelected = Color(0xFF1E293B)

    // Borders
    val Border = Color(0xFF1E293B)
    val BorderMuted = Color(0xFF334155)

    // Text
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFE2E8F0)
    val TextMuted = Color(0xFF94A3B8)
    val TextDisabled = Color(0xFF48484A)

    // Focus
    val FocusRing = Accent
    val FocusFill = Accent
    val FocusOnAccent = Color(0xFF0F172A)

    // Misc (previously hardcoded in screens)
    val OnDark = Color.White
    val BrandGradientEnd = Color(0xFF6366F1)
    val PreviewBg = Color(0xFF0F172A)
    val SwitchThumbOff = Color(0xFFCBD5E1)
    val SwitchTrackOff = Color(0xFF334155)
    val ShortcutPalette = listOf(
        0xFFFF0000, 0xFFE50914, 0xFF4285F4, 0xFF111111,
        0xFF1A1A1A, 0xFFFF4500, 0xFF24292E, 0xFF10B981,
        0xFF8B5CF6, 0xFFF59E0B
    )

    // Video player
    val PlayerBg = Color.Black
    val PlayerScrim = Color(0xCC000000)
    val PlayerSubtitleBg = Color(0xB3000000)
    val PlayerTrack = Color(0x55FFFFFF)
    val PlayerBuffered = Color(0x99FFFFFF)

    // Cursor
    val CursorOuter = Color.White
    val CursorMid = Color(0xFF0F172A)
    val CursorDot = Accent

    val ScreenGradient: Brush
        get() = Brush.verticalGradient(
            listOf(BgGradientTop, BgGradientMid, BgGradientBottom)
        )

    val GlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(Color(0x3322D3EE), Color.Transparent)
        )
}
