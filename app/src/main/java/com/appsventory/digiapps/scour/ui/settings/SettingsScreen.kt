package com.appsventory.digiapps.scour.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.data.PrivateDnsProvider
import com.appsventory.digiapps.scour.data.SearchEngine
import com.appsventory.digiapps.scour.data.UpdateStatus
import com.appsventory.digiapps.scour.data.UserAgentPreset
import com.appsventory.digiapps.scour.data.appVersionName
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors

private val SectionGap = 28.dp
private val CardRadius = 18.dp
private val TileRadius = 14.dp
private val FocusStroke = 2.5.dp

/** Which expandable settings section is open (accordion — only one at a time). */
private enum class SettingsSection { Engine, Dns, UserAgent }

@Composable
fun SettingsScreen(
    searchEngine: SearchEngine,
    onSearchEngineChange: (SearchEngine) -> Unit,
    userAgentPreset: UserAgentPreset,
    onUserAgentPresetChange: (UserAgentPreset) -> Unit,
    cursorHoldSpeed: Float = BrowserConfig.CURSOR_HOLD_SPEED_DEFAULT,
    onCursorHoldSpeedChange: (Float) -> Unit = {},
    adBlockEnabled: Boolean = true,
    onAdBlockChange: (Boolean) -> Unit = {},
    strongAdBlock: Boolean = true,
    onStrongAdBlockChange: (Boolean) -> Unit = {},
    jsEnabled: Boolean = true,
    onJsChange: (Boolean) -> Unit = {},
    blockPopups: Boolean = true,
    onBlockPopupsChange: (Boolean) -> Unit = {},
    saveHistory: Boolean = true,
    onSaveHistoryChange: (Boolean) -> Unit = {},
    privateDns: PrivateDnsProvider = PrivateDnsProvider.Off,
    onPrivateDnsChange: (PrivateDnsProvider) -> Unit = {},
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onOpenUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Accordion: at most one expandable section open.
    var openSection by remember { mutableStateOf<SettingsSection?>(null) }
    val context = LocalContext.current

    fun setSection(section: SettingsSection, open: Boolean) {
        openSection = if (open) section else if (openSection == section) null else openSection
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // —— Header ——
        Text(
            stringResource(R.string.nav_settings),
            color = DigiColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.settings_subtitle),
            color = DigiColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = SectionGap)
        )

        // —— Search engine ——
        SettingsSectionHeader(stringResource(R.string.settings_search_engine))
        ExpandableSelectCard(
            icon = Icons.Outlined.Search,
            title = searchEngine.label,
            subtitle = stringResource(R.string.settings_search_engine),
            expanded = openSection == SettingsSection.Engine,
            onExpandedChange = { setSection(SettingsSection.Engine, it) },
            contentEnter = true
        ) { choose ->
            SearchEngine.entries.forEach { engine ->
                SelectOptionRow(
                    label = engine.label,
                    selected = engine == searchEngine,
                    onClick = { choose { onSearchEngineChange(engine) } }
                )
            }
        }

        Spacer(Modifier.height(SectionGap))

        // —— Private DNS ——
        SettingsSectionHeader(stringResource(R.string.settings_private_dns))
        ExpandableSelectCard(
            icon = Icons.Outlined.Shield,
            title = privateDns.label,
            subtitle = privateDns.description,
            expanded = openSection == SettingsSection.Dns,
            onExpandedChange = { setSection(SettingsSection.Dns, it) }
        ) { choose ->
            PrivateDnsProvider.entries.forEach { provider ->
                SelectOptionRow(
                    label = provider.label,
                    subtitle = provider.description,
                    selected = provider == privateDns,
                    onClick = { choose { onPrivateDnsChange(provider) } }
                )
            }
            LinkRow(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.settings_open_system_dns),
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(SectionGap))

        // —— Browser User-Agent ——
        SettingsSectionHeader(stringResource(R.string.settings_ua_title))
        Text(
            stringResource(R.string.settings_ua_subtitle),
            color = DigiColors.TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        ExpandableSelectCard(
            icon = Icons.Outlined.Computer,
            title = userAgentPreset.label,
            subtitle = if (userAgentPreset == UserAgentPreset.Default)
                stringResource(R.string.settings_ua_default_sub)
            else
                userAgentPreset.brand,
            expanded = openSection == SettingsSection.UserAgent,
            onExpandedChange = { setSection(SettingsSection.UserAgent, it) }
        ) { choose ->
            SelectOptionRow(
                label = UserAgentPreset.Default.label,
                subtitle = stringResource(R.string.settings_ua_default_sub),
                selected = userAgentPreset == UserAgentPreset.Default,
                onClick = { choose { onUserAgentPresetChange(UserAgentPreset.Default) } }
            )
            UserAgentPreset.brandGroups.forEach { (brand, presets) ->
                Text(
                    brand,
                    color = DigiColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )
                presets.forEach { preset ->
                    SelectOptionRow(
                        label = preset.label,
                        selected = userAgentPreset == preset,
                        onClick = { choose { onUserAgentPresetChange(preset) } }
                    )
                }
            }
        }

        Spacer(Modifier.height(SectionGap))

        // —— Cursor ——
        SettingsSectionHeader(stringResource(R.string.settings_cursor))
        CursorSpeedCard(
            value = cursorHoldSpeed,
            onChange = onCursorHoldSpeedChange
        )

        Spacer(Modifier.height(SectionGap))

        // —— General toggles ——
        SettingsSectionHeader(stringResource(R.string.settings_general))
        SettingsGroup {
            ToggleTile(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_js_title),
                subtitle = stringResource(R.string.settings_js_sub),
                checked = jsEnabled,
                onCheckedChange = onJsChange
            )
            TileDivider()
            ToggleTile(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.settings_popups_title),
                subtitle = stringResource(R.string.settings_popups_sub),
                checked = blockPopups,
                onCheckedChange = onBlockPopupsChange
            )
            TileDivider()
            ToggleTile(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.settings_ads_title),
                subtitle = stringResource(R.string.settings_ads_sub),
                checked = adBlockEnabled,
                onCheckedChange = onAdBlockChange
            )
            TileDivider()
            ToggleTile(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.settings_ads_strong_title),
                subtitle = stringResource(R.string.settings_ads_strong_sub),
                checked = strongAdBlock,
                onCheckedChange = onStrongAdBlockChange
            )
            TileDivider()
            ToggleTile(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.settings_history_title),
                subtitle = stringResource(R.string.settings_history_sub),
                checked = saveHistory,
                onCheckedChange = onSaveHistoryChange
            )
        }

        Spacer(Modifier.height(SectionGap))

        // —— About / updates ——
        SettingsSectionHeader(stringResource(R.string.settings_about))
        SettingsGroup {
            val version = appVersionName(context)
            val statusLabel = when (updateStatus) {
                is UpdateStatus.Checking -> stringResource(R.string.settings_update_checking)
                is UpdateStatus.UpToDate -> stringResource(R.string.settings_update_latest)
                is UpdateStatus.Available -> stringResource(
                    R.string.settings_update_available,
                    updateStatus.release.tag
                )
                is UpdateStatus.Failed -> stringResource(R.string.settings_update_failed)
                else -> stringResource(R.string.settings_version, version)
            }
            NavTile(
                icon = Icons.Outlined.NewReleases,
                title = stringResource(R.string.settings_about),
                subtitle = statusLabel,
                onClick = onOpenUpdate
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Design primitives — TV / D-pad first
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DigiColors.Accent)
        )
        Text(
            title.uppercase(),
            color = DigiColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    // Inset padding so every row can use the same rounded focus ring without
    // looking square against the card edge.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DigiColors.BgCard, RoundedCornerShape(CardRadius))
            .border(1.dp, DigiColors.Border, RoundedCornerShape(CardRadius))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        content()
    }
}

