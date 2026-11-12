package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.ui.compose.clearFocusOnTap
import app.aaps.core.ui.compose.icons.IcSettingsOff
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.navigation.icon
import app.aaps.core.ui.compose.navigation.label
import app.aaps.core.ui.compose.stringResource
import app.aaps.core.ui.compose.stringResourceOrNull
import app.aaps.ui.UiStrings
import app.aaps.ui.search.M3SearchBar
import app.aaps.ui.search.SearchIndexEntry
import app.aaps.ui.search.SearchResults
import app.aaps.ui.search.SearchUiState

/**
 * The Menu screen of the redesign (SPEC section 6). It replaces the top bar (search, settings,
 * version) and the drawer, which are no longer reachable from the phone overview.
 *
 * It is drawn by `MainScreen` over the overview, under the bottom bar, and behaves like a screen:
 * it covers and blocks the overview, Back closes it, and a screen reader announces it as a pane.
 * Every entry sends the same [NavigationRequest] as the matching drawer entry in `MainDrawer`, so
 * nothing new happens behind a tap.
 *
 * @param isTreatmentsEnabled same rule as the drawer: the treatments list needs a loaded profile
 * @param showExit false on iOS, where the app may not close itself
 * @param onClose closes this screen (Back)
 * @param bottomPadding room to leave at the bottom for the bottom bar and the gesture bar. The top
 *   and the sides follow the safe drawing insets here.
 */
