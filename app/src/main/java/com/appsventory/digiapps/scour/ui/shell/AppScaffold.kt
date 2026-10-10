package com.appsventory.digiapps.scour.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

enum class AppTab { Home, Downloads, Bookmarks, History, Settings }

val Accent get() = DigiColors.Accent
val TextPrimary get() = DigiColors.TextPrimary
val TextMuted get() = DigiColors.TextMuted
val BgCard get() = DigiColors.BgCard

val CompactWidthBreakpoint = 600.dp

@Composable
fun AppScaffold(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    tabCount: Int = 0,
    onOpenTabs: () -> Unit = {},
    /** Sub-page title (replaces the tab title) and its right-aligned actions, e.g. a ⋮ menu. */
    headerTitle: String? = null,
    headerActions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val contentEnterFocus = remember { FocusRequester() }

    LaunchedEffect(selected) {
        delay(80)
        runCatching { contentEnterFocus.requestFocus() }
    }

    CompositionLocalProvider(LocalContentEnterFocus provides contentEnterFocus) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiColors.ScreenGradient)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val compact = maxWidth < CompactWidthBreakpoint
            if (compact) {
                CompactScaffold(
                    selected = selected,
                    onSelect = onSelect,
                    tabCount = tabCount,
                    onOpenTabs = onOpenTabs,
                    headerTitle = headerTitle,
                    headerActions = headerActions,
                    content = content
                )
            } else {
                ExpandedScaffold(
                    selected = selected,
                    onSelect = onSelect,
                    contentEnterFocus = contentEnterFocus,
                    headerTitle = headerTitle,
                    headerActions = headerActions,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun CompactScaffold(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    tabCount: Int,
    onOpenTabs: () -> Unit,
    headerTitle: String?,
    headerActions: (@Composable () -> Unit)?,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DigiColors.BgRail,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 20.dp, horizontal = 12.dp)
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        color = DigiColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        stringResource(R.string.drawer_menu),
                        color = DigiColors.TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                    )
                    DrawerItem(Icons.Default.Home, stringResource(R.string.nav_home), selected == AppTab.Home) {
                        onSelect(AppTab.Home)
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(
                        icon = null,
                        label = stringResource(R.string.nav_tabs),
                        selected = false,
                        tabCount = tabCount
                    ) {
                        scope.launch { drawerState.close() }
                        onOpenTabs()
                    }
                    DrawerItem(Icons.Outlined.Download, stringResource(R.string.nav_downloads), selected == AppTab.Downloads) {
                        onSelect(AppTab.Downloads)
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.BookmarkBorder, stringResource(R.string.nav_bookmarks), selected == AppTab.Bookmarks) {
                        onSelect(AppTab.Bookmarks)
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.History, stringResource(R.string.nav_history), selected == AppTab.History) {
                        onSelect(AppTab.History)
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    DrawerItem(Icons.Default.Settings, stringResource(R.string.nav_settings), selected == AppTab.Settings) {
                        onSelect(AppTab.Settings)
                        scope.launch { drawerState.close() }
                    }
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { scope.launch { drawerState.open() } }
                        .background(DigiColors.BgCard, RoundedCornerShape(12.dp))
                        .border(1.dp, DigiColors.Border, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Menu, stringResource(R.string.cd_open_menu),
                        tint = DigiColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    headerTitle ?: tabTitle(selected),
                    color = DigiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                headerActions?.invoke()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector?,
    label: String,
    selected: Boolean,
    tabCount: Int? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                when {
                    focused -> DigiColors.AccentDim
                    selected -> DigiColors.BgSelected
                    else -> Color.Transparent
                },
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (focused || selected) 1.5.dp else 0.dp,
                color = when {
                    focused -> DigiColors.FocusRing
                    selected -> DigiColors.BorderMuted
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = when {
            focused -> DigiColors.Accent
            selected -> DigiColors.TextPrimary
            else -> DigiColors.TextMuted
        }
        if (icon != null) {
            Icon(icon, label, tint = iconTint, modifier = Modifier.size(22.dp))
        } else {
            TabCountIcon(tabCount ?: 0, iconTint, 22.dp)
        }
        Text(
            label,
            color = when {
                focused -> DigiColors.Accent
                selected -> DigiColors.TextPrimary
                else -> DigiColors.TextMuted
            },
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun tabTitle(tab: AppTab): String = when (tab) {
    AppTab.Home -> stringResource(R.string.nav_home)
    AppTab.Downloads -> stringResource(R.string.nav_downloads)
    AppTab.Bookmarks -> stringResource(R.string.nav_bookmarks)
    AppTab.History -> stringResource(R.string.nav_history)
    AppTab.Settings -> stringResource(R.string.nav_settings)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ExpandedScaffold(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    contentEnterFocus: FocusRequester,
    headerTitle: String?,
    headerActions: (@Composable () -> Unit)?,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize(0.45f)
                .align(Alignment.TopEnd)
                .background(DigiColors.GlowBrush)
        )

        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(DigiColors.BgRail)
                    .padding(vertical = 24.dp)
                    .focusGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RailIcon(Icons.Default.Home, stringResource(R.string.nav_home), selected == AppTab.Home) {
                    onSelect(AppTab.Home)
                }
                RailIcon(Icons.Outlined.Download, stringResource(R.string.nav_downloads), selected == AppTab.Downloads) {
                    onSelect(AppTab.Downloads)
                }
                RailIcon(Icons.Outlined.BookmarkBorder, stringResource(R.string.nav_bookmarks), selected == AppTab.Bookmarks) {
                    onSelect(AppTab.Bookmarks)
                }
                RailIcon(Icons.Outlined.History, stringResource(R.string.nav_history), selected == AppTab.History) {
                    onSelect(AppTab.History)
                }
                Spacer(Modifier.weight(1f))
                RailIcon(Icons.Default.Settings, stringResource(R.string.nav_settings), selected == AppTab.Settings) {
                    onSelect(AppTab.Settings)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusGroup()
                    .focusProperties {
                        enter = { contentEnterFocus }
                        exit = { direction ->
                            if (direction == FocusDirection.Left) FocusRequester.Default
                            else FocusRequester.Cancel
                        }
                    }
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 24.dp)
            ) {
                if (headerTitle == null) {
                    content()
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                headerTitle,
                                color = DigiColors.TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            headerActions?.invoke()
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(
                when {
                    focused -> DigiColors.AccentDim
                    selected -> DigiColors.BgSelected
                    else -> Color.Transparent
                },
                RoundedCornerShape(12.dp)
            )
            .then(
                if (focused || selected) Modifier.border(
                    1.5.dp,
                    if (focused) DigiColors.FocusRing else DigiColors.BorderMuted,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon, label,
            tint = when {
                focused -> DigiColors.Accent
                selected -> DigiColors.TextPrimary
                else -> DigiColors.TextMuted
            },
            modifier = Modifier.size(24.dp)
        )
    }
}
