package com.appsventory.digiapps.scour.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.BrowserConfig
import com.appsventory.digiapps.scour.data.Prefs
import com.appsventory.digiapps.scour.data.SearchEngine
import com.appsventory.digiapps.scour.data.SearchSuggestions
import com.appsventory.digiapps.scour.data.Suggestion
import com.appsventory.digiapps.scour.data.SuggestionKind
import com.appsventory.digiapps.scour.ui.home.normalizeInput
import com.appsventory.digiapps.scour.ui.shell.MenuItem
import com.appsventory.digiapps.scour.ui.theme.DigiColors
import kotlinx.coroutines.delay

// Omnibox surface = theme card (slightly elevated over gradient).
// Suggestion colors come exclusively from DigiColors semantic tokens.

/**
 * Shared DigiBrowser circular control focus style
 * (same rules as Home TabsButton / MoreButton):
 *  - idle: BgCard + Border
 *  - focused: AccentDim fill + FocusRing border, Accent icon tint
 */
@Composable
private fun DigiFocusIconButton(
    focused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    contentDescription: String?,
    icon: @Composable (tint: Color) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusedChange(it.isFocused) }
            .focusable()
            .onPreviewKeyEvent { ev ->
                val isOk = ev.key == Key.DirectionCenter ||
                    ev.key == Key.Enter ||
                    ev.key == Key.NumPadEnter
                if (!isOk) return@onPreviewKeyEvent false
                if (ev.type == KeyEventType.KeyUp) onClick()
                true
            }
            .clickable(onClick = onClick)
            .background(
                if (focused) DigiColors.AccentDim else DigiColors.BgCard,
                CircleShape
            )
            .border(
                width = 1.5.dp,
                color = if (focused) DigiColors.FocusRing else DigiColors.Border,
                shape = CircleShape
            )
    ) {
        icon(if (focused) DigiColors.Accent else DigiColors.TextMuted)
    }
}

/**
 * Dedicated Search page. Opened from the Home search bar.
 *
 * Toolbar order: Back → Search bar (engine + query) → Search → History
 */