@Composable
private fun TileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(DigiColors.Border.copy(alpha = 0.6f))
    )
}

/**
 * Single focus target for settings rows.
 *
 * - [clickable] alone provides focus (no extra [focusable] — that caused double-OK).
 * - No scale animation (layout shift stole focus).
 * - No parent-dependent clip of the focus ring; border is drawn on the same shape as the fill.
 * - Optional [focusRequester] so callers can restore focus after list collapse.
 */
@Composable
private fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentEnter: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusedChange: ((Boolean) -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(TileRadius),
    content: @Composable (focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> DigiColors.FocusRing
            selected -> DigiColors.Accent.copy(alpha = 0.45f)
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "focusBorder"
    )
    val bg by animateColorAsState(
        targetValue = when {
            focused -> DigiColors.AccentDim
            selected -> DigiColors.BgSelected
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "focusBg"
    )

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (contentEnter) Modifier.contentEnterFocus() else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                onFocusedChange?.invoke(it.isFocused)
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .background(bg, shape)
            .border(FocusStroke, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content(focused)
    }
}

/**
 * Expandable picker card.
 *
 * [content] receives `choose { … }`: apply the selection, move focus back to the
 * header, then collapse. That prevents the disposed option from dropping focus
 * onto the rail Home icon.
 */
@Composable
private fun ExpandableSelectCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    contentEnter: Boolean = false,
    content: @Composable (choose: (action: () -> Unit) -> Unit) -> Unit
) {
    var headerFocused by remember { mutableStateOf(false) }
    val headerFocus = remember { FocusRequester() }
    val cardBorder by animateColorAsState(
        targetValue = if (headerFocused || expanded) DigiColors.FocusRing else DigiColors.Border,
        animationSpec = tween(150),
        label = "cardBorder"
    )

    fun choose(action: () -> Unit) {
        action()
        // Restore focus to the header BEFORE removing option nodes from composition.
        runCatching { headerFocus.requestFocus() }
        onExpandedChange(false)
    }

    // No clip on the card — clipping was cutting focus rings on the first/last rows.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DigiColors.BgCard, RoundedCornerShape(CardRadius))
            .border(
                width = if (headerFocused || expanded) FocusStroke else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(CardRadius)
            )
    ) {
        FocusSurface(
            onClick = { onExpandedChange(!expanded) },
            contentEnter = contentEnter,
            focusRequester = headerFocus,
            onFocusedChange = { headerFocused = it },
            shape = RoundedCornerShape(CardRadius),
            modifier = Modifier.fillMaxWidth()
        ) { focused ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DigiColors.AccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = DigiColors.Accent, modifier = Modifier.size(22.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        title,
                        color = DigiColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null,
                    tint = if (focused) DigiColors.Accent else DigiColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(DigiColors.Border)
            )
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                content(::choose)
            }
        }
    }
}

