package com.appsventory.digiapps.scour.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.data.SearchEngine
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

private val CardShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(12.dp)

@Composable
fun SettingsScreen(
    searchEngine: SearchEngine,
    onSearchEngineChange: (SearchEngine) -> Unit,
    desktopMode: Boolean,
    onDesktopModeChange: (Boolean) -> Unit,
    adBlockEnabled: Boolean = true,
    onAdBlockChange: (Boolean) -> Unit = {},
    jsEnabled: Boolean = true,
    onJsChange: (Boolean) -> Unit = {},
    blockPopups: Boolean = true,
    onBlockPopupsChange: (Boolean) -> Unit = {},
    saveHistory: Boolean = true,
    onSaveHistoryChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var engineMenuOpen by remember { mutableStateOf(false) }
    var engineCardFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.nav_settings),
            color = DigiColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.settings_subtitle),
            color = DigiColors.TextMuted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
        )

        SectionTitle(stringResource(R.string.settings_search_engine))

        // One card, one border, clip — focus never draws outside
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(DigiColors.BgCard)
                .border(
                    width = if (engineCardFocused || engineMenuOpen) 2.dp else 1.dp,
                    color = if (engineCardFocused || engineMenuOpen) DigiColors.FocusRing
                    else DigiColors.Border,
                    shape = CardShape
                )
                .onFocusChanged { /* aggregate via children */ }
        ) {
            SettingsFocusRow(
                contentEnter = true,
                onFocused = { engineCardFocused = it },
                onClick = { engineMenuOpen = !engineMenuOpen }
            ) {
                Icon(
                    Icons.Outlined.Search, null,
                    tint = DigiColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    searchEngine.label,
                    color = DigiColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                )
                Icon(
                    Icons.Outlined.ExpandMore, null,
                    tint = DigiColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (engineMenuOpen) {
                SearchEngine.entries.forEach { engine ->
                    SettingsFocusRow(
                        selected = engine == searchEngine,
                        onFocused = { if (it) engineCardFocused = true },
                        onClick = {
                            onSearchEngineChange(engine)
                            engineMenuOpen = false
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Search, null,
                            tint = if (engine == searchEngine) DigiColors.Accent
                            else DigiColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            engine.label,
                            color = DigiColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (engine == searchEngine) FontWeight.SemiBold
                            else FontWeight.Normal,
                            modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                        )
                        if (engine == searchEngine) {
                            Text("✓", color = DigiColors.Accent, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle(stringResource(R.string.settings_display))
        SettingsCard {
            SettingsToggle(
                icon = Icons.Outlined.Computer,
                title = stringResource(R.string.settings_desktop_title),
                subtitle = stringResource(R.string.settings_desktop_sub),
                checked = desktopMode,
                onCheckedChange = onDesktopModeChange
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_general))
        SettingsCard {
            SettingsToggle(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_js_title),
                subtitle = stringResource(R.string.settings_js_sub),
                checked = jsEnabled,
                onCheckedChange = onJsChange
            )
            SettingsToggle(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.settings_popups_title),
                subtitle = stringResource(R.string.settings_popups_sub),
                checked = blockPopups,
                onCheckedChange = onBlockPopupsChange
            )
            SettingsToggle(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.settings_ads_title),
                subtitle = stringResource(R.string.settings_ads_sub),
                checked = adBlockEnabled,
                onCheckedChange = onAdBlockChange
            )
            SettingsToggle(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.settings_history_title),
                subtitle = stringResource(R.string.settings_history_sub),
                checked = saveHistory,
                onCheckedChange = onSaveHistoryChange
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_about))
        SettingsCard {
            SettingsFocusRow(onClick = {}) {
                Icon(
                    Icons.Outlined.Info, null,
                    tint = DigiColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        color = DigiColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.settings_version, appVersionName()),
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = DigiColors.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DigiColors.BgCard)
            .border(1.dp, DigiColors.Border, CardShape)
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

/**
 * Focus row: highlight stays INSIDE parent (background only, no outer border).
 * Prevents focus ring from drawing outside rounded containers.
 */
@Composable
private fun SettingsFocusRow(
    selected: Boolean = false,
    contentEnter: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RowShape)
            .then(if (contentEnter) Modifier.contentEnterFocus() else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                onFocused?.invoke(it.isFocused)
            }
            .clickable(onClick = onClick)
            .background(
                when {
                    focused -> DigiColors.AccentDim
                    selected -> DigiColors.BgSelected
                    else -> Color.Transparent
                },
                RowShape
            )
            // Inner focus ring — clipped by RowShape, inset by padding
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) DigiColors.FocusRing else Color.Transparent,
                shape = RowShape
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsFocusRow(onClick = { onCheckedChange(!checked) }) {
        Icon(
            icon, null,
            tint = DigiColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                title,
                color = DigiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(subtitle, color = DigiColors.TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DigiColors.OnDark,
                checkedTrackColor = DigiColors.Accent,
                uncheckedThumbColor = DigiColors.SwitchThumbOff,
                uncheckedTrackColor = DigiColors.SwitchTrackOff
            )
        )
    }
}

/** Real versionName from the manifest (no hardcoded "1.0"). */
@Composable
private fun appVersionName(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
}