@Composable
fun SearchScreen(
    searchEngine: SearchEngine,
    onSearchEngineChange: (SearchEngine) -> Unit,
    onSubmit: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeVisible = imeBottom > 0

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var showEngineMenu by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val textFocus = remember { FocusRequester() }
    val searchBtnFocus = remember { FocusRequester() }
    val historyBtnFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val engineFocus = remember { FocusRequester() }
    val firstSuggestFocus = remember { FocusRequester() }

    var textFocused by remember { mutableStateOf(false) }
    var searchBtnFocused by remember { mutableStateOf(false) }
    var historyBtnFocused by remember { mutableStateOf(false) }
    var backFocused by remember { mutableStateOf(false) }
    var engineFocused by remember { mutableStateOf(false) }

    fun doSubmit(raw: String = query) {
        if (submitted) return
        val target = normalizeInput(raw, searchEngine)
        if (target.isBlank()) return
        submitted = true
        keyboard?.hide()
        onSubmit(target)
    }

    fun pickSuggestion(s: Suggestion) {
        if (submitted) return
        submitted = true
        keyboard?.hide()
        onSubmit(s.url ?: normalizeInput(s.text, searchEngine))
    }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { textFocus.requestFocus() }
        delay(40)
        keyboard?.show()
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        val local = SearchSuggestions.local(context, query)
        suggestions = SearchSuggestions.merge(local, emptyList(), query)
        delay(BrowserConfig.SUGGEST_DEBOUNCE_MS)
        suggestions = SearchSuggestions.merge(
            local,
            SearchSuggestions.fetchRemote(searchEngine, query),
            query
        )
    }

    // Back priority:
    //  1) engine menu open → close menu
    //  2) IME visible → hide keyboard only (stay on Search, keep query)
    //  3) otherwise → leave Search page
    BackHandler {
        when {
            showEngineMenu -> showEngineMenu = false
            imeVisible -> {
                keyboard?.hide()
                // Keep query and stay on this page.
            }
            else -> {
                keyboard?.hide()
                onDismiss()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DigiColors.ScreenGradient)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Toolbar: Back | Omnibox | Search | History
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            DigiFocusIconButton(
                focused = backFocused,
                onFocusedChange = { backFocused = it },
                onClick = {
                    // Toolbar Back mirrors system Back: hide IME first, then leave.
                    if (imeVisible) {
                        keyboard?.hide()
                    } else {
                        keyboard?.hide()
                        onDismiss()
                    }
                },
                focusRequester = backFocus,
                contentDescription = stringResource(R.string.action_cancel)
            ) { tint ->
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Omnibox: engine selector + query only (no action buttons inside)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(DigiColors.BgCard, RoundedCornerShape(24.dp))
                    .border(
                        width = if (textFocused || engineFocused) 2.dp else 1.dp,
                        color = if (textFocused || engineFocused) DigiColors.FocusRing
                        else DigiColors.BorderMuted,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .focusRequester(engineFocus)
                        .onFocusChanged { engineFocused = it.isFocused }
                        .focusable()
                        .focusProperties {
                            left = backFocus
                            right = textFocus
                            down = if (suggestions.isNotEmpty()) firstSuggestFocus
                            else FocusRequester.Cancel
                        }
                        .onPreviewKeyEvent { ev ->
                            val isOk = ev.key == Key.DirectionCenter ||
                                ev.key == Key.Enter ||
                                ev.key == Key.NumPadEnter
                            if (!isOk) return@onPreviewKeyEvent false
                            if (ev.type == KeyEventType.KeyUp) showEngineMenu = true
                            true
                        }
                        .clickable { showEngineMenu = true }
                        .padding(end = 4.dp)
                ) {
                    EngineLogo(searchEngine)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.settings_search_engine),
                        tint = DigiColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                BasicTextField(
                    value = query,
                    onValueChange = {
                        submitted = false
                        query = it
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = DigiColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(DigiColors.Accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { doSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(textFocus)
                        .onFocusChanged { textFocused = it.isFocused }
                        .focusProperties {
                            left = engineFocus
                            right = searchBtnFocus
                            down = if (suggestions.isNotEmpty()) firstSuggestFocus
                            else FocusRequester.Cancel
                        },
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.home_search_hint),
                                    color = DigiColors.TextMuted,
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(Modifier.width(10.dp))

            DigiFocusIconButton(
                focused = searchBtnFocused,
                onFocusedChange = { searchBtnFocused = it },
                onClick = { doSubmit() },
                focusRequester = searchBtnFocus,
                contentDescription = stringResource(R.string.cd_search)
            ) { tint ->
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            DigiFocusIconButton(
                focused = historyBtnFocused,
                onFocusedChange = { historyBtnFocused = it },
                onClick = {
                    keyboard?.hide()
                    onOpenHistory()
                },
                focusRequester = historyBtnFocus,
                contentDescription = stringResource(R.string.nav_history)
            ) { tint ->
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DigiColors.SuggestPanel, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                suggestions.forEachIndexed { index, s ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    SearchSuggestionRow(
                        suggestion = s,
                        modifier = Modifier
                            .then(
                                if (index == 0) Modifier.focusRequester(firstSuggestFocus)
                                else Modifier
                            )
                            .focusProperties {
                                if (index == 0) up = textFocus
                            },
                        onClick = { pickSuggestion(s) }
                    )
                }
            }
        }
    }

    if (showEngineMenu) {
        Dialog(onDismissRequest = { showEngineMenu = false }) {
            Column(
                modifier = Modifier
                    .widthIn(min = 260.dp)
                    .background(DigiColors.BgPanel, RoundedCornerShape(16.dp))
                    .border(1.dp, DigiColors.Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_search_engine),
                    color = DigiColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                SearchEngine.entries.forEach { engine ->
                    MenuItem(
                        label = engine.label,
                        onClick = {
                            onSearchEngineChange(engine)
                            Prefs.setSearchEngine(context, engine)
                            showEngineMenu = false
                        }
                    )
                }
                MenuItem(
                    label = stringResource(R.string.action_cancel),
                    onClick = { showEngineMenu = false }
                )
            }
        }
    }
}

@Composable
private fun EngineLogo(engine: SearchEngine) {
    val (bg, letter, fg) = when (engine) {
        SearchEngine.Google -> Triple(Color(0xFFFFFFFF), "G", Color(0xFF4285F4))
        SearchEngine.Bing -> Triple(Color(0xFF008373), "b", Color.White)
        SearchEngine.Yahoo -> Triple(Color(0xFF6001D2), "Y", Color.White)
        SearchEngine.DuckDuckGo -> Triple(Color(0xFFDE5833), "D", Color.White)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .background(bg, CircleShape)
            .border(1.dp, DigiColors.BorderMuted, CircleShape)
    ) {
        Text(
            letter,
            color = fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchSuggestionRow(
    suggestion: Suggestion,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val itemBg = if (focused) DigiColors.SuggestItemFocused else DigiColors.SuggestItem
    val itemStroke = if (focused) DigiColors.SuggestItemStrokeFocused else DigiColors.SuggestItemStroke
    val labelColor = if (focused) DigiColors.FocusOnAccent else DigiColors.TextPrimary
    val iconColor = if (focused) DigiColors.FocusOnAccent else DigiColors.TextMuted
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Clip first so background / border / ripple share the same rounded bounds.
            .clip(shape)
            .background(itemBg)
            .border(1.5.dp, itemStroke, shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { ev ->
                val isOk = ev.key == Key.DirectionCenter ||
                    ev.key == Key.Enter ||
                    ev.key == Key.NumPadEnter
                if (!isOk) return@onPreviewKeyEvent false
                if (ev.type == KeyEventType.KeyUp) onClick()
                true
            }
            // No default indication layer that could paint an opaque fill over our bg.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (suggestion.kind) {
                SuggestionKind.Query -> Icons.Outlined.Explore
                SuggestionKind.History -> Icons.Outlined.History
                SuggestionKind.Bookmark -> Icons.Outlined.Explore
            },
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            suggestion.text,
            color = labelColor,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