@Composable
private fun SelectOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    FocusSurface(
        onClick = onClick,
        selected = selected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = when {
                        focused || selected -> DigiColors.Accent
                        else -> DigiColors.TextPrimary
                    },
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    null,
                    tint = DigiColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = if (focused) DigiColors.Accent else DigiColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                label,
                color = if (focused) DigiColors.Accent else DigiColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun ToggleTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FocusSurface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(TileRadius)
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) DigiColors.AccentSoft else DigiColors.BgSelected),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = if (checked || focused) DigiColors.Accent else DigiColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    title,
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Custom pill indicator (more TV-friendly than Material Switch)
            StatusPill(checked = checked, focused = focused)
        }
    }
}

@Composable
private fun StatusPill(checked: Boolean, focused: Boolean) {
    val bg = when {
        checked && focused -> DigiColors.Accent
        checked -> DigiColors.Accent.copy(alpha = 0.85f)
        focused -> DigiColors.BorderMuted
        else -> DigiColors.BgSelected
    }
    val fg = when {
        checked -> DigiColors.BgDeep
        focused -> DigiColors.TextPrimary
        else -> DigiColors.TextMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                1.dp,
                if (focused && !checked) DigiColors.FocusRing else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            if (checked) "ON" else "OFF",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun NavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(TileRadius)
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DigiColors.AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = DigiColors.Accent, modifier = Modifier.size(20.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    title,
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    color = if (focused) DigiColors.Accent else DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Outlined.ExpandMore,
                null,
                tint = DigiColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CursorSpeedCard(
    value: Float,
    onChange: (Float) -> Unit
) {
    val display = value.toInt().coerceIn(
        BrowserConfig.CURSOR_HOLD_SPEED_MIN.toInt(),
        BrowserConfig.CURSOR_HOLD_SPEED_MAX.toInt()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DigiColors.BgCard, RoundedCornerShape(CardRadius))
            .border(1.dp, DigiColors.Border, RoundedCornerShape(CardRadius))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DigiColors.AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Speed, null,
                    tint = DigiColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.settings_cursor_hold_title),
                    color = DigiColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.settings_cursor_hold_sub),
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Large value + step controls — primary D-pad target area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            StepButton(
                icon = Icons.Outlined.Remove,
                enabled = display > BrowserConfig.CURSOR_HOLD_SPEED_MIN.toInt(),
                onClick = {
                    onChange(
                        (display - 1).toFloat()
                            .coerceAtLeast(BrowserConfig.CURSOR_HOLD_SPEED_MIN)
                    )
                }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp)
            ) {
                Text(
                    stringResource(R.string.settings_cursor_hold_value, display),
                    color = DigiColors.Accent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${BrowserConfig.CURSOR_HOLD_SPEED_MIN.toInt()}× – ${BrowserConfig.CURSOR_HOLD_SPEED_MAX.toInt()}×",
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp
                )
            }
            StepButton(
                icon = Icons.Outlined.Add,
                enabled = display < BrowserConfig.CURSOR_HOLD_SPEED_MAX.toInt(),
                onClick = {
                    onChange(
                        (display + 1).toFloat()
                            .coerceAtMost(BrowserConfig.CURSOR_HOLD_SPEED_MAX)
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Progress track visual (not interactive — steps are via buttons)
        val range = BrowserConfig.CURSOR_HOLD_SPEED_MAX - BrowserConfig.CURSOR_HOLD_SPEED_MIN
        val progress = ((display - BrowserConfig.CURSOR_HOLD_SPEED_MIN) / range)
            .coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DigiColors.BgSelected)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DigiColors.Accent)
            )
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = when {
            focused -> DigiColors.Accent
            enabled -> DigiColors.BgSelected
            else -> DigiColors.BgSelected.copy(alpha = 0.4f)
        },
        animationSpec = tween(100),
        label = "stepBg"
    )
    val iconTint = when {
        focused -> DigiColors.BgDeep
        enabled -> DigiColors.TextPrimary
        else -> DigiColors.TextDisabled
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .background(bg, CircleShape)
            .border(
                width = if (focused) FocusStroke else 1.dp,
                color = if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
    }
}