@Composable
fun RedesignMenuScreen(
    versionName: String,
    isSimpleMode: Boolean,
    isTreatmentsEnabled: Boolean,
    showExit: Boolean,
    searchUiState: SearchUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchResultClick: (SearchIndexEntry) -> Unit,
    onSearchPluginToggle: (PluginBase) -> Unit,
    onNavigate: (NavigationRequest) -> Unit,
    onClose: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    attentionItems: List<RedesignAttentionItem> = emptyList()
) {
    val colors = redesignColors()
    val focusManager = LocalFocusManager.current
    val title = stringResource(UiStrings.redesign_menu)

    // While the search is active its own handler takes Back first (it leaves the search). Turning
    // this one off meanwhile makes the order certain instead of depending on registration order.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = !searchUiState.isSearchActive,
        onBackCompleted = onClose
    )

    // A non clickable Surface takes every touch, so nothing reaches the overview under it.
    Surface(
        color = colors.pageBackground,
        contentColor = colors.textPrimary,
        // Stops at the bottom bar, so the gesture bar keeps the same scrim as on the home screen.
        modifier = modifier
            .padding(bottom = bottomPadding)
            .fillMaxSize()
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = PAGE_PADDING, end = PAGE_PADDING, top = TITLE_TOP_PADDING)
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .semantics { heading() }
            )

            M3SearchBar(
                query = searchUiState.query,
                isActive = searchUiState.isSearchActive,
                onQueryChange = onSearchQueryChange,
                onClearClick = onSearchClear,
                onActiveChange = onSearchActiveChange
            )

            if (searchUiState.isSearchActive) {
                SearchResults(
                    results = searchUiState.results,
                    wikiResults = searchUiState.wikiResults,
                    isSearching = searchUiState.isSearching,
                    isSearchingWiki = searchUiState.isSearchingWiki,
                    wikiOffline = searchUiState.wikiOffline,
                    revision = searchUiState.revision,
                    onResultClick = onSearchResultClick,
                    onPluginToggle = onSearchPluginToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                MenuContent(
                    colors = colors,
                    versionName = versionName,
                    isSimpleMode = isSimpleMode,
                    isTreatmentsEnabled = isTreatmentsEnabled,
                    showExit = showExit,
                    attentionItems = attentionItems,
                    onNavigate = onNavigate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MenuContent(
    colors: RedesignColors,
    versionName: String,
    isSimpleMode: Boolean,
    isTreatmentsEnabled: Boolean,
    showExit: Boolean,
    attentionItems: List<RedesignAttentionItem>,
    onNavigate: (NavigationRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    fun go(type: ElementType) = onNavigate(NavigationRequest.Element(type))

    Column(
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = PAGE_PADDING)
    ) {
        // Items moved here from the bottom bar. First, because they need the user.
        if (attentionItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeading(stringResource(UiStrings.redesign_needs_attention), colors)
                MenuCard(colors) {
                    attentionItems.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = colors.border)
                        MenuRow(
                            label = item.label,
                            colors = colors,
                            leadingIcon = item.icon,
                            onClick = item.onClick,
                            stateDescription = item.stateDescription,
                            // The badge the item had in the bottom bar; its meaning is spoken as a state above.
                            trailing = item.badge
                        )
                    }
                }
            }
        }

        // Large shortcuts, one row of three.
        // In simple mode the gear is shown crossed, as it was in the old top bar. The action is the same.
        val settingsIcon = if (isSimpleMode) IcSettingsOff else ElementType.SETTINGS.icon()
        val shortcuts = listOfNotNull(
            Shortcut(stringResourceOrNull(ElementType.SETTINGS.label()).orEmpty(), settingsIcon) { go(ElementType.SETTINGS) },
            Shortcut(stringResourceOrNull(ElementType.HISTORY_BROWSER.label()).orEmpty(), ElementType.HISTORY_BROWSER.icon()) { go(ElementType.HISTORY_BROWSER) },
            Shortcut(stringResourceOrNull(ElementType.STATISTICS.label()).orEmpty(), ElementType.STATISTICS.icon()) { go(ElementType.STATISTICS) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
            shortcuts.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                    row.forEach { shortcut ->
                        ShortcutTile(
                            label = shortcut.label,
                            icon = shortcut.icon,
                            colors = colors,
                            onClick = shortcut.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Tools: the rest of the drawer, in the drawer's order.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeading(stringResource(UiStrings.redesign_tools), colors)
            MenuCard(colors) {
                val tools = listOf(
                    ElementType.TREATMENTS,
                    ElementType.PROFILE_HELPER,
                    ElementType.MAINTENANCE,
                    ElementType.SETUP_WIZARD,
                    ElementType.CONFIGURATION
                )
                tools.forEachIndexed { index, type ->
                    if (index > 0) HorizontalDivider(color = colors.border)
                    MenuRow(
                        label = stringResourceOrNull(type.label()).orEmpty(),
                        colors = colors,
                        enabled = type != ElementType.TREATMENTS || isTreatmentsEnabled,
                        onClick = { go(type) },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.chevron,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // About (with the version, which used to be in the top bar) and Exit.
        MenuCard(colors) {
            MenuRow(
                label = stringResourceOrNull(ElementType.ABOUT.label()).orEmpty(),
                colors = colors,
                onClick = { go(ElementType.ABOUT) },
                trailing = {
                    Text(
                        text = versionName,
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            if (showExit) {
                HorizontalDivider(color = colors.border)
                MenuRow(
                    label = stringResourceOrNull(ElementType.EXIT.label()).orEmpty(),
                    colors = colors,
                    textColor = colors.danger,
                    leadingIcon = ElementType.EXIT.icon(),
                    onClick = { go(ElementType.EXIT) }
                )
            }
        }
    }
}

private class Shortcut(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun SectionHeading(text: String, colors: RedesignColors) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = colors.textSecondary,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    colors: RedesignColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.heightIn(min = 78.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.shortcutIcon, modifier = Modifier.size(22.dp))
            Spacer(Modifier.heightIn(min = 6.dp))
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        }
    }
}

@Composable
private fun MenuCard(colors: RedesignColors, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(
    label: String,
    colors: RedesignColors,
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: Color = colors.textPrimary,
    leadingIcon: ImageVector? = null,
    stateDescription: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        contentColor = textColor,
        modifier = Modifier
            .then(if (stateDescription != null) Modifier.semantics { this.stateDescription = stateDescription } else Modifier)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontSize = 15.sp,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                trailing()
            }
        }
    }
}

private val PAGE_PADDING = 16.dp
private val TITLE_TOP_PADDING = 20.dp
private val SECTION_GAP = 14.dp
private val GRID_GAP = 10.dp
private const val DISABLED_ALPHA = 0.38f
