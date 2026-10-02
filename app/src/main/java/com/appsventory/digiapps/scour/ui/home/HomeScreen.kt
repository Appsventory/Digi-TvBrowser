package com.appsventory.digiapps.scour.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import android.view.ViewConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.data.SearchEngine
import com.appsventory.digiapps.scour.data.ShortcutEntry
import com.appsventory.digiapps.scour.ui.shell.CompactWidthBreakpoint
import com.appsventory.digiapps.scour.ui.shell.MenuItem
import com.appsventory.digiapps.scour.ui.shell.TabCountIcon
import com.appsventory.digiapps.scour.ui.shell.contentEnterFocus
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.appsventory.digiapps.scour.R

private val ShortcutColors = DigiColors.ShortcutPalette

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    searchEngine: SearchEngine = SearchEngine.Google,
    tabCount: Int = 0,
    onOpenTabs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // TV / large screen: tabs button sits in the header. Phone: it lives in the sidebar.
    val showTabsButton = LocalConfiguration.current.screenWidthDp.dp >= CompactWidthBreakpoint
    var query by remember { mutableStateOf("") }
    val searchChromeFocus = remember { FocusRequester() }
    val textFieldFocus = remember { FocusRequester() }
    val moreMenuFocus = remember { FocusRequester() }
    val firstShortcutFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var searchEditing by remember { mutableStateOf(false) }

    var shortcuts by remember { mutableStateOf(Prefs.getShortcuts(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<ShortcutEntry?>(null) }
    var menuShortcut by remember { mutableStateOf<ShortcutEntry?>(null) }

    fun saveShortcuts(list: List<ShortcutEntry>) {
        shortcuts = list
        Prefs.setShortcuts(context, list)
    }

    fun submit() {
        keyboard?.hide()
        searchEditing = false
        val target = normalizeInput(query, searchEngine)
        if (target.isNotBlank()) onNavigate(target)
    }

    LaunchedEffect(Unit) {
        keyboard?.hide()
        delay(100)
        runCatching { searchChromeFocus.requestFocus() }
        keyboard?.hide()
    }

    LaunchedEffect(searchEditing) {
        if (searchEditing) {
            delay(16)
            runCatching { textFieldFocus.requestFocus() }
            delay(50)
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.linearGradient(listOf(DigiColors.Accent, DigiColors.BrandGradientEnd)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.app_name).take(1), color = DigiColors.OnDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.app_name),
                color = DigiColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            if (showTabsButton) {
                TabsButton(
                    count = tabCount,
                    onClick = onOpenTabs,
                    downTarget = searchChromeFocus
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(R.string.home_headline),
            color = DigiColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.home_subline),
            color = DigiColors.TextMuted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSubmit = { submit() },
            editing = searchEditing,
            onStartEditing = { searchEditing = true },
            onStopEditing = {
                searchEditing = false
                keyboard?.hide()
            },
            chromeFocusRequester = searchChromeFocus,
            textFocusRequester = textFieldFocus,
            downTarget = moreMenuFocus
        )

        Spacer(Modifier.height(36.dp))

        // Shortcut header + overflow menu
        // Focus order: Search ↓ → ⋯ → first shortcut (not spatial jump)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.home_shortcut),
                color = DigiColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            MoreButton(
                focusRequester = moreMenuFocus,
                upTarget = searchChromeFocus,
                downTarget = firstShortcutFocus,
                onClick = { showAddDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            shortcuts.forEachIndexed { index, entry ->
                ShortcutCard(
                    entry = entry,
                    isFirst = index == 0,
                    firstFocusRequester = if (index == 0) firstShortcutFocus else null,
                    upTarget = if (index == 0) moreMenuFocus else null,
                    onOpen = {
                        keyboard?.hide()
                        searchEditing = false
                        onNavigate(entry.url)
                    },
                    onMenu = { menuShortcut = entry }
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            stringResource(R.string.home_tips),
            color = DigiColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TipCard(stringResource(R.string.tip_back_title), stringResource(R.string.tip_back_body))
            TipCard(stringResource(R.string.tip_dpad_title), stringResource(R.string.tip_dpad_body))
            TipCard(stringResource(R.string.home_shortcut), stringResource(R.string.tip_shortcut_body))
        }

        Spacer(Modifier.height(24.dp))
    }

    // Add / Edit dialog
    if (showAddDialog || editingShortcut != null) {
        val edit = editingShortcut
        ShortcutFormDialog(
            initial = edit,
            onDismiss = {
                showAddDialog = false
                editingShortcut = null
            },
            onSave = { entry ->
                if (edit != null) {
                    saveShortcuts(shortcuts.map { if (it.url == edit.url) entry else it })
                } else {
                    saveShortcuts(shortcuts + entry)
                }
                showAddDialog = false
                editingShortcut = null
            }
        )
    }

    // Context menu (long-press)
    menuShortcut?.let { target ->
        ShortcutContextMenu(
            entry = target,
            onDismiss = { menuShortcut = null },
            onOpen = {
                menuShortcut = null
                onNavigate(target.url)
            },
            onEdit = {
                menuShortcut = null
                editingShortcut = target
            },
            onDelete = {
                saveShortcuts(shortcuts.filter { it.url != target.url })
                menuShortcut = null
            },
            onMoveLeft = {
                val i = shortcuts.indexOfFirst { it.url == target.url }
                if (i > 0) {
                    val list = shortcuts.toMutableList()
                    list[i] = list[i - 1].also { list[i - 1] = list[i] }
                    saveShortcuts(list)
                }
                menuShortcut = null
            },
            onMoveRight = {
                val i = shortcuts.indexOfFirst { it.url == target.url }
                if (i in 0 until shortcuts.lastIndex) {
                    val list = shortcuts.toMutableList()
                    list[i] = list[i + 1].also { list[i + 1] = list[i] }
                    saveShortcuts(list)
                }
                menuShortcut = null
            }
        )
    }
}

@Composable
private fun TabsButton(
    count: Int,
    onClick: () -> Unit,
    downTarget: FocusRequester
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .focusProperties { down = downTarget }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                if (focused) DigiColors.AccentDim else DigiColors.BgCard,
                CircleShape
            )
            .border(
                1.5.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                CircleShape
            )
    ) {
        TabCountIcon(
            count = count,
            tint = if (focused) DigiColors.Accent else DigiColors.TextMuted,
            size = 20.dp
        )
    }
}

@Composable
private fun MoreButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    upTarget: FocusRequester,
    downTarget: FocusRequester
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .focusRequester(focusRequester)
            .focusProperties {
                up = upTarget
                down = downTarget
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                if (focused) DigiColors.AccentDim else DigiColors.BgCard,
                CircleShape
            )
            .border(
                1.5.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                CircleShape
            )
    ) {
        Icon(
            Icons.Default.MoreVert, stringResource(R.string.shortcut_add),
            tint = if (focused) DigiColors.Accent else DigiColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    editing: Boolean,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    chromeFocusRequester: FocusRequester,
    textFocusRequester: FocusRequester,
    downTarget: FocusRequester? = null
) {
    var chromeFocused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(16.dp)
    val highlighted = chromeFocused || editing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(chromeFocusRequester)
            .contentEnterFocus()
            .then(
                if (downTarget != null) Modifier.focusProperties { down = downTarget }
                else Modifier
            )
            .onFocusChanged { chromeFocused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (!editing) {
                            onStartEditing()
                            true
                        } else false
                    }
                    Key.Back -> {
                        if (editing) {
                            onStopEditing()
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .clickable { onStartEditing() }
            .shadow(if (highlighted) 12.dp else 4.dp, shape)
            .background(DigiColors.BgCard, shape)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) DigiColors.FocusRing else DigiColors.Border,
                shape = shape
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search, null,
            tint = if (highlighted) DigiColors.Accent else DigiColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        BasicTextField(
            value = query,
            onValueChange = { if (editing) onQueryChange(it) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(textFocusRequester),
            enabled = editing,
            singleLine = true,
            textStyle = TextStyle(color = DigiColors.TextPrimary, fontSize = 17.sp),
            cursorBrush = SolidColor(DigiColors.Accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = {
                keyboard?.hide()
                onSubmit()
            }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.home_search_hint), color = DigiColors.TextMuted, fontSize = 17.sp)
                    }
                    if (editing) inner()
                    else if (query.isNotEmpty()) {
                        Text(
                            query,
                            color = DigiColors.TextPrimary,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutCard(
    entry: ShortcutEntry,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    isFirst: Boolean = false,
    firstFocusRequester: FocusRequester? = null,
    upTarget: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    // System long-press duration (not a magic number we invent)
    val longPressTimeout = remember {
        ViewConfiguration.getLongPressTimeout().toLong()
    }
    var okDownAt by remember { mutableStateOf(0L) }

    // Mouse/touch: combinedClickable (framework suppresses click after long-click).
    // D-pad OK: decide on KeyUp and CONSUME it so Dialog never sees the release
    // (that release was the bug — it activated the first menu item).
    Column(
        modifier = Modifier
            .width(160.dp)
            .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .then(if (upTarget != null) Modifier.focusProperties { up = upTarget } else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                val isOk =
                    ev.key == Key.DirectionCenter ||
                        ev.key == Key.Enter ||
                        ev.key == Key.NumPadEnter
                if (!isOk) return@onPreviewKeyEvent false
                when (ev.type) {
                    KeyEventType.KeyDown -> {
                        if (okDownAt == 0L) {
                            okDownAt = android.os.SystemClock.uptimeMillis()
                        }
                        true // consume — we handle on KeyUp
                    }
                    KeyEventType.KeyUp -> {
                        val held = android.os.SystemClock.uptimeMillis() - okDownAt
                        okDownAt = 0L
                        if (held >= longPressTimeout) {
                            onMenu()
                        } else {
                            onOpen()
                        }
                        true // consume — Dialog must not receive this KeyUp
                    }
                    else -> false
                }
            }
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onMenu
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .shadow(if (focused) 16.dp else 6.dp, shape)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(entry.colorHex), Color(entry.colorHex).copy(alpha = 0.7f))
                    )
                )
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = DigiColors.FocusRing,
                    shape = shape
                )
        ) {
            if (entry.iconEmoji.isNotBlank()) {
                Text(entry.iconEmoji, fontSize = 36.sp)
            } else {
                Text(
                    entry.title.take(1).uppercase(),
                    color = DigiColors.OnDark,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            entry.title,
            color = if (focused) DigiColors.TextPrimary else DigiColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShortcutFormDialog(
    initial: ShortcutEntry?,
    onDismiss: () -> Unit,
    onSave: (ShortcutEntry) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var icon by remember { mutableStateOf(initial?.iconEmoji ?: "") }
    var colorIdx by remember {
        mutableStateOf(
            ShortcutColors.indexOf(initial?.colorHex ?: ShortcutColors[0]).coerceAtLeast(0)
        )
    }
    val titleFocus = remember { FocusRequester() }
    val defaultTitle = stringResource(R.string.shortcut_default_title)

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { titleFocus.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 480.dp)
                .background(DigiColors.BgPanel, RoundedCornerShape(20.dp))
                .border(1.dp, DigiColors.Border, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text(
                if (initial == null) stringResource(R.string.shortcut_add) else stringResource(R.string.shortcut_edit),
                color = DigiColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))

            FormField(stringResource(R.string.field_title), title, titleFocus) { title = it }
            Spacer(Modifier.height(12.dp))
            FormField(stringResource(R.string.field_url), url) { url = it }
            Spacer(Modifier.height(12.dp))
            FormField(stringResource(R.string.field_icon), icon) { icon = it }

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.field_color), color = DigiColors.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShortcutColors.forEachIndexed { i, c ->
                    ColorSwatch(
                        color = Color(c),
                        selected = i == colorIdx,
                        onClick = { colorIdx = i }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DialogBtn(stringResource(R.string.action_cancel), onClick = onDismiss)
                Spacer(Modifier.width(10.dp))
                DialogBtn(stringResource(R.string.action_save), primary = true, onClick = {
                    val t = title.trim().ifEmpty { defaultTitle }
                    var u = url.trim()
                    if (u.isEmpty()) return@DialogBtn
                    if (!u.startsWith("http")) u = "https://$u"
                    onSave(
                        ShortcutEntry(
                            title = t,
                            url = u,
                            colorHex = ShortcutColors[colorIdx],
                            iconEmoji = icon.trim()
                        )
                    )
                })
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp) // fixed — avoid squeezing neighbors when focused
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(color, CircleShape)
            .border(
                width = when {
                    focused -> 3.dp
                    selected -> 2.5.dp
                    else -> 1.dp
                },
                color = when {
                    focused -> DigiColors.FocusRing
                    selected -> DigiColors.OnDark
                    else -> DigiColors.OnDark.copy(alpha = 0.25f)
                },
                shape = CircleShape
            )
    ) {
        // Selected mark
        if (selected && !focused) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(DigiColors.OnDark, CircleShape)
            )
        }
        if (focused) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(DigiColors.FocusRing, CircleShape)
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    focusRequester: FocusRequester? = null,
    onChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Text(label, color = DigiColors.TextMuted, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = DigiColors.TextPrimary, fontSize = 15.sp),
        cursorBrush = SolidColor(DigiColors.Accent),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .background(DigiColors.BgCard, RoundedCornerShape(12.dp))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) DigiColors.FocusRing else DigiColors.Border,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun DialogBtn(text: String, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                when {
                    focused -> DigiColors.Accent
                    primary -> DigiColors.AccentSoft
                    else -> DigiColors.BgCard
                },
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (focused || primary) DigiColors.Accent else DigiColors.Border,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = when {
                focused -> DigiColors.FocusOnAccent
                primary -> DigiColors.Accent
                else -> DigiColors.TextPrimary
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ShortcutContextMenu(
    entry: ShortcutEntry,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    // No auto-focus on Open — avoids pointer/key release activating it.
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp)
                .background(DigiColors.BgPanel, RoundedCornerShape(16.dp))
                .border(1.dp, DigiColors.Border, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            Text(
                entry.title,
                color = DigiColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            MenuItem(stringResource(R.string.action_open), onOpen)
            MenuItem(stringResource(R.string.action_edit), onEdit)
            MenuItem(stringResource(R.string.action_delete), onDelete)
            MenuItem(stringResource(R.string.menu_move_left), onMoveLeft)
            MenuItem(stringResource(R.string.menu_move_right), onMoveRight)
            MenuItem(stringResource(R.string.action_cancel), onDismiss)
        }
    }
}
@Composable
private fun TipCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DigiColors.BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, DigiColors.Border, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, color = DigiColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = DigiColors.TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

fun normalizeInput(
    input: String,
    searchEngine: SearchEngine = SearchEngine.Google
): String {
    val value = input.trim()
    if (value.isEmpty()) return ""
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return if (value.contains(".") && !value.contains(" ")) {
        "https://$value"
    } else {
        val q = java.net.URLEncoder.encode(value, "UTF-8")
        "${searchEngine.queryUrl}$q"
    }
}
