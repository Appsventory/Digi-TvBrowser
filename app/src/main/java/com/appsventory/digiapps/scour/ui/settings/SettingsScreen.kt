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
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.appsventory.digiapps.scour.data.PrivateDnsProvider
import com.appsventory.digiapps.scour.data.UpdateStatus
import com.appsventory.digiapps.scour.data.appVersionName
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
    var engineMenuOpen by remember { mutableStateOf(false) }
    var engineCardFocused by remember { mutableStateOf(false) }
    var dnsMenuOpen by remember { mutableStateOf(false) }
    var dnsCardFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

        // Search engine card
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

        SectionTitle(stringResource(R.string.settings_private_dns))

        // Private DNS card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(DigiColors.BgCard)
                .border(
                    width = if (dnsCardFocused || dnsMenuOpen) 2.dp else 1.dp,
                    color = if (dnsCardFocused || dnsMenuOpen) DigiColors.FocusRing
                    else DigiColors.Border,
                    shape = CardShape
                )
        ) {
            SettingsFocusRow(
                onFocused = { dnsCardFocused = it },
                onClick = { dnsMenuOpen = !dnsMenuOpen }
            ) {
                Icon(
                    Icons.Outlined.Shield, null,
                    tint = DigiColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        privateDns.label,
                        color = DigiColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        privateDns.description,
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.Outlined.ExpandMore, null,
                    tint = DigiColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (dnsMenuOpen) {
                PrivateDnsProvider.entries.forEach { provider ->
                    SettingsFocusRow(
                        selected = provider == privateDns,
                        onFocused = { if (it) dnsCardFocused = true },
                        onClick = {
                            onPrivateDnsChange(provider)
                            dnsMenuOpen = false
                        }
                    ) {
                        Column(Modifier.weight(1f).padding(start = 36.dp, end = 14.dp)) {
                            Text(
                                provider.label,
                                color = DigiColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (provider == privateDns) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                provider.description,
                                color = DigiColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        if (provider == privateDns) {
                            Text("✓", color = DigiColors.Accent, fontSize = 16.sp)
                        }
                    }
                }
            }
            SettingsFocusRow(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.Info, null,
                    tint = DigiColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.settings_open_system_dns),
                    color = DigiColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 14.dp)
                )
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
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.settings_ads_strong_title),
                subtitle = stringResource(R.string.settings_ads_strong_sub),
                checked = strongAdBlock,
                onCheckedChange = onStrongAdBlockChange
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
            SettingsFocusRow(onClick = onOpenUpdate) {
                Icon(
                    Icons.Outlined.Info, null,
                    tint = DigiColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        stringResource(R.string.app_name),
                        color = DigiColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.settings_version, appVersionName(context)),
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                UpdateChip(updateStatus)
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

/** Right side of the version row: checking / up to date / update available / failed. */
@Composable
private fun UpdateChip(status: UpdateStatus) {
    val (text, color) = when (status) {
        UpdateStatus.Idle -> return
        UpdateStatus.Checking -> stringResource(R.string.settings_update_checking) to DigiColors.TextMuted
        UpdateStatus.Failed -> stringResource(R.string.settings_update_failed) to DigiColors.TextMuted
        is UpdateStatus.UpToDate -> stringResource(R.string.settings_update_latest) to DigiColors.TextMuted
        is UpdateStatus.Available ->
            stringResource(R.string.settings_update_available, status.release.tag) to DigiColors.Accent
    }
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        fontWeight = if (status is UpdateStatus.Available) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .then(
                if (status is UpdateStatus.Available) {
                    Modifier
                        .background(DigiColors.AccentDim, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                } else Modifier
            )
    )
}
